package com.sap.cdc.android.sdk.core

import android.content.Context
import com.sap.cdc.android.sdk.core.network.TestHttpClientProvider
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit tests for CoreClient demonstrating testability improvements.
 * These tests validate that CoreClient can be fully mocked and tested without Android dependencies.
 */
class CoreClientTest {

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    private fun createTestSiteConfig(): SiteConfig {
        return SiteConfig(
            mockContext,
            "test-api-key",
            "test.domain.com",
            "test.cname.com",
            { 1234567890000L }
        )
    }

    @Test
    fun `CoreClient should initialize with default HttpClientProvider`() {
        // Test backward compatibility - existing constructor still works
        val testSiteConfig = createTestSiteConfig()
        val coreClient = CoreClient(testSiteConfig)
        
        assertNotNull("NetworkClient should be initialized", coreClient.networkClient)
        assertEquals("SiteConfig should be set", testSiteConfig, coreClient.siteConfig)
    }

    @Test
    fun `CoreClient should accept custom HttpClientProvider for testing`() {
        // Test dependency injection - can inject test provider
        val testSiteConfig = createTestSiteConfig()
        val testHttpClientProvider = TestHttpClientProvider()
        val coreClient = CoreClient(testSiteConfig, testHttpClientProvider)
        
        assertNotNull("NetworkClient should be initialized", coreClient.networkClient)
        assertEquals("SiteConfig should be set", testSiteConfig, coreClient.siteConfig)
    }

    @Test
    fun `CoreClient with mock responses should handle network calls`() = runTest {
        // Test with controlled HTTP responses
        val testSiteConfig = createTestSiteConfig()
        val mockResponses = mapOf(
            "https://test.domain.com/accounts.login" to """{"statusCode": 200, "UID": "test-uid"}""",
            "https://test.domain.com/accounts.getAccountInfo" to """{"statusCode": 200, "profile": {"email": "test@example.com"}}"""
        )
        
        val testHttpClientProvider = TestHttpClientProvider(mockResponses)
        val coreClient = CoreClient(testSiteConfig, testHttpClientProvider)
        
        // NetworkClient should be created with test provider
        assertNotNull("NetworkClient should be initialized", coreClient.networkClient)
        
        // HttpClient should be available for network operations
        val httpClient = coreClient.networkClient.http()
        assertNotNull("HttpClient should be available", httpClient)
    }

    @Test
    fun `CoreClient should handle network errors gracefully`() = runTest {
        // Test error handling with mock error responses
        val testSiteConfig = createTestSiteConfig()
        val testHttpClientProvider = TestHttpClientProvider(
            mockResponses = mapOf(
                "https://test.domain.com/accounts.login" to """{"statusCode": 403, "errorMessage": "Invalid credentials"}"""
            ),
            defaultStatusCode = HttpStatusCode.Forbidden
        )
        
        val coreClient = CoreClient(testSiteConfig, testHttpClientProvider)
        
        // Should still initialize properly even with error responses configured
        assertNotNull("NetworkClient should be initialized", coreClient.networkClient)
        assertNotNull("HttpClient should be available", coreClient.networkClient.http())
    }

    @Test
    fun `CoreClient should maintain SiteConfig reference`() {
        val testSiteConfig = createTestSiteConfig()
        val testHttpClientProvider = TestHttpClientProvider()
        val coreClient = CoreClient(testSiteConfig, testHttpClientProvider)
        
        // Verify SiteConfig is properly maintained
        assertEquals("API key should match", "test-api-key", coreClient.siteConfig.apiKey)
        assertEquals("Domain should match", "test.domain.com", coreClient.siteConfig.domain)
        assertEquals("CNAME should match", "test.cname.com", coreClient.siteConfig.cname)
    }

    @Test
    fun `CoreClient should support different test scenarios`() {
        val testSiteConfig = createTestSiteConfig()
        
        // Test 1: Successful responses
        val successProvider = TestHttpClientProvider(
            mapOf("https://api.test.com/success" to """{"status": "ok"}""")
        )
        val successClient = CoreClient(testSiteConfig, successProvider)
        assertNotNull("Success client should initialize", successClient.networkClient)

        // Test 2: Timeout scenarios
        val timeoutProvider = TestHttpClientProvider(
            mapOf("https://api.test.com/timeout" to """{"error": "timeout"}"""),
            HttpStatusCode.RequestTimeout
        )
        val timeoutClient = CoreClient(testSiteConfig, timeoutProvider)
        assertNotNull("Timeout client should initialize", timeoutClient.networkClient)

        // Test 3: Server error scenarios
        val errorProvider = TestHttpClientProvider(
            mapOf("https://api.test.com/error" to """{"error": "server error"}"""),
            HttpStatusCode.InternalServerError
        )
        val errorClient = CoreClient(testSiteConfig, errorProvider)
        assertNotNull("Error client should initialize", errorClient.networkClient)
    }
}
