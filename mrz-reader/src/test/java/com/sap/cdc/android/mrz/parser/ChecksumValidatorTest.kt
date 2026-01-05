package com.sap.cdc.android.mrz.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ChecksumValidator.
 * 
 * Tests the ICAO 9303 checksum algorithm with known valid examples
 * and edge cases.
 */
class ChecksumValidatorTest {
    
    @Test
    fun `validateChecksum with valid passport document number`() {
        // Example from ICAO 9303: L898902C3 with checksum 6
        val result = ChecksumValidator.validateChecksum("L898902C3", 6)
        
        assertTrue(result.isValid)
        assertEquals(6, result.calculated)
        assertEquals(6, result.expected)
    }
    
    @Test
    fun `validateChecksum with valid date of birth`() {
        // Date: 740812 (Aug 12, 1974) with checksum 2
        val result = ChecksumValidator.validateChecksum("740812", 2)
        
        assertTrue(result.isValid)
        assertEquals(2, result.calculated)
    }
    
    @Test
    fun `validateChecksum with valid expiration date`() {
        // Date: 120415 (April 15, 2012) with checksum 9
        val result = ChecksumValidator.validateChecksum("120415", 9)
        
        assertTrue(result.isValid)
        assertEquals(9, result.calculated)
    }
    
    @Test
    fun `validateChecksum with invalid checksum`() {
        val result = ChecksumValidator.validateChecksum("L898902C3", 5)
        
        assertFalse(result.isValid)
        assertEquals(6, result.calculated)
        assertEquals(5, result.expected)
    }
    
    @Test
    fun `calculateChecksum for document with fillers`() {
        // Document number with < fillers: "D23145890<<<<<<"
        val checksum = ChecksumValidator.calculateChecksum("D23145890")
        
        assertEquals(7, checksum)
    }
    
    @Test
    fun `calculateChecksum for all numeric string`() {
        val checksum = ChecksumValidator.calculateChecksum("123456789")
        
        // Manual calculation: 1*7 + 2*3 + 3*1 + 4*7 + 5*3 + 6*1 + 7*7 + 8*3 + 9*1
        // = 7 + 6 + 3 + 28 + 15 + 6 + 49 + 24 + 9 = 147 % 10 = 7
        assertEquals(7, checksum)
    }
    
    @Test
    fun `calculateChecksum for all letter string`() {
        val checksum = ChecksumValidator.calculateChecksum("ABC")
        
        // A=10, B=11, C=12
        // 10*7 + 11*3 + 12*1 = 70 + 33 + 12 = 115 % 10 = 5
        assertEquals(5, checksum)
    }
    
    @Test
    fun `calculateChecksum with filler characters`() {
        val checksum = ChecksumValidator.calculateChecksum("A<<B")
        
        // A=10, <=0, <=0, B=11
        // 10*7 + 0*3 + 0*1 + 11*7 = 70 + 0 + 0 + 77 = 147 % 10 = 7
        assertEquals(7, checksum)
    }
    
    @Test
    fun `calculateChecksum empty string returns zero`() {
        val checksum = ChecksumValidator.calculateChecksum("")
        
        assertEquals(0, checksum)
    }
    
    @Test
    fun `validateMultipleChecksums with all valid`() {
        val checksums = mapOf(
            "documentNumber" to Pair("L898902C3", 6),
            "dateOfBirth" to Pair("740812", 2),
            "expirationDate" to Pair("120415", 9)
        )
        
        val results = ChecksumValidator.validateMultipleChecksums(checksums)
        
        assertEquals(3, results.size)
        assertTrue(results["documentNumber"]!!.isValid)
        assertTrue(results["dateOfBirth"]!!.isValid)
        assertTrue(results["expirationDate"]!!.isValid)
    }
    
    @Test
    fun `validateMultipleChecksums with one invalid`() {
        val checksums = mapOf(
            "documentNumber" to Pair("L898902C3", 6),
            "dateOfBirth" to Pair("740812", 9)  // Wrong checksum
        )
        
        val results = ChecksumValidator.validateMultipleChecksums(checksums)
        
        assertTrue(results["documentNumber"]!!.isValid)
        assertFalse(results["dateOfBirth"]!!.isValid)
        assertEquals(2, results["dateOfBirth"]!!.calculated)
    }
    
    @Test
    fun `character value mapping for digits`() {
        // Test through checksum calculation
        val checksum0 = ChecksumValidator.calculateChecksum("0")
        val checksum9 = ChecksumValidator.calculateChecksum("9")
        
        assertEquals(0, checksum0)  // 0*7 % 10 = 0
        assertEquals(3, checksum9)  // 9*7 % 10 = 63 % 10 = 3
    }
    
    @Test
    fun `character value mapping for letters`() {
        // A should be 10, Z should be 35
        val checksumA = ChecksumValidator.calculateChecksum("A")
        val checksumZ = ChecksumValidator.calculateChecksum("Z")
        
        assertEquals(0, checksumA)  // 10*7 % 10 = 70 % 10 = 0
        assertEquals(5, checksumZ)  // 35*7 % 10 = 245 % 10 = 5
    }
}
