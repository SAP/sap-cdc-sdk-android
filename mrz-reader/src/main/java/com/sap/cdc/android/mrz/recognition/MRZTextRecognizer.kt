// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.recognition

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sap.cdc.android.mrz.MRZProcessorConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min

/**
 * MRZ text recognizer with advanced detection logic.
 * 
 * Implements advanced clustering and scoring:
 * - Groups lines into 2/3-line MRZ patterns
 * - Scores candidates based on quality metrics
 * - Returns structured candidates for validation
 */
internal class MRZTextRecognizer(
    private var config: MRZProcessorConfig
) {
    
    // Tuning parameters
    private val minAllowedRatio = 0.85
    private val minChevrons = 2
    private val minLineLength = 20
    
    private val textRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder()
            .setExecutor { command -> command.run() }
            .build()
    )
    
    fun updateConfig(newConfig: MRZProcessorConfig) {
        config = newConfig
    }
    
    /**
     * Extract and score MRZ line candidates from ImageProxy.
     */
    @OptIn(ExperimentalGetImage::class)
    suspend fun recognizeWithCandidates(imageProxy: ImageProxy): List<MRZLineCandidate> {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                logDebug("ImageProxy contained null media image")
                return emptyList()
            }
            
            val bitmap = imageProxy.toBitmap()
            val preprocessedBitmap = preprocessImage(bitmap)
            val inputImage = InputImage.fromBitmap(
                preprocessedBitmap,
                imageProxy.imageInfo.rotationDegrees
            )
            
            extractCandidates(inputImage, imageProxy.height)
        } catch (e: Exception) {
            logDebug("Error recognizing text: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Extract and score MRZ line candidates from Bitmap.
     */
    suspend fun recognizeWithCandidates(bitmap: Bitmap): List<MRZLineCandidate> {
        return try {
            val preprocessedBitmap = preprocessImage(bitmap)
            val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
            extractCandidates(inputImage, bitmap.height)
        } catch (e: Exception) {
            logDebug("Error recognizing text: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Extract MRZ line candidates from ML Kit result.
     */
    private suspend fun extractCandidates(inputImage: InputImage, imageHeight: Int): List<MRZLineCandidate> {
        logDebug("========================================")
        logDebug("Starting enhanced text recognition...")
        
        val result = suspendCoroutine<Text> { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        
        logDebug("ML Kit found ${result.textBlocks.size} text blocks")
        
        // Extract all OCR lines with bounding boxes
        val ocrLines = mutableListOf<OcrLine>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val rect = line.boundingBox ?: continue
                val text = line.text
                if (text.isNullOrBlank()) continue
                ocrLines.add(OcrLine(text, rect))
            }
        }
        
        logDebug("Extracted ${ocrLines.size} OCR lines")
        
        if (ocrLines.isEmpty()) {
            return emptyList()
        }
        
        // Convert to MRZ line candidates with scoring
        val candidates = ocrLines
            .mapNotNull { toMrzLineCandidate(it, imageHeight) }
            .sortedByDescending { it.score }
            .take(30) // Limit to top 30 candidates
        
        logDebug("Found ${candidates.size} MRZ line candidates")
        candidates.forEach { candidate ->
            logDebug("  Candidate: \"${candidate.text}\" (score: ${candidate.score})")
        }
        
        return candidates
    }
    
    /**
     * Convert OCR line to MRZ candidate with scoring.
     */
    private fun toMrzLineCandidate(line: OcrLine, imageHeight: Int): MRZLineCandidate? {
        val normalized = normalizeMrzText(line.text)
        
        // Length check
        if (normalized.length < minLineLength) return null
        
        // Character set check
        val allowed = normalized.count { it.isMrzAllowed() }
        val allowedRatio = allowed.toDouble() / max(1, normalized.length).toDouble()
        if (allowedRatio < minAllowedRatio) return null
        
        // Chevron check
        val chevrons = normalized.count { it == '<' }
        if (chevrons < minChevrons) return null
        
        // Calculate score
        val lowerBias = (line.bounds.centerY().toDouble() / imageHeight.toDouble())
        val lengthScore = min(1.0, normalized.length / 44.0)
        val score = (allowedRatio * 2.0) +
                    (min(10, chevrons) * 0.15) +
                    (lengthScore * 0.6) +
                    (lowerBias * 0.4)
        
        return MRZLineCandidate(
            text = normalized,
            bounds = line.bounds,
            score = score
        )
    }
    
    /**
     * Normalize MRZ text: uppercase, remove spaces, correct common errors.
     */
    private fun normalizeMrzText(text: String): String {
        return buildString(text.length) {
            for (ch in text.uppercase()) {
                when {
                    ch == ' ' || ch == '\n' || ch == '\t' -> Unit
                    ch == '«' || ch == '»' || ch == '‹' || ch == '›' -> append('<')
                    ch == '〈' || ch == '〉' -> append('<')
                    ch == '|' || ch == '¦' || ch == '│' -> append('I')
                    ch == '-' || ch == '_' -> Unit
                    else -> append(ch)
                }
            }
        }
    }
    
    /**
     * Check if character is allowed in MRZ.
     */
    private fun Char.isMrzAllowed(): Boolean =
        (this in 'A'..'Z') || (this in '0'..'9') || this == '<'
    
    /**
     * Preprocess image for better OCR.
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        try {
            val scaledBitmap = scaleToOptimalSize(bitmap)
            return enhanceContrastAndBrightness(scaledBitmap)
        } catch (e: Exception) {
            logDebug("Error preprocessing: ${e.message}")
            return bitmap
        }
    }
    
    private fun scaleToOptimalSize(bitmap: Bitmap): Bitmap {
        val maxDimension = 1920
        val minDimension = 1280
        val width = bitmap.width
        val height = bitmap.height
        val longestSide = maxOf(width, height)
        
        if (longestSide in minDimension..maxDimension) {
            return bitmap
        }
        
        val scaleFactor = if (longestSide > maxDimension) {
            maxDimension.toFloat() / longestSide
        } else {
            minDimension.toFloat() / longestSide
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun enhanceContrastAndBrightness(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var totalBrightness = 0L
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalBrightness += (r + g + b) / 3
        }
        val avgBrightness = totalBrightness / pixels.size
        
        val contrastFactor = 1.3f
        val brightnessAdjust = (128 - avgBrightness).toInt()
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            r = (((r - 128) * contrastFactor) + 128).toInt()
            g = (((g - 128) * contrastFactor) + 128).toInt()
            b = (((b - 128) * contrastFactor) + 128).toInt()
            
            r += brightnessAdjust
            g += brightnessAdjust
            b += brightnessAdjust
            
            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)
            
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        val enhancedBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        return enhancedBitmap
    }
    
    fun release() {
        try {
            textRecognizer.close()
            logDebug("Text recognizer released")
        } catch (e: Exception) {
            logDebug("Error releasing: ${e.message}")
        }
    }
    
    private fun logDebug(message: String) {
        if (config.debugMode) {
            Log.d(TAG, message)
        }
    }
    
    companion object {
        private const val TAG = "MRZTextRecognizer"
    }
}

/**
 * Simple OCR line with bounding box.
 */
internal data class OcrLine(val text: String, val bounds: Rect)