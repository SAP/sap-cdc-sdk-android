package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CDCResponseTest {

    @Test
    fun testFromJSON() {
        val json = """{"callId":"12345","errorCode":0,"errorMessage":"Success"}"""
        val response = CDCResponse().fromJSON(json)
        assertEquals("12345", response.callId())
        assertEquals(0, response.errorCode())
        assertEquals("Success", response.errorMessage())
    }

    @Test
    fun testFromError() {
        val error = CDCError(404, "Not Found", "The requested resource was not found")
        val response = CDCResponse().fromError(error)
        assertEquals(404, response.errorCode())
        assertEquals("Not Found", response.errorMessage())
        assertEquals("The requested resource was not found", response.errorDetails())
    }

    @Test
    fun testFromException() {
        val exception = Exception("Test exception")
        val response = CDCResponse().fromException(exception)
        assertEquals(-1, response.errorCode())
        assertEquals("Test exception", response.errorMessage())
    }

    @Test
    fun testNoNetwork() {
        val response = CDCResponse().noNetwork()
        assertEquals(400106, response.errorCode())
        assertEquals("Not connected", response.errorMessage())
        assertEquals("User is not connected to the required network or to any network", response.errorDetails())
    }

    @Test
    fun testProviderError() {
        val response = CDCResponse().providerError()
        assertEquals(400122, response.errorCode())
        assertEquals("Provider error", response.errorMessage())
        assertEquals("Provider configuration error", response.errorDetails())
    }

    @Test
    fun testIsError() {
        val json = """{"errorCode":1,"errorMessage":"Error"}"""
        val response = CDCResponse().fromJSON(json)
        assertTrue(response.isError())
    }

    @Test
    fun testContainsKey() {
        val json = """{"key1":"value1"}"""
        val response = CDCResponse().fromJSON(json)
        assertTrue(response.containsKey("key1"))
        assertFalse(response.containsKey("key2"))
    }

    @Test
    fun testSerializeTo() {
        val json = """{"key":"value"}"""
        val response = CDCResponse().fromJSON(json)
        val result: JsonObject? = response.serializeTo()
        assertNotNull(result)
        assertEquals("value", result?.get("key")?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun testSerializeObject() {
        val json = """{"key":{"nestedKey":"nestedValue"}}"""
        val response = CDCResponse().fromJSON(json)
        val result: JsonObject? = response.serializeObject("key")
        assertNotNull(result)
        assertEquals("nestedValue", result?.get("nestedKey")?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun testStringField() {
        val json = """{"key":"value"}"""
        val response = CDCResponse().fromJSON(json)
        assertEquals("value", response.stringField("key"))
    }

    @Test
    fun testIntField() {
        val json = """{"key":123}"""
        val response = CDCResponse().fromJSON(json)
        assertEquals(123, response.intField("key"))
    }
}