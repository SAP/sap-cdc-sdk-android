package com.sap.cdc.android.mrz.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility for parsing and formatting MRZ dates using Kotlin/Java standard library.
 * 
 * MRZ dates are in YYMMDD format (e.g., "740812" for August 12, 1974).
 * This utility converts them to LocalDate and ISO 8601 string format (YYYY-MM-DD).
 * 
 * Century rules per ICAO 9303:
 * - Years 00-29 → 2000-2029
 * - Years 30-99 → 1930-1999
 */
object DateParser {
    
    private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    /**
     * Result of date parsing operation.
     */
    sealed class DateParseResult {
        data class Success(val date: LocalDate, val formatted: String) : DateParseResult()
        data class Failure(val reason: String) : DateParseResult()
    }
    
    /**
     * Parse MRZ date string (YYMMDD) to LocalDate and ISO format string.
     * 
     * Examples:
     * - "740812" → LocalDate(1974, 8, 12) → "1974-08-12"
     * - "250630" → LocalDate(2025, 6, 30) → "2025-06-30"
     * - "990101" → LocalDate(1999, 1, 1) → "1999-01-01"
     * 
     * @param yymmdd Raw date string from MRZ (6 characters)
     * @return DateParseResult.Success with LocalDate and formatted string, or Failure with reason
     */
    fun parseDate(yymmdd: String): DateParseResult {
        // Validate length
        if (yymmdd.length != 6) {
            return DateParseResult.Failure("Date must be 6 characters, got ${yymmdd.length}")
        }
        
        // Validate all digits
        if (!yymmdd.all { it.isDigit() }) {
            return DateParseResult.Failure("Date must contain only digits")
        }
        
        return try {
            val yy = yymmdd.substring(0, 2).toInt()
            val mm = yymmdd.substring(2, 4).toInt()
            val dd = yymmdd.substring(4, 6).toInt()
            
            // Validate month
            if (mm < 1 || mm > 12) {
                return DateParseResult.Failure("Invalid month: $mm (must be 1-12)")
            }
            
            // Validate day (rough check, LocalDate will validate precisely)
            if (dd < 1 || dd > 31) {
                return DateParseResult.Failure("Invalid day: $dd (must be 1-31)")
            }
            
            // Determine century per ICAO 9303 rules
            val year = if (yy < 30) 2000 + yy else 1900 + yy
            
            // Create LocalDate (will throw if invalid, e.g., Feb 30)
            val date = LocalDate.of(year, mm, dd)
            val formatted = date.format(ISO_DATE_FORMATTER)
            
            DateParseResult.Success(date, formatted)
            
        } catch (e: Exception) {
            DateParseResult.Failure("Date parsing failed: ${e.message}")
        }
    }
    
    /**
     * Convenience method to parse date and return ISO format string directly.
     * 
     * @param yymmdd Raw date string from MRZ
     * @return ISO format string (YYYY-MM-DD) or null if parsing fails
     */
    fun parseDateToString(yymmdd: String): String? {
        return when (val result = parseDate(yymmdd)) {
            is DateParseResult.Success -> result.formatted
            is DateParseResult.Failure -> null
        }
    }
    
    /**
     * Convenience method to parse date and return LocalDate directly.
     * 
     * @param yymmdd Raw date string from MRZ
     * @return LocalDate or null if parsing fails
     */
    fun parseDateToLocalDate(yymmdd: String): LocalDate? {
        return when (val result = parseDate(yymmdd)) {
            is DateParseResult.Success -> result.date
            is DateParseResult.Failure -> null
        }
    }
    
    /**
     * Check if a date is expired (before today).
     * 
     * @param yymmdd Raw date string from MRZ
     * @return true if date is before today, false if today or future, null if parsing fails
     */
    fun isExpired(yymmdd: String): Boolean? {
        val date = parseDateToLocalDate(yymmdd) ?: return null
        val today = LocalDate.now()
        return date.isBefore(today)
    }
}
