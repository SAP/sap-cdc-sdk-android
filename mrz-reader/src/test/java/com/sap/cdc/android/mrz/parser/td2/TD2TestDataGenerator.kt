package com.sap.cdc.android.mrz.parser.td2

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Test data generator for TD2 format MRZ strings.
 * 
 * Generates valid TD2 test data with correct ICAO 9303 checksums.
 * TD2 format: 2 lines × 36 characters
 * 
 * This generator ensures all test data uses valid checksums and proper formatting.
 */
class TD2TestDataGenerator {
    
    @Test
    fun generateValidTD2TestData() {
        println("=== Valid TD2 Test Data ===\n")
        
        val testCases = listOf(
            // Standard ID card
            TestData(
                docType = "I",
                country = "UTO",
                surname = "ERIKSSON",
                givenNames = "ANNA<MARIA",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                optional = ""
            ),
            // Male gender
            TestData(
                docType = "I",
                country = "UTO",
                surname = "SMITH",
                givenNames = "JOHN",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "740812",
                sex = "M",
                expiry = "120415",
                optional = ""
            ),
            // Unspecified gender
            TestData(
                docType = "I",
                country = "UTO",
                surname = "DOE",
                givenNames = "",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "740812",
                sex = "<",
                expiry = "120415",
                optional = ""
            ),
            // Name with spaces
            TestData(
                docType = "I",
                country = "UTO",
                surname = "VON<DER<BERG",
                givenNames = "MARIA<ANNA",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                optional = ""
            ),
            // Document number with fillers
            TestData(
                docType = "I",
                country = "UTO",
                surname = "SMITH",
                givenNames = "JANE",
                docNum = "A1234<<<<",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                optional = ""
            ),
            // Different country
            TestData(
                docType = "I",
                country = "USA",
                surname = "JOHNSON",
                givenNames = "MARY",
                docNum = "D23145890",
                nationality = "USA",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                optional = ""
            ),
            // 2000s century boundary
            TestData(
                docType = "I",
                country = "UTO",
                surname = "YOUNG",
                givenNames = "ALICE",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "250630",
                sex = "F",
                expiry = "301231",
                optional = ""
            ),
            // 1900s century boundary
            TestData(
                docType = "I",
                country = "UTO",
                surname = "ELDER",
                givenNames = "BOB",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "850101",
                sex = "M",
                expiry = "251231",
                optional = ""
            ),
            // With optional data
            TestData(
                docType = "I",
                country = "UTO",
                surname = "ANDERSON",
                givenNames = "CHRIS",
                docNum = "D23145890",
                nationality = "UTO",
                dob = "900515",
                sex = "M",
                expiry = "280315",
                optional = "OPT1234"
            )
        )
        
        testCases.forEach { data ->
            val (line1, line2) = generateTD2(data)
            
            println("val lines = listOf(")
            println("    \"$line1\",")
            println("    \"$line2\"")
            println(")")
            println("// Line 1 length: ${line1.length}, Line 2 length: ${line2.length}")
            println()
        }
    }
    
    private fun generateTD2(data: TestData): Pair<String, String> {
        // Line 1: DocType + Country + Name (36 chars)
        val namePart = "${data.surname}<<${data.givenNames}"
        val line1 = "${data.docType}<${data.country}${namePart}".padEnd(36, '<')
        
        // Line 2: DocNum + Checksum + Nationality + DOB + Checksum + Sex + Expiry + Checksum + Optional + Composite
        val docNumPadded = data.docNum.padEnd(9, '<')
        val docChecksum = ChecksumValidator.calculateChecksum(docNumPadded)
        
        val dobChecksum = ChecksumValidator.calculateChecksum(data.dob)
        val expiryChecksum = ChecksumValidator.calculateChecksum(data.expiry)
        
        val optionalPadded = data.optional.padEnd(7, '<')
        
        // Composite checksum: DocNum+Checksum + DOB+Checksum + Expiry+Checksum + Optional
        val compositeData = docNumPadded + docChecksum + data.dob + dobChecksum + data.expiry + expiryChecksum + optionalPadded
        val compositeChecksum = ChecksumValidator.calculateChecksum(compositeData)
        
        val line2 = "${docNumPadded}${docChecksum}${data.nationality}${data.dob}${dobChecksum}${data.sex}${data.expiry}${expiryChecksum}${optionalPadded}${compositeChecksum}"
        
        return Pair(line1, line2)
    }
    
    private data class TestData(
        val docType: String,
        val country: String,
        val surname: String,
        val givenNames: String,
        val docNum: String,
        val nationality: String,
        val dob: String,
        val sex: String,
        val expiry: String,
        val optional: String
    )
}
