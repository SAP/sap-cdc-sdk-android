// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz

/**
 * Configuration options for MRZ image processing.
 * 
 * These settings control how the MRZ processor behaves when analyzing images
 * and can be adjusted based on your use case, device capabilities, and
 * desired accuracy vs. performance trade-offs.
 * 
 * ## Usage Example
 * 
 * ```kotlin
 * // High accuracy configuration
 * val config = MRZProcessorConfig(
 *     confidenceThreshold = 0.9f,
 *     debugMode = true,
 *     minTextLength = 30
 * )
 * 
 * // Performance-optimized configuration
 * val config = MRZProcessorConfig(
 *     confidenceThreshold = 0.7f,
 *     debugMode = false,
 *     minTextLength = 25
 * )
 * ```
 * 
 * @property confidenceThreshold Minimum confidence level (0.0 to 1.0) for text recognition.
 *                               Higher values mean more accuracy but may reject valid MRZ.
 *                               Default: 0.75
 * 
 * @property debugMode Enable debug logging for troubleshooting.
 *                     When true, logs detailed information about processing steps.
 *                     Default: false
 * 
 * @property minTextLength Minimum character length for a line to be considered valid MRZ text.
 *                         MRZ lines are typically 30, 36, or 44 characters.
 *                         Default: 28 (allows some tolerance)
 * 
 * @property maxTextLength Maximum character length for a line to be considered valid MRZ text.
 *                         Lines longer than this are likely not MRZ.
 *                         Default: 50
 * 
 * @property allowPartialMatch Allow processing of potentially incomplete MRZ data.
 *                             When false, requires all expected lines to be present.
 *                             Default: false
 */
data class MRZProcessorConfig(
    val confidenceThreshold: Float = 0.75f,
    val debugMode: Boolean = false,
    val minTextLength: Int = 28,
    val maxTextLength: Int = 50,
    val allowPartialMatch: Boolean = false
) {
    init {
        require(confidenceThreshold in 0.0f..1.0f) {
            "confidenceThreshold must be between 0.0 and 1.0, got $confidenceThreshold"
        }
        require(minTextLength > 0) {
            "minTextLength must be positive, got $minTextLength"
        }
        require(maxTextLength > minTextLength) {
            "maxTextLength must be greater than minTextLength"
        }
    }
    
    companion object {
        /**
         * High accuracy configuration.
         * Optimized for maximum accuracy at the cost of potential false negatives.
         * Use when you need highly reliable results and can afford to scan multiple times.
         */
        val HIGH_ACCURACY = MRZProcessorConfig(
            confidenceThreshold = 0.9f,
            debugMode = false,
            minTextLength = 30,
            maxTextLength = 50,
            allowPartialMatch = false
        )
        
        /**
         * Balanced configuration (default).
         * Good balance between accuracy and performance for most use cases.
         */
        val BALANCED = MRZProcessorConfig(
            confidenceThreshold = 0.75f,
            debugMode = false,
            minTextLength = 28,
            maxTextLength = 50,
            allowPartialMatch = false
        )
        
        /**
         * Performance-optimized configuration.
         * Optimized for speed and may be more lenient with recognition.
         * Use when processing power is limited or when speed is critical.
         */
        val PERFORMANCE = MRZProcessorConfig(
            confidenceThreshold = 0.6f,
            debugMode = false,
            minTextLength = 25,
            maxTextLength = 50,
            allowPartialMatch = true
        )
        
        /**
         * Debug configuration.
         * Enables extensive logging to help troubleshoot recognition issues.
         */
        val DEBUG = MRZProcessorConfig(
            confidenceThreshold = 0.75f,
            debugMode = true,
            minTextLength = 28,
            maxTextLength = 50,
            allowPartialMatch = false
        )
    }
}
