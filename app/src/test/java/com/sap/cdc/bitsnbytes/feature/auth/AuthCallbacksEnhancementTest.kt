package com.sap.cdc.bitsnbytes.feature.auth

import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthSuccess
import com.sap.cdc.android.sdk.feature.RegistrationContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify that the AuthCallbacks enhancement plan pattern works correctly.
 * This test ensures that the auto-bridging async execution works without throwing
 * the "Cannot execute synchronously when override transformers are present" error.
 */
class AuthCallbacksEnhancementTest {

    @Test
    fun `test enhancement plan pattern works without IllegalStateException`() {
        // This test verifies that the pattern described in the enhancement plan now works
        var callbackExecuted = false
        var transformedData: String? = null
        
        val callbacks = AuthCallbacks()
        
        // Apply the enhancement plan pattern
        callbacks.apply {
            // Step 1: Add override transformer (this used to cause IllegalStateException)
            doOnPendingRegistrationAndOverride { registrationContext ->
                // Transform the context (simulate parsing missing fields)
                registrationContext.copy(
                    missingRequiredFields = listOf("firstName", "lastName")
                )
            }
            
            // Step 2: Add regular callback
            onPendingRegistration = { context ->
                callbackExecuted = true
                transformedData = context.missingRequiredFields?.joinToString(",")
            }
        }
        
        // Step 3: Simulate the SDK calling the callback (this used to throw exception)
        val testContext = RegistrationContext(
            regToken = "test-token",
            missingRequiredFields = null // Will be transformed by override
        )
        
        // This should NOT throw IllegalStateException anymore
        assertNotNull("Callback should be available", callbacks.onPendingRegistration)
        
        // Execute the callback - this should work seamlessly
        callbacks.onPendingRegistration?.invoke(testContext)
        
        // Verify the callback was executed and data was transformed
        assertTrue("Callback should have been executed", callbackExecuted)
        assertEquals("firstName,lastName", transformedData)
    }
    
    @Test
    fun `test backward compatibility - synchronous callbacks still work`() {
        var callbackExecuted = false
        
        val callbacks = AuthCallbacks()
        
        // Traditional synchronous pattern (should continue to work)
        callbacks.onPendingRegistration = { context ->
            callbackExecuted = true
        }
        
        val testContext = RegistrationContext(regToken = "test-token")
        
        // Should work without any issues
        callbacks.onPendingRegistration?.invoke(testContext)
        
        assertTrue("Synchronous callback should work", callbackExecuted)
    }
    
    @Test
    fun `test mixed pattern - override and regular callbacks work together`() {
        var regularCallbackExecuted = false
        var sideEffectCallbackExecuted = false
        var transformedValue: String? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Override transformer
            doOnSuccessAndOverride { authSuccess ->
                authSuccess.copy(
                    userData = authSuccess.userData + ("transformed" to "true")
                )
            }
            
            // Side effect callback
            doOnSuccess { authSuccess ->
                sideEffectCallbackExecuted = true
            }
            
            // Regular callback
            onSuccess = { authSuccess ->
                regularCallbackExecuted = true
                transformedValue = authSuccess.userData["transformed"] as? String
            }
        }
        
        val testSuccess = AuthSuccess(
            jsonData = "{}",
            userData = mapOf("original" to "data")
        )
        
        // Execute - should handle async transformation automatically
        callbacks.onSuccess?.invoke(testSuccess)
        
        assertTrue("Regular callback should execute", regularCallbackExecuted)
        assertTrue("Side effect callback should execute", sideEffectCallbackExecuted)
        assertEquals("true", transformedValue)
    }
}
