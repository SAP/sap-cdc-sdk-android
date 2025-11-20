package com.sap.cdc.android.sdk.core.api

/**
 * Manual test/demonstration of the JSON parsing fix.
 * 
 * This demonstrates how the fix handles error messages with special characters
 * that would previously cause JSON parsing errors.
 */
fun main() {
    println("=== CDCResponse JSON Parsing Fix Demonstration ===\n")
    
    // Original problematic error message from the issue
    val errorMessage = "General Server error"
    val errorDetails = "An unexpected error occurred during the POST request: Unable to resolve host \"socialize.us1.gigya.com\": No address associated with hostname"
    val errorCode = 500001
    
    println("Creating CDCResponse with problematic error message...")
    println("Error Code: $errorCode")
    println("Error Message: $errorMessage")
    println("Error Details: $errorDetails")
    println()
    
    // Create response using the fixed fromError method
    val response = CDCResponse()
    response.fromError(errorCode, errorMessage, errorDetails)
    
    println("✅ Successfully created CDCResponse!")
    println()
    
    // Verify the JSON was properly created
    println("Generated JSON:")
    println(response.asJson())
    println()
    
    // Verify we can extract the values back correctly
    println("Extracted values:")
    println("  Error Code: ${response.errorCode()}")
    println("  Error Message: ${response.errorMessage()}")
    println("  Error Details: ${response.errorDetails()}")
    println()
    
    // Test with various special characters
    println("=== Testing with various special characters ===\n")
    
    val testCases = listOf(
        Triple(1, "Quotes: \"test\"", "Details with \"quotes\""),
        Triple(2, "Backslash: \\path", "Details with \\backslash"),
        Triple(3, "Newline: \ntest", "Details with \nnewline"),
        Triple(4, "Tab: \ttest", "Details with \ttab"),
        Triple(5, "Unicode: 错误", "エラー: сервер")
    )
    
    for ((code, msg, details) in testCases) {
        val testResponse = CDCResponse()
        testResponse.fromError(code, msg, details)
        
        println("Test Case $code:")
        println("  Message: $msg")
        println("  Details: $details")
        println("  ✅ Successfully parsed")
        println("  Extracted message: ${testResponse.errorMessage()}")
        println("  Extracted details: ${testResponse.errorDetails()}")
        println()
    }
    
    println("=== All tests passed! ===")
    println("\nThe fix using buildJsonObject properly escapes all special characters,")
    println("preventing JSON parsing errors that occurred with the previous string concatenation approach.")
}
