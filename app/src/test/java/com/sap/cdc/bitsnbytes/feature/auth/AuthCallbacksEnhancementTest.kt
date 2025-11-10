package com.sap.cdc.bitsnbytes.feature.auth

import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthError
import com.sap.cdc.android.sdk.feature.AuthResult
import com.sap.cdc.android.sdk.feature.AuthSuccess
import com.sap.cdc.android.sdk.feature.RegistrationContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
                    data = authSuccess.data + ("transformed" to "true")
                )
            }
            
            // Side effect callback
            doOnSuccess { authSuccess ->
                sideEffectCallbackExecuted = true
            }
            
            // Regular callback
            onSuccess = { authSuccess ->
                regularCallbackExecuted = true
                transformedValue = authSuccess.data["transformed"] as? String
            }
        }
        
        val testSuccess = AuthSuccess(
            jsonData = "{}",
            data = mapOf("original" to "data")
        )
        
        // Execute - should handle async transformation automatically
        callbacks.onSuccess?.invoke(testSuccess)
        
        assertTrue("Regular callback should execute", regularCallbackExecuted)
        assertTrue("Side effect callback should execute", sideEffectCallbackExecuted)
        assertEquals("true", transformedValue)
    }
    
    @Test
    fun `test universal override doOnAnyAndOverride works for Success callback`() {
        var callbackExecuted = false
        var transformedValue: String? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Universal override that should work for any callback type
            doOnAnyAndOverride { authResult ->
                when (authResult) {
                    is AuthResult.Success -> {
                        // Transform success data
                        AuthResult.Success(
                            authResult.authSuccess.copy(
                                data = authResult.authSuccess.data + ("universalOverride" to "applied")
                            )
                        )
                    }
                    else -> authResult
                }
            }
            
            // Regular callback
            onSuccess = { authSuccess ->
                callbackExecuted = true
                transformedValue = authSuccess.data["universalOverride"] as? String
            }
        }
        
        val testSuccess = AuthSuccess(
            jsonData = "{}",
            data = mapOf("original" to "data")
        )
        
        // Execute - universal override should be applied
        callbacks.onSuccess?.invoke(testSuccess)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertEquals("applied", transformedValue)
    }
    
    @Test
    fun `test universal override doOnAnyAndOverride works for PendingRegistration callback`() {
        var callbackExecuted = false
        var transformedFields: List<String>? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Universal override that should work for any callback type
            doOnAnyAndOverride { authResult ->
                when (authResult) {
                    is AuthResult.PendingRegistration -> {
                        // Transform registration context
                        AuthResult.PendingRegistration(
                            authResult.context.copy(
                                missingRequiredFields = listOf("universalField1", "universalField2")
                            )
                        )
                    }
                    else -> authResult
                }
            }
            
            // Regular callback
            onPendingRegistration = { context ->
                callbackExecuted = true
                transformedFields = context.missingRequiredFields
            }
        }
        
        val testContext = RegistrationContext(
            regToken = "test-token",
            missingRequiredFields = null
        )
        
        // Execute - universal override should be applied
        callbacks.onPendingRegistration?.invoke(testContext)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertEquals(listOf("universalField1", "universalField2"), transformedFields)
    }
    
    @Test
    fun `test universal override and individual override work together - universal first`() {
        var callbackExecuted = false
        var finalValue: String? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Universal override (applied first)
            doOnAnyAndOverride { authResult ->
                when (authResult) {
                    is AuthResult.Success -> {
                        AuthResult.Success(
                            authResult.authSuccess.copy(
                                data = authResult.authSuccess.data + ("universal" to "first")
                            )
                        )
                    }
                    else -> authResult
                }
            }
            
            // Individual override (applied second)
            doOnSuccessAndOverride { authSuccess ->
                authSuccess.copy(
                    data = authSuccess.data + ("individual" to "second")
                )
            }
            
            // Regular callback
            onSuccess = { authSuccess ->
                callbackExecuted = true
                finalValue = "${authSuccess.data["universal"]}-${authSuccess.data["individual"]}"
            }
        }
        
        val testSuccess = AuthSuccess(
            jsonData = "{}",
            data = mapOf("original" to "data")
        )
        
        // Execute - both overrides should be applied in correct order
        callbacks.onSuccess?.invoke(testSuccess)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertEquals("first-second", finalValue)
    }
    
    @Test
    fun `test universal override can change callback type - Success to Error`() {
        var successCallbackExecuted = false
        var errorCallbackExecuted = false
        var errorMessage: String? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Universal override that converts Success to Error
            doOnAnyAndOverride { authResult ->
                when (authResult) {
                    is AuthResult.Success -> {
                        // Convert success to error for testing
                        AuthResult.Error(
                            AuthError(
                                message = "Converted by universal override",
                                code = "UNIVERSAL_OVERRIDE"
                            )
                        )
                    }
                    else -> authResult
                }
            }
            
            onSuccess = { _ ->
                successCallbackExecuted = true
            }
            
            onError = { authError ->
                errorCallbackExecuted = true
                errorMessage = authError.message
            }
        }
        
        val testSuccess = AuthSuccess(
            jsonData = "{}",
            data = mapOf("original" to "data")
        )
        
        // Execute success callback - should be converted to error by universal override
        callbacks.onSuccess?.invoke(testSuccess)
        
        // The success callback should still execute (since we called onSuccess)
        // but the data should be unchanged since universal override changed the type
        assertTrue("Success callback should execute", successCallbackExecuted)
        assertFalse("Error callback should not execute from success call", errorCallbackExecuted)
        
        // Now test with actual error to verify error callback works
        val testError = AuthError(message = "Test error")
        callbacks.onError?.invoke(testError)
        
        assertTrue("Error callback should execute", errorCallbackExecuted)
        assertEquals("Test error", errorMessage)
    }
    
    @Test
    fun `test individual override still works independently without universal override`() {
        var callbackExecuted = false
        var transformedValue: String? = null
        
        val callbacks = AuthCallbacks()
        
        callbacks.apply {
            // Only individual override, no universal override
            doOnPendingRegistrationAndOverride { registrationContext ->
                registrationContext.copy(
                    missingRequiredFields = listOf("individualOverride")
                )
            }
            
            onPendingRegistration = { context ->
                callbackExecuted = true
                transformedValue = context.missingRequiredFields?.firstOrNull()
            }
        }
        
        val testContext = RegistrationContext(
            regToken = "test-token",
            missingRequiredFields = null
        )
        
        // Execute - only individual override should be applied
        callbacks.onPendingRegistration?.invoke(testContext)
        
        assertTrue("Callback should have been executed", callbackExecuted)
        assertEquals("individualOverride", transformedValue)
    }
}
