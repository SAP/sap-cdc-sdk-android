// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.sap.cdc.android.mrz.model.MRZResult
import com.sap.cdc.android.mrz.parser.MRZParserFactory
import com.sap.cdc.android.mrz.parser.ParseError
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.recognition.MRZTextRecognizer

/**
 * Default implementation of MRZImageProcessor.
 * 
 * This class coordinates between ML Kit text recognition and MRZ parsing
 * to extract and validate MRZ data from images.
 * 
 * @param context Android context
 * @param config Initial configuration
 */
internal class MRZImageProcessorImpl(
    private val context: Context,
    private var config: MRZProcessorConfig
) : MRZImageProcessor {
    
    private val textRecognizer = MRZTextRecognizer(config)
    
    private var isReleased = false
    
    override suspend fun processImage(imageProxy: ImageProxy): MRZResult {
        if (isReleased) {
            return MRZResult.Error("Processor has been released")
        }
        
        return try {
            logDebug("Processing ImageProxy")
            
            // Step 1: Extract text using ML Kit
            val textLines = textRecognizer.recognizeText(imageProxy)
            
            if (textLines.isEmpty()) {
                logDebug("No text lines detected")
                return MRZResult.Scanning
            }
            
            logDebug("Detected ${textLines.size} potential MRZ lines")
            
            // Step 2: Parse MRZ data
            parseMRZLines(textLines)
            
        } catch (e: Exception) {
            logDebug("Error processing image: ${e.message}")
            MRZResult.Error(
                message = "Failed to process image: ${e.message}",
                exception = e
            )
        }
    }
    
    override suspend fun processImage(bitmap: Bitmap): MRZResult {
        if (isReleased) {
            return MRZResult.Error("Processor has been released")
        }
        
        return try {
            logDebug("Processing Bitmap")
            
            // Step 1: Extract text using ML Kit
            val textLines = textRecognizer.recognizeText(bitmap)
            
            if (textLines.isEmpty()) {
                logDebug("No text lines detected")
                return MRZResult.Scanning
            }
            
            logDebug("Detected ${textLines.size} potential MRZ lines")
            
            // Step 2: Parse MRZ data
            parseMRZLines(textLines)
            
        } catch (e: Exception) {
            logDebug("Error processing bitmap: ${e.message}")
            MRZResult.Error(
                message = "Failed to process bitmap: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Attempt to parse MRZ lines using appropriate parser.
     */
    private fun parseMRZLines(lines: List<String>): MRZResult {
        if (lines.isEmpty()) {
            return MRZResult.Scanning
        }
        
        logDebug("Attempting to parse ${lines.size} lines")
        
        // Use factory to detect format and parse
        return when (val parseResult = MRZParserFactory.detectAndParse(lines)) {
            is ParseResult.Success -> {
                logDebug("Successfully parsed MRZ data")
                MRZResult.Success(parseResult.data)
            }
            is ParseResult.Failure -> {
                val errorMessage = parseResult.errors.joinToString(", ") { error ->
                    error.toMessage()
                }
                logDebug("Parse failed: $errorMessage")
                
                // If partial matches are allowed and we got some data, might still be scanning
                if (config.allowPartialMatch) {
                    MRZResult.Scanning
                } else {
                    MRZResult.Error("Failed to parse MRZ: $errorMessage")
                }
            }
        }
    }
    
    override fun configure(config: MRZProcessorConfig) {
        this.config = config
        textRecognizer.updateConfig(config)
        logDebug("Configuration updated")
    }
    
    override fun getConfig(): MRZProcessorConfig {
        return config
    }
    
    override fun release() {
        if (!isReleased) {
            textRecognizer.release()
            isReleased = true
            logDebug("Processor released")
        }
    }
    
    private fun logDebug(message: String) {
        if (config.debugMode) {
            Log.d(TAG, message)
        }
    }
    
    companion object {
        private const val TAG = "MRZImageProcessor"
    }
}
