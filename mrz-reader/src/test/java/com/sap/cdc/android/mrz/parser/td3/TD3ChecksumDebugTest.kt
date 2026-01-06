package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Debug test to verify checksums in TD3 test data.
 *
 * TD3 format has 5 checksums:
 * 1. Document number (line 2, position 9)
 * 2. Date of birth (line 2, position 19)
 * 3. Expiration date (line 2, position 27)
 * 4. Personal number (line 2, position 42) - optional
 * 5. Composite (line 2, position 43)
 *
 * This test helps verify that test data has correct checksums
 * before using it in actual parser tests.
 */
class TD3ChecksumDebugTest {

    @Test
    fun `verify ICAO example checksums for TD3`() {
        println("=== TD3 ICAO Example Checksums ===\n")

        // Example TD3 MRZ
        val line1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        val line2 = "L898902C36UTO7408122F1204159ZE184226B<<<<<10"

        println("Full TD3 MRZ:")
        println("Line 1: '$line1' (length: ${line1.length})")
        println("Line 2: '$line2' (length: ${line2.length})")

        println("\n=== Checksum Calculations ===\n")

        // 1. Document number checksum (positions 0-8, checksum at 9)
        val docNum = line2.substring(0, 9)  // "L898902C3"
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

        // 4. Personal number checksum (positions 28-41, checksum at 42)
        val personalNum = line2.substring(28, 42)  // "ZE184226B<<<<<"
        val personalChecksum = ChecksumValidator.calculateChecksum(personalNum)
        println("\n4. Personal Number:")
        println("   Data: '$personalNum'")
        println("   Calculated checksum: $personalChecksum")
        println("   Expected at position 43: ${line2[42]}")
        println("   Match: ${personalChecksum.toString() == line2[42].toString()}")

        // 5. Composite checksum (positions 0-9 + 13-19 + 21-42, checksum at 43)
        val compositeData = line2.substring(0, 10) +
                           line2.substring(13, 20) +
                           line2.substring(21, 43)
        val compositeChecksum = ChecksumValidator.calculateChecksum(compositeData)
        println("\n5. Composite Checksum:")
        println("   Data: '${line2.substring(0, 10)}' + '${line2.substring(13, 20)}' + '${line2.substring(21, 43)}'")
        println("   Combined: '$compositeData'")
        println("   Calculated checksum: $compositeChecksum")
        println("   Expected at position 44: ${line2[43]}")
        println("   Match: ${compositeChecksum.toString() == line2[43].toString()}")

        println("\n=== Summary ===")
        val allMatch =
            docChecksum.toString() == line2[9].toString() &&
            dobChecksum.toString() == line2[19].toString() &&
            expiryChecksum.toString() == line2[27].toString() &&
            personalChecksum.toString() == line2[42].toString() &&
            compositeChecksum.toString() == line2[43].toString()

        if (allMatch) {
            println("✅ All checksums are VALID!")
        } else {
            println("❌ Some checksums are INVALID!")
        }
    }
}
