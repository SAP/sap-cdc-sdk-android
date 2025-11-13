package com.sap.cdc.android.sdk.feature.session

import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.TestResourceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionService using the SessionSecureProvider abstraction.
 * These tests demonstrate how the dependency injection makes SessionService fully testable.
 */
class SessionServiceTest {

    private lateinit var siteConfig: SiteConfig
    private lateinit var mockSessionSecureProvider: SessionSecureProvider
    private lateinit var sessionService: SessionService
    
    private val testSession = Session(
        token = "test-token-123",
        secret = "test-secret-456",
        expiration = 3600L
    )

    @Before
    fun setUp() {
        // Create mock dependencies
        val mockResourceProvider = TestResourceProvider()
        siteConfig = SiteConfig(
            apiKey = "test-api-key",
            domain = "test.domain.com",
            resourceProvider = mockResourceProvider
        )
        
        // Create mock SessionSecureProvider
        mockSessionSecureProvider = mockk<SessionSecureProvider>(relaxed = true)
        
        // Create SessionService with mocked dependencies
        sessionService = SessionService(siteConfig, mockSessionSecureProvider)
    }

    @Test
    fun `availableSession should return true when session exists`() {
        // Given
        every { mockSessionSecureProvider.availableSession() } returns true
        
        // When
        val result = sessionService.availableSession()
        
        // Then
        assertTrue(result)
        verify { mockSessionSecureProvider.availableSession() }
    }

    @Test
    fun `availableSession should return false when no session exists`() {
        // Given
        every { mockSessionSecureProvider.availableSession() } returns false
        
        // When
        val result = sessionService.availableSession()
        
        // Then
        assertFalse(result)
        verify { mockSessionSecureProvider.availableSession() }
    }

    @Test
    fun `getSession should return session when available`() {
        // Given
        every { mockSessionSecureProvider.getSession() } returns testSession
        
        // When
        val result = sessionService.getSession()
        
        // Then
        assertEquals(testSession, result)
        verify { mockSessionSecureProvider.getSession() }
    }

    @Test
    fun `getSession should return null when no session available`() {
        // Given
        every { mockSessionSecureProvider.getSession() } returns null
        
        // When
        val result = sessionService.getSession()
        
        // Then
        assertNull(result)
        verify { mockSessionSecureProvider.getSession() }
    }

    @Test
    fun `setSession should delegate to SessionSecureProvider`() {
        // When
        sessionService.setSession(testSession)
        
        // Then
        verify { mockSessionSecureProvider.setSession(testSession) }
    }

    @Test
    fun `invalidateSession should delegate to SessionSecureProvider`() {
        // When
        sessionService.invalidateSession()
        
        // Then
        verify { mockSessionSecureProvider.invalidateSession() }
    }

    @Test
    fun `clearSession should delegate to SessionSecureProvider`() {
        // When
        sessionService.clearSession()
        
        // Then
        verify { mockSessionSecureProvider.clearSession() }
    }

    @Test
    fun `sessionSecureLevel should return STANDARD level`() {
        // Given
        every { mockSessionSecureProvider.sessionSecureLevel() } returns SessionSecureLevel.STANDARD
        
        // When
        val result = sessionService.sessionSecureLevel()
        
        // Then
        assertEquals(SessionSecureLevel.STANDARD, result)
        verify { mockSessionSecureProvider.sessionSecureLevel() }
    }

    @Test
    fun `sessionSecureLevel should return BIOMETRIC level`() {
        // Given
        every { mockSessionSecureProvider.sessionSecureLevel() } returns SessionSecureLevel.BIOMETRIC
        
        // When
        val result = sessionService.sessionSecureLevel()
        
        // Then
        assertEquals(SessionSecureLevel.BIOMETRIC, result)
        verify { mockSessionSecureProvider.sessionSecureLevel() }
    }

    @Test
    fun `secureBiometricSession should delegate to SessionSecureProvider`() {
        // Given
        val encryptedSession = "encrypted-session-data"
        val iv = "initialization-vector"
        
        // When
        sessionService.secureBiometricSession(encryptedSession, iv)
        
        // Then
        verify { mockSessionSecureProvider.secureBiometricSession(encryptedSession, iv) }
    }

    @Test
    fun `unlockBiometricSession should delegate to SessionSecureProvider`() {
        // Given
        val decryptedSession = "decrypted-session-data"
        
        // When
        sessionService.unlockBiometricSession(decryptedSession)
        
        // Then
        verify { mockSessionSecureProvider.unlockBiometricSession(decryptedSession) }
    }

    @Test
    fun `biometricLocked should return true when session is biometric locked`() {
        // Given
        every { mockSessionSecureProvider.biometricLocked() } returns true
        
        // When
        val result = sessionService.biometricLocked()
        
        // Then
        assertTrue(result)
        verify { mockSessionSecureProvider.biometricLocked() }
    }

    @Test
    fun `biometricLocked should return false when session is not biometric locked`() {
        // Given
        every { mockSessionSecureProvider.biometricLocked() } returns false
        
        // When
        val result = sessionService.biometricLocked()
        
        // Then
        assertFalse(result)
        verify { mockSessionSecureProvider.biometricLocked() }
    }

    @Test
    fun `reloadWithSiteConfig should update siteConfig and create new SessionSecure`() {
        // Given
        val newSiteConfig = SiteConfig(
            apiKey = "new-api-key",
            domain = "new.domain.com",
            resourceProvider = TestResourceProvider()
        )
        
        // When
        val result = sessionService.reloadWithSiteConfig(newSiteConfig)
        
        // Then
        assertEquals(sessionService, result) // Should return self for chaining
        assertEquals(newSiteConfig, sessionService.siteConfig)
        // Note: In real implementation, this would create a new SessionSecure instance
        // but our test uses a mock, so we can't verify the internal change
    }

    @Test
    fun `gmidLatest should return stored GMID value`() {
        // When
        val result = sessionService.gmidLatest()
        
        // Then
        // The result should be an empty string since TestResourceProvider returns empty encrypted preferences
        assertEquals("", result)
    }

    // Integration-style test demonstrating the full flow
    @Test
    fun `session workflow should work correctly`() {
        // Given a fresh session service
        every { mockSessionSecureProvider.availableSession() } returns false andThen true
        every { mockSessionSecureProvider.getSession() } returns null andThen testSession
        every { mockSessionSecureProvider.sessionSecureLevel() } returns SessionSecureLevel.STANDARD
        
        // Initially no session should be available
        assertFalse(sessionService.availableSession())
        assertNull(sessionService.getSession())
        
        // Set a session
        sessionService.setSession(testSession)
        
        // Now session should be available
        assertTrue(sessionService.availableSession())
        assertEquals(testSession, sessionService.getSession())
        assertEquals(SessionSecureLevel.STANDARD, sessionService.sessionSecureLevel())
        
        // Clear session
        sessionService.clearSession()
        
        // Verify all expected calls were made
        verify { mockSessionSecureProvider.setSession(testSession) }
        verify { mockSessionSecureProvider.clearSession() }
    }

    // Test error handling
    @Test
    fun `should handle SessionSecureProvider exceptions gracefully`() {
        // Given
        every { mockSessionSecureProvider.getSession() } throws RuntimeException("Test exception")
        
        // When/Then - exception should propagate (this is expected behavior)
        try {
            sessionService.getSession()
            fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
    }
}
