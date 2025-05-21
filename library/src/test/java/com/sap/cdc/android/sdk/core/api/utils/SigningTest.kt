package com.sap.cdc.android.sdk.core.api.utils

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Base64

class SigningTest {

    private lateinit var signing: Signing

    @Before
    fun setUp() {
        signing = Signing(object : Base64Encoder {
            override fun encodeToString(input: ByteArray, flags: Int): String {
                return Base64.getEncoder().encodeToString(input)
            }

            override fun decode(input: String, flags: Int): ByteArray {
                return Base64.getUrlDecoder().decode(input)
            }

            override fun decode(input: ByteArray, flags: Int): ByteArray {
                return Base64.getUrlDecoder().decode(input)
            }
        })
    }

    @Test
    fun testNewSignature() {
        val spec = SigningSpec(
            secret = "encoded_secret",
            api = "https://api.example.com/resource",
            method = "GET",
            queryParameters = mutableMapOf("param1" to "value1", "param2" to "value2")
        )
        val signature = signing.newSignature(spec)
        assertEquals("vwuCq0TsC0o3/TQm11twEa6+b8c=", signature)
    }

    @Test
    fun testUrlEncode() {
        val url = "https://api.example.com/resource?param1=value1&param2=value2"
        val encodedUrl = url.urlEncode()
        assertEquals(
            "https%3A%2F%2Fapi.example.com%2Fresource%3Fparam1%3Dvalue1%26param2%3Dvalue2",
            encodedUrl
        )
    }

    @Test
    fun testToEncodedQuery() {
        val queryParams = mutableMapOf("param1" to "value1", "param2" to "value2")
        val encodedQuery = queryParams.toEncodedQuery()
        assertEquals("param1=value1&param2=value2", encodedQuery)
    }
}