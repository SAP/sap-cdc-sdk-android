package com.sap.cdc.android.sdk.extensions

import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.api.utils.Base64Encoder
import org.json.JSONObject
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale


/**
 * String extension functions for SAP CDC SDK.
 * 
 * Provides utility functions for string manipulation, cryptographic operations,
 * JWT decoding, URL building, and query string parsing.
 * 
 * @author Tal Mirmelshtein
 * @since 21/06/2024
 * 
 * Copyright: SAP LTD.
 */

/**
 * Capitalizes the first character of the string.
 * @return String with first character capitalized
 */
fun String.capitalFirst(): String = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.getDefault()
    ) else it.toString()
}

/**
 * Generates a cryptographic nonce for OAuth/PKCE flows.
 * @param base64Encoder Base64 encoder for encoding the nonce
 * @return Base64-encoded random nonce string
 */
fun String.generateNonce(base64Encoder: Base64Encoder): String {
    val nonceBytes = ByteArray(40)
    val random = SecureRandom()
    random.nextBytes(nonceBytes)
    return base64Encoder.encodeToString(nonceBytes, 8)
}

/**
 * Prepares a complete API URL from an endpoint path.
 * Handles both CNAME and standard domain configurations.
 * @param siteConfig Site configuration containing domain/CNAME settings
 * @return Fully qualified HTTPS URL for the API endpoint
 */
fun String.prepareApiUrl(siteConfig: SiteConfig): String {
    val sb = StringBuilder()
    val split = this.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()
    if (siteConfig.cname != null) {
        sb.append("https://")
            .append(siteConfig.cname)
            .append("/")
            .append(this)
            .toString();
    } else {
        sb.append("https://")
            .append(split[0]).append(".")
            .append(siteConfig.domain)
            .append("/")
            .append(this)
            .toString()
    }
    return sb.toString()
}

/**
 * Parses a query string into a map of key-value pairs.
 * Handles URL decoding of values.
 * @return Map of decoded query parameters
 */
fun String.parseQueryStringParams(): Map<String, String> =
    this.split("&")
        .mapNotNull { keyAndValue ->
            val keyAndValueList = keyAndValue.split("=")
            keyAndValueList.takeIf { it.size == 2 }
        }
        .mapNotNull { keyAndValueList ->
            val (key, value) = keyAndValueList
            (key to URLDecoder.decode(value.trim(), "UTF8")).takeIf {
                value.isNotBlank()
            }
        }
        .toMap()

/**
 * Decodes a JWT token and returns the payload as a JSONObject.
 * @param base64Encoder Base64 encoder for decoding the JWT
 * @return JSONObject containing the decoded JWT payload
 */
fun String.jwtDecode(base64Encoder: Base64Encoder): JSONObject {
    val parts = this.split(".")
    val base64EncodedData = parts[1]
    val data = base64Encoder.decode(
        base64EncodedData.toByteArray(charset = StandardCharsets.UTF_8),
        0
    )
    return JSONObject(String(data, StandardCharsets.UTF_8))
}

/**
 * Encodes a string using the specified hash algorithm.
 * @param algorithm The hash algorithm (e.g., "SHA-512")
 * @return Hexadecimal string representation of the hash (128 chars)
 */
fun String.encodeWith(algorithm: String): String {
    val md: MessageDigest = MessageDigest.getInstance(algorithm)
    val messageDigest = md.digest(this.toByteArray())
    val no = BigInteger(1, messageDigest)
    var hash: String = no.toString(16)
    // Add preceding 0s to make it 128 chars long
    while (hash.length < 128) {
        hash = "0$hash"
    }
    return hash
}
