// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.sap.cdc.android.mrz.model.MRZResult

/**
 * Interface for processing images to detect and parse Machine Readable Zone (MRZ) data.
 * 
 * This interface defines the contract for MRZ image processing, allowing clients to:
 * - Use their own CameraX implementation
 * - Submit images for MRZ detection and parsing
 * - Process both live camera frames and saved images
 * 
 * The processor handles:
 * - Text recognition using ML Kit
 * - MRZ format detection (TD1, TD2, TD3)
 * - Data extraction and validation
 * - Checksum verification per ICAO 9303 standard
 * 
 * ## Usage Example
 * 
 * ```kotlin
 * // Create processor instance
 * val processor = MRZImageProcessor.create(context)
 * 
 * // Configure if needed
 * processor.configure(MRZProcessorConfig(
 *     confidenceThreshold = 0.8f,
 *     debugMode = false
 * ))
 * 
 * // In your CameraX ImageAnalysis.Analyzer
 * override fun analyze(imageProxy: ImageProxy) {
 *     lifecycleScope.launch {
 *         when (val result = processor.processImage(imageProxy)) {
 *             is MRZResult.Success -> {
 *                 // Handle parsed MRZ data
 *                 val data = result.data
 *                 println("Passport: ${data.documentNumber}")
 *             }
 *             is MRZResult.Error -> {
 *                 // Handle error
 *                 println("Error: ${result.message}")
 *             }
 *             is MRZResult.Scanning -> {
 *                 // Still scanning, no MRZ detected yet
 *             }
 *         }
 *         imageProxy.close()
 *     }
 * }
 * ```
 * 
 * @see MRZResult
 * @see MRZProcessorConfig
 */
interface MRZImageProcessor {
    
    /**
     * Process a CameraX ImageProxy for MRZ detection and parsing.
     * 
     * This is the primary method for processing live camera frames. The method:
     * 1. Extracts the image from the ImageProxy
     * 2. Runs ML Kit text recognition
     * 3. Filters and formats detected text
     * 4. Attempts to parse MRZ data
     * 5. Validates checksums
     * 
     * **Important**: The caller is responsible for closing the ImageProxy after processing.
     * 
     * @param imageProxy CameraX ImageProxy from your camera implementation
     * @return MRZResult indicating success with data, error, or scanning state
     */
    suspend fun processImage(imageProxy: ImageProxy): MRZResult
    
    /**
     * Process a Bitmap image for MRZ detection and parsing.
     * 
     * Useful for:
     * - Processing saved images from gallery
     * - Testing with sample images
     * - Processing images from other sources
     * 
     * @param bitmap Bitmap image containing MRZ data
     * @return MRZResult indicating success with data, error, or scanning state
     */
    suspend fun processImage(bitmap: Bitmap): MRZResult
    
    /**
     * Configure the processor with custom settings.
     * 
     * Settings can be changed at any time and will affect subsequent processing calls.
     * 
     * @param config Configuration options for processing behavior
     */
    fun configure(config: MRZProcessorConfig)
    
    /**
     * Get the current configuration.
     * 
     * @return Current MRZProcessorConfig in use
     */
    fun getConfig(): MRZProcessorConfig
    
    /**
     * Release resources used by the processor.
     * 
     * Call this when you're done using the processor to free up ML Kit resources.
     * After calling this method, the processor should not be used again.
     */
    fun release()
    
    companion object {
        /**
         * Create a new MRZImageProcessor instance.
         * 
         * @param context Android context (application or activity context)
         * @param config Optional initial configuration
         * @return Configured MRZImageProcessor ready for use
         */
        fun create(
            context: android.content.Context,
            config: MRZProcessorConfig = MRZProcessorConfig()
        ): MRZImageProcessor {
            return MRZImageProcessorImpl(context, config)
        }
    }
}
