package com.sap.cdc.android.sdk.core.api.utils

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.CDCRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Request signing utility for SAP CDC OAuth 1.0 authentication.
 * 
 * Generates HMAC-SHA1 signatures for CDC API requests to ensure request authenticity.
 * The signature is computed from the HTTP method, URL, and query parameters.
 * 
 * @property base64Encoder Base64 encoder for encoding/decoding signature components
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see SigningSpec
 */
class Signing(private val base64Encoder: Base64Encoder) {

    companion object {
        private const val LOG_TAG = "Signing"
        const val SIGNING_ALGORITHM = "HmacSHA1"
    }

    /**
     * Generates an HMAC-SHA1 signature for a CDC API request.
     * 
     * @param spec Signing specification containing secret, URL, method, and parameters
     * @return Base64-encoded signature string
     */
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

/**
 * URL-encodes a string for use in OAuth signatures.
 * Applies RFC 3986 encoding rules specific to OAuth.
 * @return URL-encoded string
 */
fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8").replace("+", "%20")
        .replace("*", "%2A").replace("%7E", "~")
}

/**
 * Converts a map of parameters to an encoded query string.
 * @return Encoded query string in format "key1=value1&key2=value2"
 */
fun MutableMap<String, String>.toEncodedQuery(): String {
    return entries.stream()
        .map { (k, v) ->
            "$k=${v.urlEncode()}"
        }
        .reduce { p1, p2 -> "$p1&$p2" }
        .orElse("")
}

/**
 * Specification for request signing.
 * 
 * Contains all components needed to generate an OAuth 1.0 signature.
 * 
 * @property secret Base64-encoded secret key for HMAC signing
 * @property api The CDC API endpoint URL
 * @property method HTTP method (GET or POST)
 * @property queryParameters Request parameters to include in signature
 */
class SigningSpec(
    var secret: String,
    var api: String,
    var method: String,
    var queryParameters: MutableMap<String, String>
) {
    /**
     * Creates a SigningSpec from a CDCRequest.
     * @param request The CDC request to create spec from
     * @return This SigningSpec instance
     */
    fun fromRequest(request: CDCRequest): SigningSpec {
        return this
    }
}
