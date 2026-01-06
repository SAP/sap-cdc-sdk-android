package com.sap.cdc.android.mrz.parser.td2

import com.sap.cdc.android.mrz.parser.ParseError
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD2Parser
import com.sap.cdc.android.mrz.parser.hasChecksumErrors
import com.sap.cdc.android.mrz.parser.hasFormatErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD2Parser with invalid MRZ examples.
 * 
 * Tests error handling and validation with malformed data.
 */
class TD2ParserInvalidTest {
    
    private lateinit var parser: TD2Parser
    
    @Before
    fun setup() {
        parser = TD2Parser()
    }
    
    @Test
    fun `parse fails with wrong number of lines - only one`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<"
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6",
            "EXTRA<LINE<<<<<<<<<<<<<<<<<<<<<<<<<"  // Extra line
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with wrong line length - line 1 too short`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<",  // 27 chars instead of 36
            "D231458907UTO7408122F1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val lengthError = errors.filterIsInstance<ParseError.InvalidLength>().firstOrNull()
        assertNotNull(lengthError)
        assertEquals(36, lengthError?.expected)
        assertEquals(27, lengthError?.actual)
    }
    
    @Test
    fun `parse fails with wrong line length - line 2 too long`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6EXTRA"  // 41 chars
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with invalid characters - lowercase`() {
        val lines = listOf(
            "I<utoeriksson<<ANNA<MARIA<<<<<<<<<<<",  // lowercase
            "D231458907UTO7408122F1204159<<<<<<<6"
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159@@#####6"  // @ and # symbols
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458908UTO7408122F1204159<<<<<<<6"  // Wrong checksum (8 instead of 7)
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408129F1204159<<<<<<<6"  // Wrong DOB checksum (9 instead of 2)
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204150<<<<<<<6"  // Wrong expiry checksum (0 instead of 9)
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "expirationDate" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid composite checksum`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<0"  // Wrong composite (0 instead of 6)
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7413012F1204159<<<<<<<6"  // Month 13
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7402302F1204159<<<<<<<6"  // Feb 30
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
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458909UTO7408129F1204150<<<<<<<0"  // All 4 checksums wrong
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumErrors = errors.filterIsInstance<ParseError.ChecksumFailed>()
        assertTrue(checksumErrors.size >= 4)
    }
    
    @Test
    fun `validateFormat rejects wrong line count`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `validateFormat accepts correct format`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isEmpty())
    }
}
