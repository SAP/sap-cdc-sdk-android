package com.sap.cdc.android.mrz.parser.td2

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import com.sap.cdc.android.mrz.parser.ParseError
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD2Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Checksum-specific tests for TD2Parser.
 * 
 * TD2 format has 4 checksums:
 * 1. Document number checksum (line 2, position 10)
 * 2. Date of birth checksum (line 2, position 20)
 * 3. Expiration date checksum (line 2, position 28)
 * 4. Composite checksum (line 2, position 36)
 */
class TD2ChecksumTest {
    
    private lateinit var parser: TD2Parser
    
    @Before
    fun setup() {
        parser = TD2Parser()
    }
    
    @Test
    fun `all checksums valid - parsing succeeds`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        assertTrue(data.isValid)
    }
    
    @Test
    fun `document number checksum calculation`() {
        val docNum = "D23145890"
        val expectedChecksum = 7
        
        val calculated = ChecksumValidator.calculateChecksum(docNum)
        
        assertEquals(expectedChecksum, calculated)
    }
    
    @Test
    fun `document number checksum failure detected`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458909UTO7408122F1204159<<<<<<<6"  // Doc checksum 9 instead of 7
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "documentNumber" }
        assertNotNull(checksumError)
        assertEquals(7, checksumError?.calculated)
        assertEquals(9, checksumError?.expected)
    }
    
    @Test
    fun `date of birth checksum calculation`() {
        val dob = "740812"
        val expectedChecksum = 2
        
        val calculated = ChecksumValidator.calculateChecksum(dob)
        
        assertEquals(expectedChecksum, calculated)
    }
    
    @Test
    fun `date of birth checksum failure detected`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408125F1204159<<<<<<<6"  // DOB checksum 5 instead of 2
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "dateOfBirth" }
        assertNotNull(checksumError)
        assertEquals(2, checksumError?.calculated)
        assertEquals(5, checksumError?.expected)
    }
    
    @Test
    fun `expiration date checksum calculation`() {
        val expiry = "120415"
        val expectedChecksum = 9
        
        val calculated = ChecksumValidator.calculateChecksum(expiry)
        
        assertEquals(expectedChecksum, calculated)
    }
    
    @Test
    fun `expiration date checksum failure detected`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204153<<<<<<<6"  // Expiry checksum 3 instead of 9
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "expirationDate" }
        assertNotNull(checksumError)
        assertEquals(9, checksumError?.calculated)
        assertEquals(3, checksumError?.expected)
    }
    
    @Test
    fun `composite checksum calculation`() {
        // Composite = DocNum+Checksum(10) + DOB+Checksum(7) + Expiry+Checksum+Optional(14)
        val compositeData = "D231458907" + "7408122" + "1204159" + "<<<<<<<"
        val expectedChecksum = 6
        
        val calculated = ChecksumValidator.calculateChecksum(compositeData)
        
        assertEquals(expectedChecksum, calculated)
    }
    
    @Test
    fun `composite checksum failure detected`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<3"  // Composite 3 instead of 6
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumError = errors.filterIsInstance<ParseError.ChecksumFailed>()
            .find { it.field == "composite" }
        assertNotNull(checksumError)
        assertEquals(6, checksumError?.calculated)
        assertEquals(3, checksumError?.expected)
    }
    
    @Test
    fun `multiple checksum failures detected`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458908UTO7408125F1204153<<<<<<<0"  // All 4 checksums wrong
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Failure)
        val errors = (result as ParseResult.Failure).errors
        
        val checksumErrors = errors.filterIsInstance<ParseError.ChecksumFailed>()
        assertEquals(4, checksumErrors.size)
        
        assertTrue(checksumErrors.any { it.field == "documentNumber" })
        assertTrue(checksumErrors.any { it.field == "dateOfBirth" })
        assertTrue(checksumErrors.any { it.field == "expirationDate" })
        assertTrue(checksumErrors.any { it.field == "composite" })
    }
    
    @Test
    fun `document number with fillers - checksum valid`() {
        val lines = listOf(
            "I<UTOSMITH<<JANE<<<<<<<<<<<<<<<<<<<<",
            "A1234<<<<8UTO7408122F1204159<<<<<<<4"  // Checksum 8 for "A1234<<<<" 
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        assertTrue(data.isValid)
        assertEquals("A1234", data.documentNumber)
    }
    
    @Test
    fun `composite checksum with optional data`() {
        val lines = listOf(
            "I<UTOANDERSON<<CHRIS<<<<<<<<<<<<<<<<",
            "D231458907UTO9005156M2803157OPT12348"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        assertTrue(data.isValid)
        assertEquals("OPT1234", data.personalNumber)
    }
}
