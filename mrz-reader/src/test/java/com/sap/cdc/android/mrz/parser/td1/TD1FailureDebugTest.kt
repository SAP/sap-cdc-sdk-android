package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD1Parser
import org.junit.Test

/**
 * Debug test to see actual vs expected values for failing tests.
 */
class TD1FailureDebugTest {
    
    @Test
    fun `debug name with spaces test`() {
        val parser = TD1Parser()
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "VON<DER<BERG<<MARIA<ANNA<<<<<<"
        )
        
        println("=== Name with spaces test ===")
        val result = parser.parse(lines)
        
        when (result) {
            is ParseResult.Success -> {
                val data = result.data
                println("Surname: '${data.surname}'")
                println("Given names: '${data.givenNames}'")
                println("Full name: '${data.fullName}'")
            }
            is ParseResult.Failure -> {
                println("FAILED: ${result.errors}")
            }
        }
    }
    
    @Test
    fun `debug 2000s date test`() {
        val parser = TD1Parser()
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "2506300F3012316UTO<<<<<<<<<<<<",
            "YOUNG<<ALICE<<<<<<<<<<<<<<<<<<"
        )
        
        println("=== 2000s date test ===")
        val result = parser.parse(lines)
        
        when (result) {
            is ParseResult.Success -> {
                val data = result.data
                println("DOB: '${data.dateOfBirth}'")
                println("Expiry: '${data.expirationDate}'")
            }
            is ParseResult.Failure -> {
                println("FAILED: ${result.errors}")
            }
        }
    }
    
    @Test
    fun `debug invalid test line 1 short`() {
        val parser = TD1Parser()
        val lines = listOf(
            "I<UTOD231458907<<<<<<<",  // 23 chars
            "7408122F1204159UTO<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<"
        )
        
        println("=== Invalid line 1 short test ===")
        val result = parser.parse(lines)
        
        when (result) {
            is ParseResult.Success -> {
                println("Unexpected success!")
            }
            is ParseResult.Failure -> {
                println("Errors: ${result.errors.size}")
                result.errors.forEach { 
                    println("  - ${it.toMessage()}")
                }
            }
        }
    }
}
