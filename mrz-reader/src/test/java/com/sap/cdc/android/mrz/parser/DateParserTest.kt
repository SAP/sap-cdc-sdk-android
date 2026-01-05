package com.sap.cdc.android.mrz.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for DateParser.
 * 
 * Tests date parsing with various valid and invalid inputs,
 * including century boundary handling.
 */
class DateParserTest {
    
    @Test
    fun `parseDate with valid date in 1900s`() {
        val result = DateParser.parseDate("740812")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(1974, 8, 12), success.date)
        assertEquals("1974-08-12", success.formatted)
    }
    
    @Test
    fun `parseDate with valid date in 2000s`() {
        val result = DateParser.parseDate("250630")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(2025, 6, 30), success.date)
        assertEquals("2025-06-30", success.formatted)
    }
    
    @Test
    fun `parseDate with century boundary year 30`() {
        // Year 30 should be 1930 (>= 30 rule)
        val result = DateParser.parseDate("300101")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(1930, 1, 1), success.date)
    }
    
    @Test
    fun `parseDate with century boundary year 29`() {
        // Year 29 should be 2029 (< 30 rule)
        val result = DateParser.parseDate("291231")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(2029, 12, 31), success.date)
    }
    
    @Test
    fun `parseDate with year 00`() {
        val result = DateParser.parseDate("000101")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(2000, 1, 1), success.date)
    }
    
    @Test
    fun `parseDate with year 99`() {
        val result = DateParser.parseDate("991231")
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(1999, 12, 31), success.date)
    }
    
    @Test
    fun `parseDate with leap year February 29`() {
        val result = DateParser.parseDate("000229")  // 2000 was a leap year
        
        assertTrue(result is DateParser.DateParseResult.Success)
        val success = result as DateParser.DateParseResult.Success
        assertEquals(LocalDate.of(2000, 2, 29), success.date)
    }
    
    @Test
    fun `parseDate with invalid February 30`() {
        val result = DateParser.parseDate("250230")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("Date parsing failed"))
    }
    
    @Test
    fun `parseDate with invalid month 13`() {
        val result = DateParser.parseDate("251301")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("Invalid month"))
    }
    
    @Test
    fun `parseDate with invalid month 00`() {
        val result = DateParser.parseDate("250001")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("Invalid month"))
    }
    
    @Test
    fun `parseDate with invalid day 32`() {
        val result = DateParser.parseDate("250132")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("Invalid day") || failure.reason.contains("Date parsing failed"))
    }
    
    @Test
    fun `parseDate with invalid day 00`() {
        val result = DateParser.parseDate("250100")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("Invalid day"))
    }
    
    @Test
    fun `parseDate with wrong length`() {
        val result = DateParser.parseDate("74081")  // 5 chars instead of 6
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("must be 6 characters"))
    }
    
    @Test
    fun `parseDate with non-numeric characters`() {
        val result = DateParser.parseDate("74AB12")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
        val failure = result as DateParser.DateParseResult.Failure
        assertTrue(failure.reason.contains("only digits"))
    }
    
    @Test
    fun `parseDate with empty string`() {
        val result = DateParser.parseDate("")
        
        assertTrue(result is DateParser.DateParseResult.Failure)
    }
    
    @Test
    fun `parseDateToString returns formatted date`() {
        val formatted = DateParser.parseDateToString("740812")
        
        assertEquals("1974-08-12", formatted)
    }
    
    @Test
    fun `parseDateToString returns null for invalid date`() {
        val formatted = DateParser.parseDateToString("invalid")
        
        assertNull(formatted)
    }
    
    @Test
    fun `parseDateToLocalDate returns LocalDate`() {
        val date = DateParser.parseDateToLocalDate("250630")
        
        assertNotNull(date)
        assertEquals(LocalDate.of(2025, 6, 30), date)
    }
    
    @Test
    fun `parseDateToLocalDate returns null for invalid date`() {
        val date = DateParser.parseDateToLocalDate("999999")
        
        assertNull(date)
    }
    
    @Test
    fun `isExpired with past date returns true`() {
        // Date far in the past
        val expired = DateParser.isExpired("900101")
        
        assertNotNull(expired)
        assertTrue(expired!!)
    }
    
    @Test
    fun `isExpired with future date returns false`() {
        // Date in 2029 (future)
        val expired = DateParser.isExpired("291231")
        
        assertNotNull(expired)
        assertFalse(expired!!)
    }
    
    @Test
    fun `isExpired with invalid date returns null`() {
        val expired = DateParser.isExpired("invalid")
        
        assertNull(expired)
    }
}
