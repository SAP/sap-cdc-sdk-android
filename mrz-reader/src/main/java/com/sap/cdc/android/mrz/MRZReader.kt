// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sap.cdc.android.mrz.model.MRZResult
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MRZReader - Simplified API for MRZ scanning with start/stop control.
 * 
 * This class provides a high-level interface for MRZ scanning that:
 * - Wraps the IMRZImageProcessor with start/stop controls
 * - Provides an ImageAnalysis.Analyzer for CameraX integration
 * - Manages scanning state (active/inactive)
 * - Handles lifecycle automatically
 * 
 * Usage:
 * ```kotlin
 * val mrzReader = MRZReader.create(context, config, lifecycleOwner) { result ->
 *     // Handle MRZ result
 * }
 * 
 * // Get analyzer for CameraX
 * val analyzer = mrzReader.getImageAnalyzer()
 * 
 * // Control scanning
 * mrzReader.start()  // Start processing frames
 * mrzReader.stop()   // Stop processing frames
 * mrzReader.reset()  // Reset to scanning state
 * mrzReader.release() // Clean up resources
 * ```
 */
class MRZReader private constructor(
    private val context: Context,
    private val config: MRZProcessorConfig,
    private val lifecycleOwner: LifecycleOwner,
    private val onResult: (MRZResult) -> Unit
) {
    
    private val processor = IMRZImageProcessor.create(context, config)
    private val isActive = AtomicBoolean(false)
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    
    /**
     * Check if scanning is currently active.
     */
    val isScanning: Boolean
        get() = isActive.get()
    
    /**
     * Get the ImageAnalysis.Analyzer to attach to CameraX.
     * 
     * This analyzer will process frames only when scanning is active.
     */
    fun getImageAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            processFrame(imageProxy)
        }
    }
    
    /**
     * Start processing camera frames.
     * 
     * Call this when the user wants to begin scanning.
     */
    fun start() {
        if (isActive.compareAndSet(false, true)) {
            frameCounter = 0
            logDebug("MRZ scanning started")
            onResult(MRZResult.Scanning())
        }
    }
    
    /**
     * Stop processing camera frames.
     * 
     * Call this when the user wants to pause scanning.
     * Frames will still flow through the analyzer but will be ignored.
     */
    fun stop() {
        if (isActive.compareAndSet(true, false)) {
            logDebug("MRZ scanning stopped")
        }
    }
    
    /**
     * Reset the scanner to scanning state.
     * 
     * Useful after a successful scan or error to start fresh.
     */
    fun reset() {
        frameCounter = 0
        isProcessing.set(false)
        logDebug("MRZ scanner reset")
        onResult(MRZResult.Scanning())
    }
    
    /**
     * Release all resources.
     * 
     * Call this when done with the scanner (e.g., in onDestroy).
     */
    fun release() {
        stop()
        processor.release()
        logDebug("MRZ reader released")
    }
    
    /**
     * Process a single camera frame.
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Always close the image proxy
            if (!isActive.get()) {
                imageProxy.close()
                return
            }
            
            // Check if already processing
            if (isProcessing.get()) {
                logDebug("Frame skipped: already processing")
                imageProxy.close()
                return
            }
            
            frameCounter++
            
            // Process every 10th frame to avoid overload
            if (frameCounter % 10 != 0) {
                imageProxy.close()
                return
            }
            
            logDebug("Processing frame $frameCounter")
            isProcessing.set(true)
            
            // Process image using coroutine
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        val result = processor.processImage(imageProxy)
                        
                        // Only deliver results if still active
                        if (isActive.get()) {
                            onResult(result)
                            
                            // Auto-stop on success or error
                            if (result is MRZResult.Success || result is MRZResult.Error) {
                                stop()
                            }
                        }
                    } catch (e: Exception) {
                        logDebug("Error processing frame: ${e.message}")
                        if (isActive.get()) {
                            onResult(MRZResult.Error("Processing error: ${e.message}", e))
                        }
                    } finally {
                        isProcessing.set(false)
                        imageProxy.close()
                    }
                }
            }
        } catch (e: Exception) {
            logDebug("Error in processFrame: ${e.message}")
            imageProxy.close()
            isProcessing.set(false)
        }
    }
    
    private fun logDebug(message: String) {
        if (config.debugMode) {
            Log.d(TAG, message)
        }
    }
    
    companion object {
        private const val TAG = "MRZReader"
        
        /**
         * Create a new MRZReader instance.
         * 
         * @param context Android context
         * @param config MRZ processor configuration
         * @param lifecycleOwner Lifecycle owner for automatic lifecycle management
         * @param onResult Callback for MRZ scan results
         * @return Configured MRZReader instance
         */
        fun create(
            context: Context,
            config: MRZProcessorConfig,
            lifecycleOwner: LifecycleOwner,
            onResult: (MRZResult) -> Unit
        ): MRZReader {
            return MRZReader(context, config, lifecycleOwner, onResult)
        }
    }
}
