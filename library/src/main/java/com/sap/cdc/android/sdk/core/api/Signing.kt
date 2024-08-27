package com.sap.cdc.android.sdk.core.api

import android.util.Base64
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class Signing {

    companion object {
        const val SIGNING_ALGORITHM = "HmacSHA1"
    }

    fun newSignature(spec: SigningSpec): String {
        return encode(spec.secret, normalizeUrl(spec))
    }

    private fun normalizeUrl(
        spec: SigningSpec
    ): String {
        println("normalizedUrl_: ${spec.api}")
        return "${spec.method}&${spec.api.urlEncode()}&${
            spec.queryParameters.toEncodedQuery().urlEncode()
        }"
    }

    private fun encode(
        secret: String,
        normalizedUrl: String,
    ): String {
        println("baseSignature_: $normalizedUrl")
        val keyBytes = Base64.decode(secret, Base64.DEFAULT)
        val textData: ByteArray = normalizedUrl.toByteArray(StandardCharsets.UTF_8)
        val signingKey =
            SecretKeySpec(keyBytes, SIGNING_ALGORITHM)
        val mac = Mac.getInstance(SIGNING_ALGORITHM)
        mac.init(signingKey)
        val rawHmac = mac.doFinal(textData)
        return Base64.encodeToString(rawHmac, Base64.NO_WRAP or Base64.URL_SAFE)
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
    fun fromRequest(request: Request): SigningSpec {
        return this
    }
}
