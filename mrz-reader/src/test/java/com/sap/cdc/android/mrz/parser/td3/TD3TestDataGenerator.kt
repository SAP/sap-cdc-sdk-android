package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.parser.ChecksumValidator
import org.junit.Test

/**
 * Test data generator for TD3 format MRZ strings.
 * 
 * Generates valid TD3 test data with correct ICAO 9303 checksums.
 * TD3 format: 2 lines × 44 characters (standard passports)
 * 
 * This generator ensures all test data uses valid checksums and proper formatting.
 */
class TD3TestDataGenerator {
    
    @Test
    fun generateValidTD3TestData() {
        println("=== Valid TD3 Test Data ===\n")
        
        val testCases = listOf(
            // Standard passport
            TestData(
                docType = "P",
                country = "UTO",
                surname = "ERIKSSON",
                givenNames = "ANNA<MARIA",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                personalNum = "ZE184226B"
            ),
            // Male gender
            TestData(
                docType = "P",
                country = "UTO",
                surname = "SMITH",
                givenNames = "JOHN",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "740812",
                sex = "M",
                expiry = "120415",
                personalNum = "ZE184226B"
            ),
            // Unspecified gender
            TestData(
                docType = "P",
                country = "UTO",
                surname = "DOE",
                givenNames = "",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "740812",
                sex = "<",
                expiry = "120415",
                personalNum = ""
            ),
            // Name with spaces
            TestData(
                docType = "P",
                country = "UTO",
                surname = "VON<DER<BERG",
                givenNames = "MARIA<ANNA",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                personalNum = "ZE184226B"
            ),
            // Document number with fillers
            TestData(
                docType = "P",
                country = "UTO",
                surname = "SMITH",
                givenNames = "JANE",
                docNum = "A1234<<<<",
                nationality = "UTO",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                personalNum = ""
            ),
            // Different country
            TestData(
                docType = "P",
                country = "USA",
                surname = "JOHNSON",
                givenNames = "MARY",
                docNum = "L898902C3",
                nationality = "USA",
                dob = "740812",
                sex = "F",
                expiry = "120415",
                personalNum = "ZE184226B"
            ),
            // 2000s century boundary
            TestData(
                docType = "P",
                country = "UTO",
                surname = "YOUNG",
                givenNames = "ALICE",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "250630",
                sex = "F",
                expiry = "301231",
                personalNum = "ZE184226B"
            ),
            // 1900s century boundary
            TestData(
                docType = "P",
                country = "UTO",
                surname = "ELDER",
                givenNames = "BOB",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "850101",
                sex = "M",
                expiry = "251231",
                personalNum = "ZE184226B"
            ),
            // No personal number
            TestData(
                docType = "P",
                country = "UTO",
                surname = "ANDERSON",
                givenNames = "CHRIS",
                docNum = "L898902C3",
                nationality = "UTO",
                dob = "900515",
                sex = "M",
                expiry = "280315",
                personalNum = ""
            )
        )
        
        testCases.forEach { data ->
            val (line1, line2) = generateTD3(data)
            
            println("val lines = listOf(")
            println("    \"$line1\",")
            println("    \"$line2\"")
            println(")")
            println("// Line 1 length: ${line1.length}, Line 2 length: ${line2.length}")
            println()
        }
    }
    
    private fun generateTD3(data: TestData): Pair<String, String> {
        // Line 1: DocType + Country + Name (44 chars)
        val namePart = "${data.surname}<<${data.givenNames}"
        val line1 = "${data.docType}<${data.country}${namePart}".padEnd(44, '<')
        
        // Line 2: DocNum + Checksum + Nationality + DOB + Checksum + Sex + Expiry + Checksum + PersonalNum + PersonalChecksum + Composite
        val docNumPadded = data.docNum.padEnd(9, '<')
        val docChecksum = ChecksumValidator.calculateChecksum(docNumPadded)
        
        val dobChecksum = ChecksumValidator.calculateChecksum(data.dob)
        val expiryChecksum = ChecksumValidator.calculateChecksum(data.expiry)
        
        val personalPadded = data.personalNum.padEnd(14, '<')
        val personalChecksum = ChecksumValidator.calculateChecksum(personalPadded)
        
        // Composite checksum: DocNum+Checksum + DOB+Checksum + Expiry+Checksum + PersonalNum+PersonalChecksum
        val compositeData = docNumPadded + docChecksum + data.dob + dobChecksum + data.expiry + expiryChecksum + personalPadded + personalChecksum
        val compositeChecksum = ChecksumValidator.calculateChecksum(compositeData)
        
        val line2 = "${docNumPadded}${docChecksum}${data.nationality}${data.dob}${dobChecksum}${data.sex}${data.expiry}${expiryChecksum}${personalPadded}${personalChecksum}${compositeChecksum}"
        
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
        val personalNum: String
    )
}
