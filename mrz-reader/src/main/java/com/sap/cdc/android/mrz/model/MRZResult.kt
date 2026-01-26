package com.sap.cdc.android.mrz.model

/**
 * Represents the result state of MRZ scanning operations.
 * 
 * This sealed class provides a type-safe way to handle different states during
 * the MRZ scanning process, including success, error, and in-progress scanning states.
 * 
 * Used with Kotlin Flow/StateFlow to reactively communicate scanning results
 * from the camera manager to the application layer.
 * 
 * Example usage:
 * ```kotlin
 * mrzReader.scanResults.collect { result ->
 *     when (result) {
 *         is MRZResult.Success -> handleMRZData(result.data)
 *         is MRZResult.Error -> showError(result.message)
 *         is MRZResult.Scanning -> showScanningIndicator()
 *     }
 * }
 * ```
 */
sealed class MRZResult {
    /**
     * Indicates successful MRZ detection and parsing.
     * 
     * This result is emitted when:
     * - MRZ lines are successfully extracted from camera frames
     * - MRZ format is recognized (TD1, TD2, or TD3)
     * - All checksums are validated
     * - Data is successfully parsed into MRZData structure
     * 
     * @property data The parsed and validated MRZ data
     */
    data class Success(
        val data: MRZData,
        val detectionInfo: MRZDetectionInfo? = null
    ) : MRZResult() {
        override fun toString(): String {
            return "MRZResult.Success(data=${data.fullName}, valid=${data.isValid})"
        }
    }
    
    /**
     * Indicates an error occurred during MRZ scanning or processing.
     * 
     * This result is emitted when:
     * - Camera initialization fails
     * - ML Kit OCR processing fails
     * - Invalid MRZ format is detected
     * - Checksum validation fails
     * - Any other processing error occurs
     * 
     * @property message Human-readable error message describing what went wrong
     * @property exception Optional exception that caused the error, useful for debugging
     */
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : MRZResult() {
        override fun toString(): String {
            return "MRZResult.Error(message='$message', exception=${exception?.javaClass?.simpleName})"
        }
    }
    
    /**
     * Indicates that MRZ scanning is in progress.
     * 
     * This is the initial state when scanning starts and represents the ongoing
     * process of capturing frames and attempting to detect MRZ data.
     * 
     * This state can be used to:
     * - Show a scanning indicator/animation to the user
     * - Keep the camera preview active
     * - Indicate that the system is actively looking for MRZ
     * - Display helpful feedback messages to guide the user
     * 
     * @property detectionInfo Optional information about detected text regions (even if not valid MRZ)
     * @property message Optional message to display to user (e.g., "Hold steady", "Parse failed: retrying")
     */
    data class Scanning(
        val detectionInfo: MRZDetectionInfo? = null,
        val message: String? = null
    ) : MRZResult() {
        override fun toString(): String {
            return if (message != null) {
                "MRZResult.Scanning(message='$message')"
            } else {
                "MRZResult.Scanning"
            }
        }
    }
    
    companion object {
        /**
         * Check if the result represents a successful scan.
         * 
         * @return true if this is a Success result, false otherwise
         */
        fun MRZResult.isSuccess(): Boolean = this is Success
        
        /**
         * Check if the result represents an error state.
         * 
         * @return true if this is an Error result, false otherwise
         */
        fun MRZResult.isError(): Boolean = this is Error
        
        /**
         * Check if scanning is currently in progress.
         * 
         * @return true if this is a Scanning result, false otherwise
         */
        fun MRZResult.isScanning(): Boolean = this is Scanning
        
        /**
         * Safely extract detection info from any result that contains it.
         * 
         * @return MRZDetectionInfo if available, null otherwise
         */
        fun MRZResult.getDetectionInfoOrNull(): MRZDetectionInfo? = when (this) {
            is Success -> detectionInfo
            is Scanning -> detectionInfo
            else -> null
        }
        
        /**
         * Safely extract MRZData if the result is Success, null otherwise.
         * 
         * @return MRZData if Success, null otherwise
         */
        fun MRZResult.getDataOrNull(): MRZData? = (this as? Success)?.data
        
        /**
         * Safely extract error message if the result is Error, null otherwise.
         * 
         * @return Error message if Error, null otherwise
         */
        fun MRZResult.getErrorOrNull(): String? = (this as? Error)?.message
    }
}
