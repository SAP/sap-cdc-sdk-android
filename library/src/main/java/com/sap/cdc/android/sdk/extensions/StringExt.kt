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
 * Created by Tal Mirmelshtein on 21/06/2024
 * Copyright: SAP LTD.
 */

fun String.capitalFirst(): String = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.getDefault()
    ) else it.toString()
}

fun String.generateNonce(base64Encoder: Base64Encoder): String {
    val nonceBytes = ByteArray(40)
    val random = SecureRandom()
    random.nextBytes(nonceBytes)
    return base64Encoder.encodeToString(nonceBytes, 8)
}

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
 * Helper extension method to parse String value for required missing fields list.
 */
fun String.parseRequiredMissingFieldsForRegistration(): List<String> {
    val fields = this.substringAfterLast(": ")
    return fields.split(", ")
}

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

fun String.jwtDecode(base64Encoder: Base64Encoder): JSONObject {
    val parts = this.split(".")
    val base64EncodedData = parts[1]
    val data = base64Encoder.decode(
        base64EncodedData.toByteArray(charset = StandardCharsets.UTF_8),
        0
    )
    return JSONObject(String(data, StandardCharsets.UTF_8))
}

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