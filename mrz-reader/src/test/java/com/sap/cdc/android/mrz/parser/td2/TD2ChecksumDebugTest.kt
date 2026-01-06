package com.sap.cdc.android.mrz.parser.td2

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Debug test to verify checksums in TD2 test data.
 * 
 * TD2 format has 4 checksums:
 * 1. Document number checksum (line 2, position 10)
 * 2. Date of birth checksum (line 2, position 20)
 * 3. Expiration date checksum (line 2, position 28)
 * 4. Composite checksum (line 2, position 36)
 * 
 * This test helps verify that test data has correct checksums
 * before using it in actual parser tests.
 */
class TD2ChecksumDebugTest {
    
    @Test
    fun `verify ICAO example checksums for TD2`() {
        println("=== TD2 ICAO Example Checksums ===\n")
        
        // Example TD2 MRZ
        val line1 = "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<"
        val line2 = "D231458907UTO7408122F1204159<<<<<<<6"
        
        println("Full TD2 MRZ:")
        println("Line 1: '$line1' (length: ${line1.length})")
        println("Line 2: '$line2' (length: ${line2.length})")
        
        println("\n=== Checksum Calculations ===\n")
        
        // 1. Document number checksum (positions 0-8, checksum at 9)
        val docNum = line2.substring(0, 9)  // "D23145890"
        val docChecksum = ChecksumValidator.calculateChecksum(docNum)
        println("1. Document Number:")
        println("   Data: '$docNum'")
        println("   Calculated checksum: $docChecksum")
        println("   Expected at position 10: ${line2[9]}")
        println("   Match: ${docChecksum.toString() == line2[9].toString()}")
        
        // 2. Date of birth checksum (positions 13-18, checksum at 19)
        val dob = line2.substring(13, 19)  // "740812"
        val dobChecksum = ChecksumValidator.calculateChecksum(dob)
        println("\n2. Date of Birth:")
        println("   Data: '$dob'")
        println("   Calculated checksum: $dobChecksum")
        println("   Expected at position 20: ${line2[19]}")
        println("   Match: ${dobChecksum.toString() == line2[19].toString()}")
        
        // 3. Expiration date checksum (positions 21-26, checksum at 27)
        val expiry = line2.substring(21, 27)  // "120415"
        val expiryChecksum = ChecksumValidator.calculateChecksum(expiry)
        println("\n3. Expiration Date:")
        println("   Data: '$expiry'")
        println("   Calculated checksum: $expiryChecksum")
        println("   Expected at position 28: ${line2[27]}")
        println("   Match: ${expiryChecksum.toString() == line2[27].toString()}")
        
        // 4. Composite checksum (positions 0-9 + 13-19 + 21-34, checksum at 35)
        val compositeData = line2.substring(0, 10) + 
                           line2.substring(13, 20) + 
                           line2.substring(21, 35)
        val compositeChecksum = ChecksumValidator.calculateChecksum(compositeData)
        println("\n4. Composite Checksum:")
        println("   Data: '${line2.substring(0, 10)}' + '${line2.substring(13, 20)}' + '${line2.substring(21, 35)}'")
        println("   Combined: '$compositeData'")
        println("   Calculated checksum: $compositeChecksum")
        println("   Expected at position 36: ${line2[35]}")
        println("   Match: ${compositeChecksum.toString() == line2[35].toString()}")
        
        println("\n=== Summary ===")
        val allMatch = 
            docChecksum.toString() == line2[9].toString() &&
            dobChecksum.toString() == line2[19].toString() &&
            expiryChecksum.toString() == line2[27].toString() &&
            compositeChecksum.toString() == line2[35].toString()
        
        if (allMatch) {
            println("✅ All checksums are VALID!")
        } else {
            println("❌ Some checksums are INVALID!")
        }
    }
}
