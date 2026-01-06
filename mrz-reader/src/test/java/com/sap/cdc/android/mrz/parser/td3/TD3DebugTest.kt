package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD3Parser
import org.junit.Test

/**
 * Debug test to identify issues in TD3 parsing.
 * 
 * This test provides verbose output to help troubleshoot:
 * - Line lengths and content
 * - Parse success/failure
 * - Detailed error messages
 * 
 * Run this test when TD3 parsing fails to quickly identify the issue.
 */
class TD3DebugTest {
    
    @Test
    fun `debug TD3 parsing with print statements`() {
        val parser = TD3Parser()
        
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        println("=== TD3 Debug Test ===")
        println("\nInput lines:")
        lines.forEachIndexed { index, line ->
            println("Line ${index + 1}: '$line' (length: ${line.length})")
        }
        
        val result = parser.parse(lines)
        
        println("\nParse result type: ${result::class.simpleName}")
        
        when (result) {
            is ParseResult.Success -> {
                println("✅ SUCCESS!")
                println("\nParsed Data:")
                val data = result.data
                println("  Document Type: ${data.documentType}")
                println("  Country: ${data.countryCode}")
                println("  Surname: ${data.surname}")
                println("  Given Names: ${data.givenNames}")
                println("  Full Name: ${data.fullName}")
                println("  Document Number: ${data.documentNumber}")
                println("  Nationality: ${data.nationality}")
                println("  Date of Birth: ${data.dateOfBirth}")
                println("  Sex: ${data.sex}")
                println("  Expiration Date: ${data.expirationDate}")
                println("  Personal Number: ${data.personalNumber ?: "(none)"}")
                println("  Is Valid: ${data.isValid}")
            }
            is ParseResult.Failure -> {
                println("❌ FAILURE!")
                println("\nNumber of errors: ${result.errors.size}")
                result.errors.forEachIndexed { index, error ->
                    println("  ${index + 1}. ${error.toMessage()}")
                }
            }
        }
    }
}
