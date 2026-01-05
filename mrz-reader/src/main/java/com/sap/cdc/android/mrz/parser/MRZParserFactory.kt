package com.sap.cdc.android.mrz.parser

import com.sap.cdc.android.mrz.model.MRZFormat

/**
 * Factory for creating MRZ parsers based on detected format.
 * 
 * This factory:
 * - Detects MRZ format from line dimensions
 * - Instantiates appropriate parser
 * - Provides convenience methods for parsing
 * 
 * Example usage:
 * ```kotlin
 * val lines = listOf(
 *     "I<UTOD23145890<<<<<<<<<<<<<",
 *     "7408122F1204159UTO<<<<<<<<<<<",
 *     "ERIKSSON<<ANNA<MARIA<<<<<<<<<<<"
 * )
 * 
 * when (val result = MRZParserFactory.detectAndParse(lines)) {
 *     is ParseResult.Success -> println(result.data)
 *     is ParseResult.Failure -> println(result.getMessage())
 * }
 * ```
 */
object MRZParserFactory {
    
    /**
     * Get parser instance for specified format.
     * 
     * Currently supported:
     * - TD1 (ID cards)
     * 
     * Future support:
     * - TD3 (passports)
     * - TD2 (some passports)
     * 
     * @param format The MRZ format
     * @return Parser instance or null if format not supported
     */
    fun getParser(format: MRZFormat): MRZParser? {
        return when (format) {
            MRZFormat.TD1 -> TD1Parser()
            // Future parsers will be added here:
            // MRZFormat.TD3 -> TD3Parser()
            // MRZFormat.TD2 -> TD2Parser()
            else -> null  // TD2, TD3, MRVA, MRVB not yet implemented
        }
    }
    
    /**
     * Detect MRZ format and parse lines in one operation.
     * 
     * This is the primary entry point for parsing MRZ data.
     * It automatically:
     * 1. Detects format from line count and length
     * 2. Gets appropriate parser
     * 3. Parses the lines
     * 4. Returns result with detailed errors if parsing fails
     * 
     * @param lines Raw MRZ text lines from OCR
     * @return ParseResult with MRZData or detailed errors
     */
    fun detectAndParse(lines: List<String>): ParseResult {
        // Check if we have any lines
        if (lines.isEmpty()) {
            return ParseResult.Failure(
                listOf(
                    ParseError.InvalidLength(
                        expected = 2,  // Minimum for any format
                        actual = 0,
                        lineNumber = 0
                    )
                )
            )
        }
        
        // Detect format from line dimensions
        val lineCount = lines.size
        val firstLineLength = lines.firstOrNull()?.length ?: 0

        val format = MRZFormat.detect(lineCount, firstLineLength) ?: return ParseResult.Failure(
            listOf(
                ParseError.UnsupportedFormat(
                    detectedFormat = "$lineCount lines × $firstLineLength chars"
                )
            )
        )

        // Get parser for detected format
        val parser = getParser(format) ?: return ParseResult.Failure(
            listOf(
                ParseError.UnsupportedFormat(
                    detectedFormat = "${format.name} (not yet implemented)"
                )
            )
        )

        // Parse with detected parser
        return parser.parse(lines)
    }
    
    /**
     * Check if a format is currently supported.
     * 
     * @param format The MRZ format to check
     * @return true if parser is available for this format
     */
    fun isFormatSupported(format: MRZFormat): Boolean {
        return getParser(format) != null
    }
    
    /**
     * Get list of all currently supported formats.
     * 
     * @return Set of supported MRZFormat values
     */
    fun getSupportedFormats(): Set<MRZFormat> {
        return MRZFormat.entries.filter { isFormatSupported(it) }.toSet()
    }
}
