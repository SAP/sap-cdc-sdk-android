package com.sap.cdc.android.mrz.parser

import com.sap.cdc.android.mrz.model.MRZFormat

/**
 * Protocol defining the contract for MRZ parsing implementations.
 * 
 * Each MRZ format (TD1, TD3, etc.) has its own parser implementation
 * following this protocol. This enables:
 * - Easy addition of new format parsers
 * - Clear separation of concerns
 * - Consistent parsing interface
 * - Independent testing of each parser
 * 
 * Example usage:
 * ```kotlin
 * val parser = TD1Parser()
 * when (val result = parser.parse(lines)) {
 *     is ParseResult.Success -> handleData(result.data)
 *     is ParseResult.Failure -> handleError(result.getMessage())
 * }
 * ```
 */
interface MRZParser {
    
    /**
     * The MRZ format this parser handles.
     */
    val supportedFormat: MRZFormat
    
    /**
     * Parse MRZ lines into structured, validated data.
     * 
     * This method:
     * 1. Validates the format (line count, character length, character set)
     * 2. Extracts fields from their specified positions
     * 3. Validates checksums per ICAO 9303 standard
     * 4. Parses dates into proper date types
     * 5. Returns Success with MRZData or Failure with detailed errors
     * 
     * @param lines Raw MRZ text lines from OCR
     * @return ParseResult containing either validated MRZData or list of errors
     */
    fun parse(lines: List<String>): ParseResult
    
    /**
     * Validate MRZ format before parsing.
     * 
     * Checks:
     * - Correct number of lines
     * - Correct characters per line
     * - Valid character set (uppercase, digits, '<' only)
     * 
     * @param lines Raw MRZ text lines to validate
     * @return List of format validation errors (empty if format is valid)
     */
    fun validateFormat(lines: List<String>): List<ParseError>
}
