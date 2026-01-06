package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for TD3 checksum calculations.
 * 
 * TD3 has 5 checksums:
 * 1. Document number (line 2, position 9)
 * 2. Date of birth (line 2, position 19)
 * 3. Expiration date (line 2, position 27)
 * 4. Personal number (line 2, position 42) - optional
 * 5. Composite (line 2, position 43)
 */
class TD3ChecksumTest {
    
    @Test
    fun `calculate document number checksum`() {
        val docNum = "L898902C3"
        val checksum = ChecksumValidator.calculateChecksum(docNum)
        assertEquals(6, checksum)
    }
    
    @Test
    fun `calculate date of birth checksum`() {
        val dob = "740812"
        val checksum = ChecksumValidator.calculateChecksum(dob)
        assertEquals(2, checksum)
    }
    
    @Test
    fun `calculate expiration date checksum`() {
        val expiry = "120415"
        val checksum = ChecksumValidator.calculateChecksum(expiry)
        assertEquals(9, checksum)
    }
    
    @Test
    fun `calculate personal number checksum`() {
        val personalNum = "ZE184226B<<<<<"
        val checksum = ChecksumValidator.calculateChecksum(personalNum)
        assertEquals(1, checksum)
    }
    
    @Test
    fun `calculate composite checksum`() {
        // Composite = DocNum+Checksum + DOB+Checksum + Expiry+Checksum + PersonalNum+Checksum
        val composite = "L898902C36" + "7408122" + "1204159" + "ZE184226B<<<<<1"
        val checksum = ChecksumValidator.calculateChecksum(composite)
        assertEquals(0, checksum)
    }
    
    @Test
    fun `validate correct document number checksum`() {
        val docNum = "L898902C3"
        val validation = ChecksumValidator.validateChecksum(docNum, 6)
        assertEquals(true, validation.isValid)
        assertEquals(6, validation.calculated)
    }
    
    @Test
    fun `validate incorrect document number checksum`() {
        val docNum = "L898902C3"
        val validation = ChecksumValidator.validateChecksum(docNum, 5)
        assertEquals(false, validation.isValid)
        assertEquals(6, validation.calculated)
    }
    
    @Test
    fun `validate correct date of birth checksum`() {
        val dob = "740812"
        val validation = ChecksumValidator.validateChecksum(dob, 2)
        assertEquals(true, validation.isValid)
    }
    
    @Test
    fun `validate incorrect expiration date checksum`() {
        val expiry = "120415"
        val validation = ChecksumValidator.validateChecksum(expiry, 8)
        assertEquals(false, validation.isValid)
        assertEquals(9, validation.calculated)
    }
    
    @Test
    fun `calculate checksum with all fillers`() {
        val allFillers = "<<<<<<<<<<<<<<"
        val checksum = ChecksumValidator.calculateChecksum(allFillers)
        assertEquals(0, checksum)
    }
    
    @Test
    fun `calculate checksum with mixed data`() {
        val mixed = "A1234<<<<" // Document number with fillers
        val checksum = ChecksumValidator.calculateChecksum(mixed)
        assertEquals(8, checksum)
    }
}
