package com.sap.cdc.android.sdk.core.api

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CDCResponse JSON error handling.
 * 
 * Tests the fix for JSON parsing errors when error messages contain special characters
 * such as quotes, backslashes, and other JSON-breaking characters.
 */
class CDCResponseTest {

    @Test
    fun `test fromError with special characters in error message`() {
        // This is the problematic error message from the issue
        val errorMessage = "General Server error"
        val errorDetails = "An unexpected error occurred during the POST request: Unable to resolve host \"socialize.us1.gigya.com\": No address associated with hostname"
        val errorCode = 500001

        // Create response with the problematic error
        val response = CDCResponse()
        response.fromError(errorCode, errorMessage, errorDetails)

        // Verify the response was created successfully
        assertNotNull("Response should not be null", response.asJson())
        
        // Verify we can parse the JSON without errors
        assertNotNull("JSON object should be parsed", response.jsonObject)
        
        // Verify the error code is correct
        assertEquals("Error code should match", errorCode, response.errorCode())
        
        // Verify the error message is correct
        assertEquals("Error message should match", errorMessage, response.errorMessage())
        
        // Verify the error details are correct (with escaped quotes properly handled)
        assertEquals("Error details should match", errorDetails, response.errorDetails())
        
        // Verify isError returns true
        assertTrue("Response should be an error", response.isError())
    }

    @Test
    fun `test fromError with various special characters`() {
        val testCases = listOf(
            Triple(500001, "Error with \"quotes\"", "Details with \"quotes\""),
            Triple(500002, "Error with \\backslash", "Details with \\backslash"),
            Triple(500003, "Error with \nnewline", "Details with \nnewline"),
            Triple(500004, "Error with \ttab", "Details with \ttab"),
            Triple(500005, "Error with \rcarriage return", "Details with \rcarriage return"),
            Triple(500006, "Complex: \"quotes\" and \\backslash and \nnewlines", "All mixed: \"test\" \\path \n\t\r")
        )

        for ((code, message, details) in testCases) {
            val response = CDCResponse()
            response.fromError(code, message, details)

            assertNotNull("Response should not be null for code $code", response.asJson())
            assertNotNull("JSON object should be parsed for code $code", response.jsonObject)
            assertEquals("Error code should match for code $code", code, response.errorCode())
            assertEquals("Error message should match for code $code", message, response.errorMessage())
            assertEquals("Error details should match for code $code", details, response.errorDetails())
            assertTrue("Response should be an error for code $code", response.isError())
        }
    }

    @Test
    fun `test fromError with empty strings`() {
        val response = CDCResponse()
        response.fromError(0, "", "")

        assertNotNull("Response should not be null", response.asJson())
        assertNotNull("JSON object should be parsed", response.jsonObject)
        assertEquals("Error code should be 0", 0, response.errorCode())
        assertEquals("Error message should be empty", "", response.errorMessage())
        assertEquals("Error details should be empty", "", response.errorDetails())
        assertFalse("Response should not be an error (code 0)", response.isError())
    }

    @Test
    fun `test fromError preserves unicode characters`() {
        val errorMessage = "错误消息"
        val errorDetails = "エラーの詳細: Unable to connect to сервер"
        val errorCode = 500001

        val response = CDCResponse()
        response.fromError(errorCode, errorMessage, errorDetails)

        assertNotNull("Response should not be null", response.asJson())
        assertEquals("Error message should preserve unicode", errorMessage, response.errorMessage())
        assertEquals("Error details should preserve unicode", errorDetails, response.errorDetails())
    }
}
