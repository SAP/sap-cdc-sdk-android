package com.sap.cdc.android.sdk.session.api

import android.util.Log
import com.sap.cdc.android.sdk.session.BuildConfig
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.SiteConfig
import com.sap.cdc.android.sdk.session.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.session.session.Session
import io.ktor.http.HttpMethod
import io.ktor.util.generateNonce

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class Request(
    private val siteConfig: SiteConfig
) {

    private var method: String = HttpMethod.Post.value // Default method is post.
    var api: String = ""
    var parameters: MutableMap<String, String> = mutableMapOf(
        "apiKey" to siteConfig.apiKey,
        "sdk" to BuildConfig.VERSION,
        "targetEnv" to "mobile",
        "format" to "json",
        "httpStatusCodes" to false.toString(),
        "nonce" to newNonce(),
    )

    private var userAgent: String? = null

    var headers: MutableMap<String, String> = mutableMapOf(
        "apiKey" to siteConfig.apiKey
    )


    fun method(method: String) = apply {
        this.method = method
    }

    fun api(api: String) = apply {
        this.api = api
    }

    fun parameters(parameters: MutableMap<String, String>) = apply {
        this.parameters += parameters
    }

    fun headers(headers: MutableMap<String, String>?) = apply {
        if (headers != null) {
            this.headers += headers
        }
    }

    fun sign(session: Session) = apply {
        val signature = Signing().newSignature(
            SigningSpec(
                session.secret,
                api,
                method,
                parameters
            )
        )
        parameters["sig"] = signature
    }

    init {
        gmid()?.let {
            parameters["gmid"] = it
        }

        try {
            userAgent = System.getProperty("http.agent")
        } catch (ex: Exception) {
            Log.d("Request", "Unable to fetch system property http.agent.")
        }
        if (userAgent != null) {
            headers["User-Agent"] = userAgent!!
        }
    }

    //region DYNAMIC PARAMETERS

    // Generate unique nonce.
    private fun newNonce(): String = generateNonce()
    //System.currentTimeMillis().toString() + "_" + SecureRandom().nextInt()

    // Fetch GMID from secure preferences.
    private fun gmid(): String? {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(SessionService.CDC_SECURE_PREFS)
        if (esp.contains("gmid")) {
            return esp.getString("gmid", null)
        }
        return null
    }

    //endregion

}