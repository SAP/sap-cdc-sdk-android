package com.sap.cdc.android.sdk.core.api

import android.util.Log
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.BuildConfig
import io.ktor.http.HttpMethod
import io.ktor.util.generateNonce

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class Request(
    siteConfig: SiteConfig
) {
    private var method: String = HttpMethod.Post.value // Default method is post.
    var api: String = ""
    var parameters: MutableMap<String, String> = sortedMapOf(
        "apiKey" to siteConfig.apiKey,
        "sdk" to "Android_${BuildConfig.VERSION}",
        "targetEnv" to "mobile",
        "format" to "json",
        "httpStatusCodes" to "false",
        "nonce" to generateNonce(),
    )

    private var userAgent: String? = null

    var headers: MutableMap<String, String> = mutableMapOf(
        "apiKey" to siteConfig.apiKey
    )

    init {
        try {
            userAgent = System.getProperty("http.agent")
        } catch (ex: Exception) {
            Log.d("Request", "Unable to fetch system property http.agent.")
        }
        if (userAgent != null) {
            headers["User-Agent"] = userAgent!!
        }
    }

    fun method(method: String) = apply {
        this.method = method
    }

    fun parameters(parameters: MutableMap<String, String>) = apply {
        this.parameters += parameters
    }

    fun headers(headers: MutableMap<String, String>?) = apply {
        if (headers != null) {
            this.headers += headers
        }
    }

    fun gmid(gmid: String) = apply {
        parameters["gmid"] = gmid
    }

    fun api(api: String) = apply {
        this.api = api
    }

    fun authenticated(token: String) = apply {
        parameters["oauth_token"] = token
    }

    fun timestamp(timestamp: String) = apply {
        parameters["timestamp"] = timestamp
    }

    fun sign(secret: String) = apply {
        val signature = Signing().newSignature(
            SigningSpec(
                secret,
                api,
                method,
                parameters
            )
        )
        parameters["sig"] = signature
    }

}