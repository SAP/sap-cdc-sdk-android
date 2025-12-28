package com.sap.cdc.android.sdk.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit tests for SiteConfig demonstrating improved testability through
 * ResourceProvider abstraction and dependency injection.
 */
class SiteConfigTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var testResourceProvider: TestResourceProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testResourceProvider = TestResourceProvider()
    }

    @Test
    fun testSiteConfigWithMockedResources() {
        // Arrange
        testResourceProvider.setString("com.sap.cxcdc.apikey", "test-api-key-123")
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.gigya.com")
        testResourceProvider.setString("com.sap.cxcdc.cname", "custom.domain.com")

        // Act
        val siteConfig = SiteConfig(mockContext, testResourceProvider)

        // Assert
        assertEquals("test-api-key-123", siteConfig.apiKey)
        assertEquals("test.gigya.com", siteConfig.domain)
        assertEquals("custom.domain.com", siteConfig.cname)
        assertNotNull(siteConfig.applicationContext)
    }

    @Test
    fun testSiteConfigWithMissingOptionalResource() {
        // Arrange - only required resources
        testResourceProvider.setString("com.sap.cxcdc.apikey", "test-api-key")
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.domain.com")
        // Note: cname is optional and not provided

        // Act
        val siteConfig = SiteConfig(mockContext, testResourceProvider)

        // Assert
        assertEquals("test-api-key", siteConfig.apiKey)
        assertEquals("test.domain.com", siteConfig.domain)
        assertEquals(null, siteConfig.cname) // Should be null when not provided
    }

    @Test
    fun testSiteConfigWithMissingRequiredResource() {
        // Arrange - missing required resource
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.domain.com")
        // Note: apikey is required but not provided

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            SiteConfig(mockContext, testResourceProvider)
        }
    }

    @Test
    fun testServerTimestampWithMockedTime() {
        // Arrange
        val fixedTime = 1609459200000L // 2021-01-01 00:00:00 UTC
        val timeProvider = { fixedTime }
        
        testResourceProvider.setString("com.sap.cxcdc.apikey", "test-api")
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.domain.com")

        val siteConfig = SiteConfig(
            mockContext, 
            "test-api", 
            "test.domain.com", 
            null, 
            timeProvider
        )

        // Act
        val timestamp = siteConfig.getServerTimestamp()

        // Assert
        assertEquals("1609459200", timestamp) // Fixed time in seconds
    }

    @Test
    fun testServerOffsetCalculation() {
        // Arrange
        val fixedTime = 1609459200000L // 2021-01-01 00:00:00 UTC
        val timeProvider = { fixedTime }
        
        testResourceProvider.setString("com.sap.cxcdc.apikey", "test-api")
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.domain.com")

        val siteConfig = SiteConfig(
            mockContext, 
            "test-api", 
            "test.domain.com", 
            null, 
            timeProvider
        )

        // Act - set server time 1 hour ahead
        val serverDateString = "Fri, 01 Jan 2021 01:00:00 GMT" // 1 hour ahead
        siteConfig.setServerOffset(serverDateString)
        val timestampWithOffset = siteConfig.getServerTimestamp()

        // Assert - should include 1 hour (3600 seconds) offset
        assertEquals("1609462800", timestampWithOffset) // fixedTime + 3600 seconds
    }

    @Test
    fun testServerOffsetWithInvalidDate() {
        // Arrange
        testResourceProvider.setString("com.sap.cxcdc.apikey", "test-api")
        testResourceProvider.setString("com.sap.cxcdc.domain", "test.domain.com")

        // Create SiteConfig with testable constructor
        val siteConfig = SiteConfig(
            mockContext,
            "test-api",
            "test.domain.com",
            null,
            { System.currentTimeMillis() }
        )
        val originalTimestamp = siteConfig.getServerTimestamp()

        // Act - set invalid date
        siteConfig.setServerOffset("invalid-date-format")
        val timestampAfterInvalid = siteConfig.getServerTimestamp()

        // Assert - timestamp should remain unchanged
        assertEquals(originalTimestamp, timestampAfterInvalid)
    }

    @Test
    fun testDirectConstructorForBackwardCompatibility() {
        // Act - using direct constructor (testable version)
        val siteConfig = SiteConfig(
            mockContext,
            "direct-api-key",
            "direct.domain.com",
            "direct.cname.com"
        )

        // Assert
        assertEquals("direct-api-key", siteConfig.apiKey)
        assertEquals("direct.domain.com", siteConfig.domain)
        assertEquals("direct.cname.com", siteConfig.cname)
    }
}
