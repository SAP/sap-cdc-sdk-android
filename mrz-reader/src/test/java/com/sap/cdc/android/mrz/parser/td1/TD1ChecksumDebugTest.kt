package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Debug test to verify checksums in test data.
 */
class TD1ChecksumDebugTest {
    
    @Test
    fun `verify ICAO example checksums`() {
        println("=== ICAO Example Checksums ===")
        
        // Line 1: I<UTOD231458907<<<<<<<<<<<<<<<
        val docNum = "D23145890"
        val docChecksum = ChecksumValidator.calculateChecksum(docNum)
        println("Document number: $docNum")
        println("  Calculated checksum: $docChecksum")
        println("  Position 14 in line should be: $docChecksum")
        
        // Line 2: 7408122F1204159UTO<<<<<<<<<<<<
        val dob = "740812"
        val dobChecksum = ChecksumValidator.calculateChecksum(dob)
        println("\nDate of birth: $dob")
        println("  Calculated checksum: $dobChecksum")
        println("  Position 6 in line should be: $dobChecksum")
        
        val expiry = "120415"
        val expiryChecksum = ChecksumValidator.calculateChecksum(expiry)
        println("\nExpiration date: $expiry")
        println("  Calculated checksum: $expiryChecksum")
        println("  Position 14 in line should be: $expiryChecksum")
        
        println("\n=== Full MRZ Lines ===")
        val line1 = "I<UTOD231458907<<<<<<<<<<<<<<<"
        val line2 = "7408122F1204159UTO<<<<<<<<<<<<" 
        val line3 = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        
        println("Line 1: '$line1' (length: ${line1.length})")
        println("Line 2: '$line2' (length: ${line2.length})")
        println("Line 3: '$line3' (length: ${line3.length})")
    }
}
