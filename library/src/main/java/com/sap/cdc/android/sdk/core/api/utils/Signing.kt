package com.sap.cdc.android.sdk.core.api.utils

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.CDCRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class Signing(private val base64Encoder: Base64Encoder) {

    companion object {
        private const val LOG_TAG = "Signing"
        const val SIGNING_ALGORITHM = "HmacSHA1"
    }

    fun newSignature(spec: SigningSpec): String {
        val normalizedUrl = normalizeUrl(spec)
        CDCDebuggable.log(LOG_TAG, "baseSignature_: $normalizedUrl")
        val keyBytes = base64Encoder.decode(spec.secret, android.util.Base64.DEFAULT)
        val textData: ByteArray = normalizedUrl.toByteArray(StandardCharsets.UTF_8)
        val signingKey = SecretKeySpec(keyBytes, SIGNING_ALGORITHM)
        val mac = Mac.getInstance(SIGNING_ALGORITHM)
        mac.init(signingKey)
        val rawHmac = mac.doFinal(textData)
        return base64Encoder.encodeToString(
            rawHmac,
            android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        )
    }

    private fun normalizeUrl(
        spec: SigningSpec
    ): String {
        CDCDebuggable.log(LOG_TAG, "normalizedUrl_: ${spec.api}")
        return "${spec.method}&${spec.api.urlEncode()}&${
            spec.queryParameters.toEncodedQuery().urlEncode()
        }"
    }
}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8").replace("+", "%20")
        .replace("*", "%2A").replace("%7E", "~")
}

fun MutableMap<String, String>.toEncodedQuery(): String {
    return entries.stream()
        .map { (k, v) ->
            "$k=${v.urlEncode()}"
        }
        .reduce { p1, p2 -> "$p1&$p2" }
        .orElse("")
}

class SigningSpec(
    var secret: String,
    var api: String,
    var method: String,
    var queryParameters: MutableMap<String, String>
) {
    fun fromRequest(request: CDCRequest): SigningSpec {
        return this
    }
}
