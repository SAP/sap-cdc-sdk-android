package com.sap.cdc.android.sdk.core.api.utils

/**
 * Created by Tal Mirmelshtein on 04/04/2025
 * Copyright: SAP LTD.
 */

/**
 * Base64Encoder interface for encoding and decoding Base64 strings.
 * This interface used to allow different implementations of Base64 encoding and decoding.
 * Mostly used for testing purposes.
 */
interface Base64Encoder {
    fun encodeToString(input: ByteArray, flags: Int): String
    fun decode(input: String, flags: Int): ByteArray
    fun decode(input: ByteArray, flags: Int): ByteArray
}

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