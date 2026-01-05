package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD1Parser
import org.junit.Test

/**
 * Debug test to identify why TD1 parsing is failing.
 */
class TD1DebugTest {
    
    @Test
    fun `debug TD1 parsing with print statements`() {
        val parser = TD1Parser()
        
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        println("Input lines:")
        lines.forEachIndexed { index, line ->
            println("Line ${index + 1}: '$line' (length: ${line.length})")
        }
        
        val result = parser.parse(lines)
        
        println("\nParse result type: ${result::class.simpleName}")
        
        when (result) {
            is ParseResult.Success -> {
                println("SUCCESS!")
                println("Data: ${result.data}")
            }
            is ParseResult.Failure -> {
                println("FAILURE!")
                println("Number of errors: ${result.errors.size}")
                result.errors.forEach { error ->
                    println("  - ${error.toMessage()}")
                }
            }
        }
    }
}
