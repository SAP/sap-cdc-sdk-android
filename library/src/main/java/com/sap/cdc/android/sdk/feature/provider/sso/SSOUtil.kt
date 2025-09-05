package com.sap.cdc.android.sdk.feature.provider.sso

import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.feature.auth.session.Session
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Created by Tal Mirmelshtein on 13/12/2024
 * Copyright: SAP LTD.
 */

/**
 * Single sign on util class.
 */
class SSOUtil {

    companion object {

        private const val LOG_TAG = "SSOProvider"
        private const val fidmPath = "/oidc/op/v1.0/"
        private const val fidmUrl = "https://fidm."

        // Paths
        private const val AUTHORIZE = "authorize"
        const val TOKEN = "token"
    }

    /**
     * Build fidm base url.
     */
    fun getUrl(siteConfig: SiteConfig, path: String) =
        when (siteConfig.cname != null) {
            true -> "https://${siteConfig.cname}$fidmPath${siteConfig.apiKey ?: ""}/$path"
            false -> "$fidmUrl${siteConfig.domain}$fidmPath${siteConfig.apiKey ?: ""}/$path"
        }

    /**
     * Get authorization URL.
     * URL will be used in the Custom tab implementation to authenticate the user.
     */
    fun getAuthorizeUrl(
        siteConfig: SiteConfig,
        params: MutableMap<String, Any>?,
        redirectUri: String,
        challenge: String,
    ): String {
        val urlString = getUrl(siteConfig, AUTHORIZE)

        val serverParams: MutableMap<String, Any> = mutableMapOf(
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "client_id" to siteConfig.apiKey,
            "scope" to "device_sso",
            "code_challenge" to challenge,
            "code_challenge_method" to "S256"
        )

        // Evaluate context & parameters.
        params?.let {
            params.forEach { entry ->
                print("${entry.key} : ${entry.value}")
                if (entry.value is Map<*, *>) {
                    val json = JSONObject(entry.value as Map<*, *>).toString()
                    params[entry.key] = json
                }
            }
            serverParams += params
        }

        val queryString = serverParams.entries.joinToString("&")
        { "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
        return "$urlString?$queryString"
    }

    /**
     * Parse the SessionInfo object from code exchange response.
     */
    fun parseSessionInfo(ssoResponseEntity: SSOResponseEntity): Session {
        return Session(
            token = ssoResponseEntity.access_token!!,
            secret = ssoResponseEntity.device_secret!!,
            expiration = ssoResponseEntity.expires_in)
    }

}