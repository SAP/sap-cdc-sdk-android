package com.sap.cdc.android.mrz.parser

/**
 * Represents specific errors that can occur during MRZ parsing.
 * 
 * This sealed class hierarchy provides granular error tracking, enabling:
 * - Precise debugging of parsing failures
 * - Analytics on common failure modes
 * - User-friendly error messages
 * - Comprehensive test coverage
 */
sealed class ParseError {
    
    /**
     * Invalid line length detected.
     * 
     * @property expected Expected number of characters for this format
     * @property actual Actual number of characters found
     * @property lineNumber Line number (1-based) where error occurred
     */
    data class InvalidLength(
        val expected: Int,
        val actual: Int,
        val lineNumber: Int
    ) : ParseError() {
        override fun toMessage(): String {
            return "Line $lineNumber: Expected $expected characters, found $actual"
        }
    }
    
    /**
     * Invalid characters found in MRZ line.
     * MRZ only allows uppercase letters, digits, and '<' character.
     * 
     * @property invalidChars Set of invalid characters found
     * @property lineNumber Line number (1-based) where error occurred
     */
    data class InvalidCharacters(
        val invalidChars: Set<Char>,
        val lineNumber: Int
    ) : ParseError() {
        override fun toMessage(): String {
            val chars = invalidChars.joinToString(", ") { "'$it'" }
            return "Line $lineNumber: Invalid characters found: $chars"
        }
    }
    
    /**
     * Failed to extract a required field from its expected position.
     * 
     * @property field Name of the field (e.g., "documentNumber", "dateOfBirth")
     * @property position Character position range where extraction was attempted
     * @property reason Why extraction failed
     */
    data class FieldExtractionFailed(
        val field: String,
        val position: IntRange,
        val reason: String = "Substring extraction failed"
    ) : ParseError() {
        override fun toMessage(): String {
            return "Failed to extract '$field' from position ${position.first}-${position.last}: $reason"
        }
    }
    
    /**
     * Checksum validation failed for a specific field.
     * 
     * @property field Name of the field being validated
     * @property data The data string used for checksum calculation
     * @property expected Expected checksum digit from MRZ
     * @property calculated Calculated checksum digit
     */
    data class ChecksumFailed(
        val field: String,
        val data: String,
        val expected: Int,
        val calculated: Int
    ) : ParseError() {
        override fun toMessage(): String {
            return "Checksum failed for '$field': expected $expected, calculated $calculated"
        }
    }
    
    /**
     * Invalid date format or value detected.
     * 
     * @property rawDate The raw date string from MRZ (YYMMDD format)
     * @property field Field name (e.g., "dateOfBirth", "expirationDate")
     * @property reason Why date parsing failed
     */
    data class InvalidDate(
        val rawDate: String,
        val field: String,
        val reason: String = "Invalid date format"
    ) : ParseError() {
        override fun toMessage(): String {
            return "Invalid date for '$field': '$rawDate' - $reason"
        }
    }
    
    /**
     * Required field is missing or empty.
     * 
     * @property field Name of the missing field
     */
    data class MissingRequiredField(
        val field: String
    ) : ParseError() {
        override fun toMessage(): String {
            return "Required field missing: '$field'"
        }
    }
    
    /**
     * Composite checksum validation failed (TD3 only).
     * The final checksum validates all previous fields together.
     * 
     * @property compositeData The composite data string used for validation
     * @property expected Expected checksum digit
     * @property calculated Calculated checksum digit
     */
    data class CompositeChecksumFailed(
        val compositeData: String,
        val expected: Int,
        val calculated: Int
    ) : ParseError() {
        override fun toMessage(): String {
            return "Composite checksum failed: expected $expected, calculated $calculated"
        }
    }
    
    /**
     * Unsupported MRZ format detected.
     * 
     * @property detectedFormat The format that was detected but not supported
     */
    data class UnsupportedFormat(
        val detectedFormat: String
    ) : ParseError() {
        override fun toMessage(): String {
            return "Unsupported MRZ format: $detectedFormat"
        }
    }
    
    /**
     * Convert this error to a human-readable message.
     */
    abstract fun toMessage(): String
}

/**
 * Extension functions for working with ParseError lists.
 */
fun List<ParseError>.summarize(): String {
    return when {
        isEmpty() -> "Unknown parsing error"
        size == 1 -> first().toMessage()
        else -> "Multiple errors: ${first().toMessage()} (and ${size - 1} more)"
    }
}

/**
 * Check if errors contain any checksum failures.
 */
fun List<ParseError>.hasChecksumErrors(): Boolean {
    return any { it is ParseError.ChecksumFailed || it is ParseError.CompositeChecksumFailed }
}

/**
 * Check if errors contain any format validation errors.
 */
fun List<ParseError>.hasFormatErrors(): Boolean {
    return any { it is ParseError.InvalidLength || it is ParseError.InvalidCharacters }
}
