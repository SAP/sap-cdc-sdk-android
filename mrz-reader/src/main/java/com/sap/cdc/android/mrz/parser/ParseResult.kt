package com.sap.cdc.android.mrz.parser

import com.sap.cdc.android.mrz.model.MRZData

/**
 * Represents the result of an MRZ parsing operation.
 * 
 * This sealed class provides type-safe handling of parsing outcomes:
 * - Success: MRZ was successfully parsed and validated
 * - Failure: Parsing failed with detailed error information
 */
sealed class ParseResult {
    
    /**
     * Indicates successful MRZ parsing with all validations passed.
     * 
     * This result means:
     * - All required fields were extracted
     * - All checksums validated successfully
     * - Dates were parsed correctly
     * - MRZData is complete and valid
     * 
     * @property data The parsed and validated MRZ data
     */
    data class Success(val data: MRZData) : ParseResult() {
        override fun toString(): String {
            return "ParseResult.Success(${data.fullName}, valid=${data.isValid})"
        }
    }
    
    /**
     * Indicates parsing failure with detailed error information.
     * 
     * This result means:
     * - One or more validation checks failed
     * - Format was invalid
     * - Required fields could not be extracted
     * - Checksums did not match
     * 
     * The errors list provides specific information about what went wrong,
     * enabling precise debugging and user feedback.
     * 
     * @property errors List of all errors encountered during parsing
     */
    data class Failure(val errors: List<ParseError>) : ParseResult() {
        override fun toString(): String {
            return "ParseResult.Failure(${errors.size} errors: ${errors.summarize()})"
        }
        
        /**
         * Get a user-friendly error message summarizing all errors.
         */
        fun getMessage(): String = errors.summarize()
    }
    
    companion object {
        /**
         * Check if this result represents a successful parse.
         */
        fun ParseResult.isSuccess(): Boolean = this is Success
        
        /**
         * Check if this result represents a failed parse.
         */
        fun ParseResult.isFailure(): Boolean = this is Failure
        
        /**
         * Safely extract MRZData if successful, null otherwise.
         */
        fun ParseResult.getDataOrNull(): MRZData? = (this as? Success)?.data
        
        /**
         * Safely extract error list if failed, empty list otherwise.
         */
        fun ParseResult.getErrorsOrEmpty(): List<ParseError> = (this as? Failure)?.errors ?: emptyList()
    }
}
