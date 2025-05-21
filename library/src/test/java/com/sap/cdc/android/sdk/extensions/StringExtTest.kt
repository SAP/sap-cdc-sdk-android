package com.sap.cdc.android.sdk.extensions

import android.content.Context
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.api.utils.Base64Encoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Base64

class StringExtTest {

    private val mockContext: Context = mock(Context::class.java)
    private val base64Encode: Base64Encoder = object : Base64Encoder {
        override fun encodeToString(input: ByteArray, flags: Int): String {
            return Base64.getEncoder().encodeToString(input)
        }

        override fun decode(input: String, flags: Int): ByteArray {
            return Base64.getUrlDecoder().decode(input)
        }

        override fun decode(input: ByteArray, flags: Int): ByteArray {
            return Base64.getUrlDecoder().decode(input)
        }
    }

    @Test
    fun testCapitalFirst() {
        assertEquals("Hello", "hello".capitalFirst())
        assertEquals("Hello", "Hello".capitalFirst())
        assertEquals("123abc", "123abc".capitalFirst())
    }

    @Test
    fun testGenerateNonce() {
        val nonce = "test".generateNonce(base64Encode)
        assertNotNull(nonce)
        assertTrue(nonce.isNotEmpty())
    }

    @Test
    fun testPrepareApiUrl() {
        val siteConfig = SiteConfig(
            applicationContext = mockContext,
            apiKey = "testApiKey",
            domain = "us1-gigya.com",
            cname = null
        )
        val url = "test.endpoint".prepareApiUrl(siteConfig)
        assertEquals("https://test.us1-gigya.com/test.endpoint", url)
    }

    @Test
    fun testParseRequiredMissingFieldsForRegistration() {
        val fields =
            "Missing fields: field1, field2, field3".parseRequiredMissingFieldsForRegistration()
        assertEquals(listOf("field1", "field2", "field3"), fields)
    }

    @Test
    fun testParseQueryStringParams() {
        val queryString = "param1=value1&param2=value2"
        val params = queryString.parseQueryStringParams()
        assertEquals(mapOf("param1" to "value1", "param2" to "value2"), params)
    }

    @Test
    fun testJwtDecode() {
        val jwt = "header.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature"
        val json = jwt.jwtDecode(base64Encode)
        assertEquals("1234567890", json.getString("sub"))
    }

    @Test
    fun testEncodeWith() {
        val encoded = "test".encodeWith("SHA-256")
        assertNotNull(encoded)
        assertEquals(128, encoded.length)
    }
}