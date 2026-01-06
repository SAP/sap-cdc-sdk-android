package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.parser.ParseError
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD3Parser
import com.sap.cdc.android.mrz.parser.hasChecksumErrors
import com.sap.cdc.android.mrz.parser.hasFormatErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD3Parser with invalid MRZ examples.
 * 
 * Tests error handling and validation with malformed passport data.
 */
class TD3ParserInvalidTest {
    
    private lateinit var parser: TD3Parser
    
    @Before
    fun setup() {
        parser = TD3Parser()
    }
    
    @Test
    fun `parse fails with wrong number of lines - only one`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            // Missing line 2
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
        assertTrue(errors.hasFormatErrors())
    }
    
    @Test
    fun `parse fails with wrong number of lines - three lines`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10",
            "EXTRA<LINE<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"  // Extra line
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with wrong line length - line 1 too short`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<",  // 28 chars instead of 44
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val lengthError = errors.filterIsInstance<ParseError.InvalidLength>().firstOrNull()
        assertNotNull(lengthError)
        assertEquals(44, lengthError?.expected)
        assertEquals(28, lengthError?.actual)
    }
    
    @Test
    fun `parse fails with wrong line length - line 2 too long`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10EXTRA"  // 49 chars
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with invalid characters - lowercase`() {
        val lines = listOf(
            "P<utoeriksson<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",  // lowercase
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val charError = errors.filterIsInstance<ParseError.InvalidCharacters>().firstOrNull()
        assertNotNull(charError)
        assertTrue(charError!!.invalidChars.contains('u'))
    }
    
    @Test
    fun `parse fails with invalid characters - special symbols`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159@@###########+10"  // @ and # symbols
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val charError = errors.filterIsInstance<ParseError.InvalidCharacters>().firstOrNull()
        assertNotNull(charError)
        assertTrue(charError!!.invalidChars.contains('@'))
        assertTrue(charError.invalidChars.contains('#'))
    }
    
    @Test
    fun `parse fails with invalid document number checksum`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C35UTO7408122F1204159ZE184226B<<<<<10"  // Wrong checksum (5 instead of 6)
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.hasChecksumErrors())
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "documentNumber" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid date of birth checksum`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408129F1204159ZE184226B<<<<<10"  // Wrong DOB checksum (9 instead of 2)
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "dateOfBirth" }
        assertNotNull(checksumError)
        assertEquals(2, checksumError?.calculated)
        assertEquals(9, checksumError?.expected)
    }
    
    @Test
    fun `parse fails with invalid expiration date checksum`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204150ZE184226B<<<<<10"  // Wrong expiry checksum (0 instead of 9)
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "expirationDate" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid personal number checksum`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<90"  // Wrong personal checksum (9 instead of 2), composite will also fail
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "personalNumber" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid composite checksum`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<29"  // Wrong composite (9 instead of 0)
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "composite" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid date format - month 13`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7413012F1204159ZE184226B<<<<<10"  // Month 13
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val dateError = errors.filterIsInstance<ParseError.InvalidDate>()
            .find { it.field == "dateOfBirth" }
        assertNotNull(dateError)
        assertTrue(dateError!!.reason.contains("month"))
    }
    
    @Test
    fun `parse fails with invalid date - February 30`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7402302F1204159ZE184226B<<<<<10"  // Feb 30
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidDate })
    }
    
    @Test
    fun `parse fails with empty lines`() {
        val lines = listOf("", "")
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with all checksums invalid`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C35UTO7408129F1204150ZE184226B<<<<<99"  // All 5 checksums wrong
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumErrors = errors.filterIsInstance<ParseError.ChecksumFailed>()
        assertTrue(checksumErrors.size >= 4)  // At least doc, dob, expiry, composite
    }
    
    @Test
    fun `validateFormat rejects wrong line count`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `validateFormat accepts correct format`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isEmpty())
    }
}
