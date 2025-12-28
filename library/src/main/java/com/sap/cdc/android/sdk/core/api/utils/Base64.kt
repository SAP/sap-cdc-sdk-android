package com.sap.cdc.android.sdk.core.api.utils

/**
 * Base64 encoding and decoding interface.
 * 
 * Abstraction layer for Base64 operations, allowing different implementations
 * for production and testing scenarios. Used primarily in request signing.
 * 
 * @author Tal Mirmelshtein
 * @since 04/04/2025
 * 
 * Copyright: SAP LTD.
 * 
 * @see AndroidBase64Encoder
 * @see Signing
 */
interface Base64Encoder {
    /**
     * Encodes a byte array to a Base64 string.
     * @param input The byte array to encode
     * @param flags Android Base64 flags (e.g., NO_WRAP, URL_SAFE)
     * @return Base64-encoded string
     */
    fun encodeToString(input: ByteArray, flags: Int): String
    
    /**
     * Decodes a Base64 string to a byte array.
     * @param input The Base64 string to decode
     * @param flags Android Base64 flags
     * @return Decoded byte array
     */
    fun decode(input: String, flags: Int): ByteArray
    
    /**
     * Decodes a Base64 byte array to a byte array.
     * @param input The Base64 byte array to decode
     * @param flags Android Base64 flags
     * @return Decoded byte array
     */
    fun decode(input: ByteArray, flags: Int): ByteArray
}

/**
 * Android platform implementation of Base64Encoder.
 * 
 * Delegates to android.util.Base64 for encoding/decoding operations.
 */
class AndroidBase64Encoder : Base64Encoder {
    override fun encodeToString(input: ByteArray, flags: Int): String {
        return android.util.Base64.encodeToString(input, flags)
    }

    override fun decode(input: String, flags: Int): ByteArray {
        return android.util.Base64.decode(input, flags)
    }

    override fun decode(input: ByteArray, flags: Int): ByteArray {
        return android.util.Base64.decode(input, flags)
    }
}
