package com.sap.cdc.android.mrz.parser

/**
 * Utility for extracting and cleaning fields from MRZ lines.
 * 
 * Provides common operations for field extraction with error handling:
 * - Safe substring extraction
 * - Filler character removal
 * - Name parsing (surname << given names)
 * - Type code parsing
 */
object FieldExtractor {
    
    /**
     * Result of field extraction operation.
     */
    sealed class ExtractionResult {
        data class Success(val value: String) : ExtractionResult()
        data class Failure(val error: ParseError) : ExtractionResult()
    }
    
    /**
     * Safely extract substring from line with bounds checking.
     * 
     * @param line The source line
     * @param range Character position range to extract
     * @param fieldName Name of the field for error reporting
     * @return ExtractionResult with extracted string or error
     */
    fun extractField(line: String, range: IntRange, fieldName: String): ExtractionResult {
        return try {
            if (range.last >= line.length) {
                ExtractionResult.Failure(
                    ParseError.FieldExtractionFailed(
                        field = fieldName,
                        position = range,
                        reason = "Range ${range.first}-${range.last} exceeds line length ${line.length}"
                    )
                )
            } else {
                val value = line.substring(range.first, range.last + 1)
                ExtractionResult.Success(value)
            }
        } catch (e: Exception) {
            ExtractionResult.Failure(
                ParseError.FieldExtractionFailed(
                    field = fieldName,
                    position = range,
                    reason = e.message ?: "Unknown extraction error"
                )
            )
        }
    }
    
    /**
     * Extract field and remove filler characters ('<').
     * 
     * Example: "L898902C3<" → "L898902C3"
     * 
     * @param line The source line
     * @param range Character position range to extract
     * @param fieldName Name of the field for error reporting
     * @return ExtractionResult with cleaned string or error
     */
    fun extractAndClean(line: String, range: IntRange, fieldName: String): ExtractionResult {
        return when (val result = extractField(line, range, fieldName)) {
            is ExtractionResult.Success -> {
                val cleaned = result.value.replace("<", "")
                ExtractionResult.Success(cleaned)
            }
            is ExtractionResult.Failure -> result
        }
    }
    
    /**
     * Parse name field which uses "<<" as separator between surname and given names.
     * Single '<' characters within names are converted to spaces.
     * 
     * Examples:
     * - "ERIKSSON<<ANNA<MARIA" → ("ERIKSSON", "ANNA MARIA")
     * - "SMITH<<JOHN" → ("SMITH", "JOHN")
     * - "DOE<<" → ("DOE", "")
     * 
     * @param nameSection The name section from MRZ (already extracted)
     * @return Pair of (surname, givenNames)
     */
    fun parseName(nameSection: String): Pair<String, String> {
        val parts = nameSection.split("<<")
        
        val surname = parts.getOrNull(0)
            ?.replace("<", " ")
            ?.trim()
            ?: ""
        
        val givenNames = parts.getOrNull(1)
            ?.replace("<", " ")
            ?.trim()
            ?: ""
        
        return Pair(surname, givenNames)
    }
    
    /**
     * Extract a single character field (like document type or gender).
     * 
     * @param line The source line
     * @param position Character position (0-based)
     * @param fieldName Name of the field for error reporting
     * @return ExtractionResult with character as string or error
     */
    fun extractChar(line: String, position: Int, fieldName: String): ExtractionResult {
        return extractField(line, position..position, fieldName)
    }
    
    /**
     * Extract checksum digit from specified position.
     * 
     * @param line The source line
     * @param position Character position (0-based)
     * @param fieldName Name of the field this checksum validates
     * @return Checksum digit (0-9) or null if invalid
     */
    fun extractChecksum(line: String, position: Int, fieldName: String): Int? {
        return when (val result = extractChar(line, position, "$fieldName checksum")) {
            is ExtractionResult.Success -> result.value.firstOrNull()?.digitToIntOrNull()
            is ExtractionResult.Failure -> null
        }
    }
}
