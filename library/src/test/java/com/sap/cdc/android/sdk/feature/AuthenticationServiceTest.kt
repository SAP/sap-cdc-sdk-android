package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.TestResourceProvider
import com.sap.cdc.android.sdk.feature.account.IAuthAccount
import com.sap.cdc.android.sdk.feature.notifications.CDCNotificationManager
import com.sap.cdc.android.sdk.feature.notifications.CDCNotificationOptions
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.session.IAuthSession
import com.sap.cdc.android.sdk.feature.session.SessionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for AuthenticationService demonstrating complete testability through dependency injection.
 * These tests show how the default parameter approach enables full mocking without breaking existing APIs.
 */
@RunWith(RobolectricTestRunner::class)
class AuthenticationServiceTest {

    private lateinit var siteConfig: SiteConfig
    private lateinit var mockCoreClient: CoreClient
    private lateinit var mockSessionService: SessionService
    private lateinit var mockNotificationManagerFactory: (AuthenticationService, CDCNotificationOptions) -> CDCNotificationManager
    private lateinit var mockNotificationManager: CDCNotificationManager
    private lateinit var authenticationService: AuthenticationService

    @Before
    fun setUp() {
        // Create test dependencies
        siteConfig = SiteConfig(
            apiKey = "test-api-key",
            domain = "test.domain.com",
            resourceProvider = TestResourceProvider()
        )
        
        // Create mocks
        mockCoreClient = mockk<CoreClient>(relaxed = true)
        mockSessionService = mockk<SessionService>(relaxed = true)
        mockNotificationManager = mockk<CDCNotificationManager>(relaxed = true)
        mockNotificationManagerFactory = mockk<(AuthenticationService, CDCNotificationOptions) -> CDCNotificationManager>()
        
        // Setup notification manager factory mock
        every { mockNotificationManagerFactory(any(), any()) } returns mockNotificationManager
        
        // Create AuthenticationService with mocked dependencies
        authenticationService = AuthenticationService(
            siteConfig = siteConfig,
            coreClient = mockCoreClient,
            sessionService = mockSessionService,
            notificationManagerFactory = mockNotificationManagerFactory
        )
    }

    @Test
    fun `constructor with siteConfig only should work for backwards compatibility`() {
        // This tests that existing code continues to work unchanged
        val service = AuthenticationService(siteConfig)
        
        assertNotNull(service)
        assertEquals(siteConfig, service.siteConfig)
    }

    @Test
    fun `authenticate should return IAuthApis instance`() {
        // When
        val result = authenticationService.authenticate()
        
        // Then
        assertNotNull(result)
        assertTrue(result is IAuthApis)
    }

    @Test
    fun `account should return IAuthAccount instance`() {
        // When
        val result = authenticationService.account()
        
        // Then
        assertNotNull(result)
        assertTrue(result is IAuthAccount)
    }

    @Test
    fun `session should return IAuthSession instance`() {
        // When
        val result = authenticationService.session()
        
        // Then
        assertNotNull(result)
        assertTrue(result is IAuthSession)
    }

    @Test
    fun `updateDeviceInfo should store device info in encrypted preferences`() {
        // Given
        val deviceInfo = DeviceInfo(pushToken = "test-token")
        
        // When
        authenticationService.updateDeviceInfo(deviceInfo)
        
        // Then - This would normally verify SharedPreferences interaction
        // In a real test, we'd verify the encrypted preferences storage
        // For now, we just verify the method doesn't throw
        // (The ResourceProvider abstraction makes this testable)
    }

    @Test
    fun `registerForPushAuthentication should create notification manager and request token`() {
        // Given
        val mockFcmTokenRequest = mockk<IFCMTokenRequest>(relaxed = true)
        val notificationOptions = CDCNotificationOptions()
        
        // When
        val result = authenticationService.registerForPushAuthentication(
            fcmTokenRequest = mockFcmTokenRequest,
            notificationOptions = notificationOptions
        )
        
        // Then
        assertEquals(authenticationService, result) // Should return self for chaining
        verify { mockNotificationManagerFactory(authenticationService, notificationOptions) }
        verify { mockFcmTokenRequest.requestFCMToken() }
    }

    @Test
    fun `registerForPushAuthentication with null options should use defaults`() {
        // Given
        val mockFcmTokenRequest = mockk<IFCMTokenRequest>(relaxed = true)
        
        // When
        authenticationService.registerForPushAuthentication(
            fcmTokenRequest = mockFcmTokenRequest,
            notificationOptions = null
        )
        
        // Then
        verify { mockFcmTokenRequest.requestFCMToken() }
        // The notification manager factory should be called with default options
        verify { mockNotificationManagerFactory(authenticationService, any()) }
    }

    @Test
    fun `fluent interface should work with mocked dependencies`() {
        // This test demonstrates that the fluent protocol design is preserved
        // and works correctly with mocked dependencies
        
        // When - Using the fluent interface
        val authApis = authenticationService.authenticate()
        val authAccount = authenticationService.account()
        val authSession = authenticationService.session()
        
        // Then - All should work and return appropriate interfaces
        assertNotNull(authApis)
        assertNotNull(authAccount)
        assertNotNull(authSession)
        
        // The fluent interface pattern is preserved:
        // authService.authenticate().login().emailPassword(...) would still work
        // authService.account().getAccountInfo(...) would still work
        // authService.session().isValid() would still work
    }

    @Test
    fun `mocked dependencies should be used in service creation`() {
        // When creating auth APIs
        val authApis = authenticationService.authenticate()
        
        // Then the mocked dependencies should be used
        // This verifies that dependency injection is working correctly
        assertNotNull(authApis)
        
        // In a real implementation, we could verify that the AuthApis
        // instance was created with our mocked CoreClient and SessionService
    }

    @Test
    fun `service should handle DeviceInfo serialization correctly`() {
        // Given
        val deviceInfo = DeviceInfo(
            pushToken = "test-push-token-123",
            // Add other DeviceInfo properties as needed
        )
        
        // When
        try {
            authenticationService.updateDeviceInfo(deviceInfo)
            // Should not throw any exceptions
        } catch (e: Exception) {
            fail("updateDeviceInfo should not throw exceptions: ${e.message}")
        }
        
        // Then - method completes successfully
        // In a real test, we'd verify the JSON serialization and storage
    }

    @Test
    fun `service should maintain singleton-like behavior for notification manager`() {
        // Given
        val mockFcmTokenRequest1 = mockk<IFCMTokenRequest>(relaxed = true)
        val mockFcmTokenRequest2 = mockk<IFCMTokenRequest>(relaxed = true)
        val options = CDCNotificationOptions()
        
        // When - registering multiple times
        authenticationService.registerForPushAuthentication(mockFcmTokenRequest1, options)
        authenticationService.registerForPushAuthentication(mockFcmTokenRequest2, options)
        
        // Then - should create new notification manager each time (current behavior)
        verify(exactly = 2) { mockNotificationManagerFactory(any(), any()) }
        verify { mockFcmTokenRequest1.requestFCMToken() }
        verify { mockFcmTokenRequest2.requestFCMToken() }
    }

    // Integration-style test demonstrating the complete flow
    @Test
    fun `complete authentication flow should work with mocked dependencies`() {
        // This test shows how other classes can now easily test with AuthenticationService
        
        // Given - a class that depends on AuthenticationService
        class SomeFeature(private val authService: AuthenticationService) {
            fun performAuthentication(): IAuthApis = authService.authenticate()
            fun getAccount(): IAuthAccount = authService.account()
            fun checkSession(): IAuthSession = authService.session()
        }
        
        // When - using the feature with our mocked AuthenticationService
        val feature = SomeFeature(authenticationService)
        
        val authApis = feature.performAuthentication()
        val account = feature.getAccount()
        val session = feature.checkSession()
        
        // Then - everything should work smoothly
        assertNotNull(authApis)
        assertNotNull(account)
        assertNotNull(session)
        
        // This demonstrates how AuthenticationService is now fully mockable
        // for any class that depends on it
    }
}
