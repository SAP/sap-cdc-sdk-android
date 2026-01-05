package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ParseError
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD1Parser
import com.sap.cdc.android.mrz.parser.hasChecksumErrors
import com.sap.cdc.android.mrz.parser.hasFormatErrors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD1Parser with invalid MRZ examples.
 * 
 * Tests error handling and validation with malformed data.
 */
class TD1ParserInvalidTest {
    
    private lateinit var parser: TD1Parser
    
    @Before
    fun setup() {
        parser = TD1Parser()
    }
    
    @Test
    fun `parse fails with wrong number of lines - too few`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<"
            // Missing line 3
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
        assertTrue(errors.hasFormatErrors())
    }
    
    @Test
    fun `parse fails with wrong number of lines - too many`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "EXTRA<LINE<<<<<<<<<<<<<<<<<<<<"  // Extra line
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with wrong line length - line 1 too short`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<",  // 22 chars instead of 30
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val lengthError = errors.filterIsInstance<ParseError.InvalidLength>().firstOrNull()
        assertNotNull(lengthError)
        assertEquals(30, lengthError?.expected)
        assertEquals(22, lengthError?.actual)
    }
    
    @Test
    fun `parse fails with wrong line length - line 2 too long`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<<<<<<",  // 35 chars instead of 30
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with invalid characters - lowercase`() {
        val lines = listOf(
            "I<utod231458907<<<<<<<<<<<<<<<",  // lowercase 'u' and 't'
            "7408122F1204159UTO<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<"
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
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO@#<<<<<<<<<",  // @ and # symbols
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<"
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
            "I<UTOD231458908<<<<<<<<<<<<<<<",  // Wrong checksum (8 instead of 7)
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
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
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408129F1204159UTO<<<<<<<<<<<<",  // Wrong DOB checksum (9 instead of 2)
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
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
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204150UTO<<<<<<<<<<<<",  // Wrong expiry checksum (0 instead of 9)
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "expirationDate" }
        assertNotNull(checksumError)
    }
    
    @Test
    fun `parse fails with invalid date format - month 13`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7413012F1204159UTO<<<<<<<<<<<<",  // Month 13 instead of valid month
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
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
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7402302F1204159UTO<<<<<<<<<<<<",  // Feb 30 is invalid
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidDate })
    }
    
    @Test
    fun `parse fails with empty lines`() {
        val lines = listOf("", "", "")
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `parse fails with all checksums invalid`() {
        val lines = listOf(
            "I<UTOD231458909<<<<<<<<<<<<<<<",  // Wrong doc checksum
            "7408129F1204150UTO<<<<<<<<<<<<",   // Wrong DOB and expiry checksums
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        // Should have 3 checksum errors
        val checksumErrors = errors.filterIsInstance<ParseError.ChecksumFailed>()
        assertTrue(checksumErrors.size >= 3)
    }
    
    @Test
    fun `validateFormat rejects wrong line count`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it is ParseError.InvalidLength })
    }
    
    @Test
    fun `validateFormat accepts correct format`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun `parse with missing required fields returns failure`() {
        val lines = listOf(
            "I<UTO<<<<<<<<<<<<<<<<<<<<<<<<<",  // No document number
            "<<<<<<<<<<<<<<<<<UTO<<<<<<<<<<",  // No dates
            "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"  // No name
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        assertTrue(errors.any { it is ParseError.MissingRequiredField })
    }
}
