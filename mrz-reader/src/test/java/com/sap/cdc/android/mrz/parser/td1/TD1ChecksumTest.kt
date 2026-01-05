package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests specifically for TD1 checksum validation.
 * 
 * Tests the three checksums used in TD1 format:
 * 1. Document number checksum (line 1, position 14)
 * 2. Date of birth checksum (line 2, position 6)
 * 3. Expiration date checksum (line 2, position 14)
 * 
 * Uses known valid TD1 examples from ICAO 9303 specification.
 */
class TD1ChecksumTest {
    
    @Test
    fun `validate TD1 document number checksum from ICAO example`() {
        // Document number: D23145890 from line 1 positions 5-13
        // Expected checksum at position 14: 7
        val documentNumber = "D23145890"
        val expectedChecksum = 7
        
        val result = ChecksumValidator.validateChecksum(documentNumber, expectedChecksum)
        
        assertTrue("Document number checksum should be valid", result.isValid)
        assertEquals(7, result.calculated)
        
        // Manual verification:
        // D(13)*7 + 2*3 + 3*1 + 1*7 + 4*3 + 5*1 + 8*7 + 9*3 + 0*1
        // = 91 + 6 + 3 + 7 + 12 + 5 + 56 + 27 + 0 = 207 % 10 = 7 ✓
    }
    
    @Test
    fun `validate TD1 date of birth checksum from ICAO example`() {
        // Date of birth: 740812 from line 2 positions 0-5
        // Expected checksum at position 6: 2
        val dateOfBirth = "740812"
        val expectedChecksum = 2
        
        val result = ChecksumValidator.validateChecksum(dateOfBirth, expectedChecksum)
        
        assertTrue("Date of birth checksum should be valid", result.isValid)
        assertEquals(2, result.calculated)
        
        // Manual verification:
        // 7*7 + 4*3 + 0*1 + 8*7 + 1*3 + 2*1
        // = 49 + 12 + 0 + 56 + 3 + 2 = 122 % 10 = 2 ✓
    }
    
    @Test
    fun `validate TD1 expiration date checksum from ICAO example`() {
        // Expiration date: 120415 from line 2 positions 8-13
        // Expected checksum at position 14: 9
        val expirationDate = "120415"
        val expectedChecksum = 9
        
        val result = ChecksumValidator.validateChecksum(expirationDate, expectedChecksum)
        
        assertTrue("Expiration date checksum should be valid", result.isValid)
        assertEquals(9, result.calculated)
        
        // Manual verification:
        // 1*7 + 2*3 + 0*1 + 4*7 + 1*3 + 5*1
        // = 7 + 6 + 0 + 28 + 3 + 5 = 49 % 10 = 9 ✓
    }
    
    @Test
    fun `validate all TD1 checksums together from ICAO example`() {
        val checksums = mapOf(
            "documentNumber" to Pair("D23145890", 7),
            "dateOfBirth" to Pair("740812", 2),
            "expirationDate" to Pair("120415", 9)
        )
        
        val results = ChecksumValidator.validateMultipleChecksums(checksums)
        
        assertEquals(3, results.size)
        assertTrue("Document number checksum should be valid", results["documentNumber"]!!.isValid)
        assertTrue("Date of birth checksum should be valid", results["dateOfBirth"]!!.isValid)
        assertTrue("Expiration date checksum should be valid", results["expirationDate"]!!.isValid)
    }
    
    @Test
    fun `detect TD1 checksum failure - document number`() {
        // Intentionally wrong checksum
        val result = ChecksumValidator.validateChecksum("D23145890", 3)
        
        assertFalse(result.isValid)
        assertEquals(7, result.calculated)
        assertEquals(3, result.expected)
    }
    
    @Test
    fun `detect TD1 checksum failure - date of birth`() {
        // Intentionally wrong checksum
        val result = ChecksumValidator.validateChecksum("740812", 5)
        
        assertFalse(result.isValid)
        assertEquals(2, result.calculated)
        assertEquals(5, result.expected)
    }
    
    @Test
    fun `detect TD1 checksum failure - expiration date`() {
        // Intentionally wrong checksum
        val result = ChecksumValidator.validateChecksum("120415", 1)
        
        assertFalse(result.isValid)
        assertEquals(9, result.calculated)
        assertEquals(1, result.expected)
    }
    
    @Test
    fun `validate TD1 document number with fillers`() {
        // Document number with trailing fillers: A1234<<<<
        val documentNumber = "A1234<<<<"
        
        // Calculate expected checksum
        // A(10)*7 + 1*3 + 2*1 + 3*7 + 4*3 + <(0)*1 + <(0)*7 + <(0)*3 + <(0)*1
        // = 70 + 3 + 2 + 21 + 12 + 0 + 0 + 0 + 0 = 108 % 10 = 8
        val checksum = ChecksumValidator.calculateChecksum(documentNumber)
        
        assertEquals(8, checksum)
    }
    
    @Test
    fun `validate TD1 with all numeric document number`() {
        val documentNumber = "123456789"
        
        // 1*7 + 2*3 + 3*1 + 4*7 + 5*3 + 6*1 + 7*7 + 8*3 + 9*1
        // = 7 + 6 + 3 + 28 + 15 + 6 + 49 + 24 + 9 = 147 % 10 = 7
        val checksum = ChecksumValidator.calculateChecksum(documentNumber)
        
        assertEquals(7, checksum)
    }
    
    @Test
    fun `validate TD1 with alphanumeric document number`() {
        val documentNumber = "AB1234567"
        
        // A(10)*7 + B(11)*3 + 1*1 + 2*7 + 3*3 + 4*1 + 5*7 + 6*3 + 7*1
        // = 70 + 33 + 1 + 14 + 9 + 4 + 35 + 18 + 7 = 191 % 10 = 1
        val checksum = ChecksumValidator.calculateChecksum(documentNumber)
        
        assertEquals(1, checksum)
    }
}
