package com.sap.cdc.android.sdk.session.extensions

import android.util.Base64
import com.sap.cdc.android.sdk.session.SiteConfig
import java.net.URLDecoder
import java.security.SecureRandom

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

fun String.generateNonce(): String {
    val nonceBytes = ByteArray(40)
    val random = SecureRandom()
    random.nextBytes(nonceBytes)
    return Base64.encodeToString(nonceBytes, Base64.URL_SAFE)
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