// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.recognition

import android.graphics.Bitmap
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

/**
 * Internal helper class for ML Kit text recognition operations.
 * 
 * This class wraps Google ML Kit's text recognition API and provides
 * MRZ-specific text extraction and filtering capabilities.
 * 
 * @property config Configuration for text recognition behavior
 */
internal class MRZTextRecognizer(
    private var config: MRZProcessorConfig
) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Update configuration at runtime.
     */
    fun updateConfig(newConfig: MRZProcessorConfig) {
        config = newConfig
    }
    
    /**
     * Extract text from a CameraX ImageProxy.
     * 
     * @param imageProxy Image from camera
     * @return List of text lines that could be MRZ data
     */
    @OptIn(ExperimentalGetImage::class)
    suspend fun recognizeText(imageProxy: ImageProxy): List<String> {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                logDebug("ImageProxy contained null media image")
                return emptyList()
            }
            
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            recognizeFromInputImage(inputImage)
        } catch (e: Exception) {
            logDebug("Error recognizing text from ImageProxy: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Extract text from a Bitmap.
     * 
     * @param bitmap Image bitmap
     * @return List of text lines that could be MRZ data
     */
    suspend fun recognizeText(bitmap: Bitmap): List<String> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizeFromInputImage(inputImage)
        } catch (e: Exception) {
            logDebug("Error recognizing text from Bitmap: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Common recognition logic for InputImage.
     */
    private suspend fun recognizeFromInputImage(inputImage: InputImage): List<String> {
        logDebug("========================================")
        logDebug("Starting text recognition...")
        
        val result = suspendCoroutine<Text> { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    logDebug("ML Kit recognition successful")
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    logDebug("ML Kit recognition failed: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }
        
        val recognizedText = result.text
        logDebug("========================================")
        logDebug("RAW RECOGNIZED TEXT (${recognizedText.length} chars):")
        logDebug("\"$recognizedText\"")
        logDebug("========================================")
        logDebug("Total text blocks detected: ${result.textBlocks.size}")
        
        // Extract lines from text blocks
        // ML Kit sometimes groups MRZ lines into single blocks, so we need to split them
        val allLines = mutableListOf<String>()
        
        logDebug("----------------------------------------")
        logDebug("PHASE 1: Extracting lines from blocks...")
        // First try: Get lines from text blocks
        result.textBlocks.forEachIndexed { blockIndex, block ->
            logDebug("Block [$blockIndex]: \"${block.text}\" (${block.lines.size} lines)")
            block.lines.forEachIndexed { lineIndex, line ->
                logDebug("  Line [$blockIndex.$lineIndex]: \"${line.text}\"")
                allLines.add(line.text)
            }
        }
        logDebug("Phase 1 result: ${allLines.size} lines extracted")
        
        logDebug("----------------------------------------")
        logDebug("PHASE 2: Splitting blocks with newlines...")
        var phase2Count = 0
        result.textBlocks.forEach { block ->
            val blockText = block.text
            if (blockText.contains('\n')) {
                logDebug("Block contains newlines: \"$blockText\"")
                // Split block by newlines to get individual lines
                blockText.lines().forEach { splitLine ->
                    if (splitLine.isNotBlank() && !allLines.contains(splitLine.trim())) {
                        logDebug("  Split line: \"$splitLine\"")
                        allLines.add(splitLine.trim())
                        phase2Count++
                    }
                }
            }
        }
        logDebug("Phase 2 result: $phase2Count new lines found")
        
        logDebug("----------------------------------------")
        logDebug("PHASE 3: Parsing raw text for newlines...")
        var phase3Count = 0
        if (recognizedText.contains('\n')) {
            logDebug("Raw text contains newlines, splitting...")
            recognizedText.lines().forEachIndexed { index, rawLine ->
                val trimmed = rawLine.trim()
                if (trimmed.isNotBlank()) {
                    if (!allLines.contains(trimmed)) {
                        logDebug("  Raw line [$index]: \"$trimmed\" (NEW)")
                        allLines.add(trimmed)
                        phase3Count++
                    } else {
                        logDebug("  Raw line [$index]: \"$trimmed\" (duplicate, skipped)")
                    }
                }
            }
        } else {
            logDebug("No newlines in raw text")
        }
        logDebug("Phase 3 result: $phase3Count new lines found")
        
        logDebug("----------------------------------------")
        logDebug("TOTAL LINES EXTRACTED: ${allLines.size}")
        allLines.forEachIndexed { index, line ->
            logDebug("  [$index]: \"$line\" (${line.length} chars)")
        }
        
        logDebug("========================================")
        logDebug("PREPROCESSING & FILTERING...")
        logDebug("----------------------------------------")
        
        // Filter for potential MRZ lines
        logDebug("Step 1: Cleaning lines (trim, remove spaces, uppercase)...")
        val cleanedLines = allLines.map { original ->
            val cleaned = original.trim().replace(" ", "").uppercase()
            if (original != cleaned) {
                logDebug("  \"$original\" -> \"$cleaned\"")
            }
            cleaned
        }
        
        logDebug("Step 2: Removing duplicates...")
        val uniqueLines = cleanedLines.distinct()
        val duplicatesRemoved = cleanedLines.size - uniqueLines.size
        if (duplicatesRemoved > 0) {
            logDebug("  Removed $duplicatesRemoved duplicate lines")
        }
        
        logDebug("Step 3: Filtering MRZ candidates...")
        logDebug("  Testing ${uniqueLines.size} lines against MRZ criteria...")
        val mrzLines = uniqueLines.filter { isMRZCandidate(it) }
        
        logDebug("Step 4: Sorting by length (longest first)...")
        val sortedLines = mrzLines.sortedByDescending { it.length }
        
        logDebug("========================================")
        logDebug("FINAL RESULTS:")
        logDebug("  Total MRZ candidates found: ${sortedLines.size}")
        
        if (sortedLines.isEmpty()) {
            logDebug("  ❌ NO MRZ CANDIDATES FOUND!")
            logDebug("  All ${allLines.size} extracted lines were filtered out")
        } else {
            logDebug("  ✅ Found ${sortedLines.size} valid MRZ line(s):")
            sortedLines.forEachIndexed { index, line ->
                logDebug("    [$index]: $line (${line.length} chars)")
            }
        }
        logDebug("========================================")
        
        return sortedLines
    }
    
    /**
     * Check if a text line is a potential MRZ candidate.
     * 
     * MRZ lines have specific characteristics:
     * - Contain mostly uppercase letters, digits, and '<' character
     * - Have specific lengths (28-50 characters for various formats)
     * - Usually contain multiple '<' filler characters
     */
    private fun isMRZCandidate(line: String): Boolean {
        // Minimum length check - be more lenient
        if (line.length < 20) {
            logDebug("Line rejected: too short (${line.length})")
            return false
        }
        
        // Character set check - MRZ only contains A-Z, 0-9, and '<'
        // Count valid MRZ characters
        val validChars = line.count { it in 'A'..'Z' || it in '0'..'9' || it == '<' }
        val validRatio = validChars.toFloat() / line.length.toFloat()
        
        // Allow lines with at least 80% valid MRZ characters (accounts for OCR errors)
        if (validRatio < 0.8f) {
            logDebug("Line rejected: only ${validRatio * 100}% valid MRZ characters: $line")
            return false
        }
        
        // MRZ lines typically have '<' characters (used as fillers)
        // But be lenient - some might have few or OCR might miss them
        val hasUppercase = line.any { it in 'A'..'Z' }
        val hasDigits = line.any { it in '0'..'9' }
        
        if (!hasUppercase && !hasDigits) {
            logDebug("Line rejected: no letters or digits: $line")
            return false
        }
        
        logDebug("Line accepted as MRZ candidate: $line (length: ${line.length}, valid: ${validRatio * 100}%)")
        return true
    }
    
    /**
     * Release ML Kit resources.
     */
    fun release() {
        try {
            textRecognizer.close()
            logDebug("Text recognizer released")
        } catch (e: Exception) {
            logDebug("Error releasing text recognizer: ${e.message}")
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
