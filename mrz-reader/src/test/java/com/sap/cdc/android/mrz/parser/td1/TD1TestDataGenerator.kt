package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Helper to generate valid TD1 test data with correct checksums.
 */
class TD1TestDataGenerator {
    
    @Test
    fun `generate valid TD1 test cases`() {
        println("=== Valid TD1 Test Data ===\n")
        
        // Test 1: Male gender
        generateTD1("D23145890", "740812", "M", "120415", "UTO", "SMITH<<JOHN")
        
        // Test 2: Unspecified gender
        generateTD1("D23145890", "740812", "<", "120415", "UTO", "DOE")
        
        // Test 3: Name with spaces
        generateTD1("D23145890", "740812", "F", "120415", "UTO", "VON<DER<BERG<<MARIA<ANNA")
        
        // Test 4: Only surname
        generateTD1("D23145890", "740812", "F", "120415", "UTO", "ERIKSSON")
        
        // Test 5: Document with fillers
        generateTD1("A1234<<<<", "740812", "F", "120415", "UTO", "SMITH<<JANE")
        
        // Test 6: Different country
        generateTD1("D23145890", "740812", "F", "120415", "USA", "JOHNSON<<MARY")
        
        // Test 7: 2000s date
        generateTD1("D23145890", "250630", "F", "301231", "UTO", "YOUNG<<ALICE")
        
        // Test 8: 1900s date
        generateTD1("D23145890", "850101", "M", "251231", "UTO", "ELDER<<BOB")
    }
    
    private fun generateTD1(
        docNum: String,  // 9 chars
        dob: String,     // 6 chars YYMMDD
        sex: String,     // 1 char
        expiry: String,  // 6 chars YYMMDD
        country: String, // 3 chars
        name: String     // Variable, will be padded to 30
    ) {
        // Calculate checksums
        val docChecksum = ChecksumValidator.calculateChecksum(docNum)
        val dobChecksum = ChecksumValidator.calculateChecksum(dob)
        val expiryChecksum = ChecksumValidator.calculateChecksum(expiry)
        
        // Build lines
        val docType = "I"
        val line1 = buildString {
            append(docType)
            append("<")
            append(country)
            append(docNum)
            append(docChecksum)
            // Pad to 30 with '<'
            while (length < 30) append("<")
        }
        
        val line2 = buildString {
            append(dob)
            append(dobChecksum)
            append(sex)
            append(expiry)
            append(expiryChecksum)
            append(country)
            // Pad to 30 with '<'
            while (length < 30) append("<")
        }
        
        val line3 = buildString {
            append(name)
            // Pad to 30 with '<'
            while (length < 30) append("<")
        }
        
        println("val lines = listOf(")
        println("    \"$line1\",")
        println("    \"$line2\",")
        println("    \"$line3\"")
        println(")")
        println("// Line 1 length: ${line1.length}, Line 2 length: ${line2.length}, Line 3 length: ${line3.length}")
        println()
    }
}
