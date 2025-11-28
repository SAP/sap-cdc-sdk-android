package com.sap.cdc.android.sdk.feature.provider.sso

import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.feature.session.Session
import org.json.JSONObject
import java.net.URLEncoder

/**
 * SSO (Single Sign-On) utility for FIDM authentication flows.
 * 
 * Provides URL building and session parsing utilities for OAuth 2.0/OIDC
 * authentication with SAP FIDM (Federated Identity Management).
 * 
 * @author Tal Mirmelshtein
 * @since 13/12/2024
 * 
 * Copyright: SAP LTD.
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
     * Builds the FIDM base URL for SSO operations.
     * Supports both custom CNAME and standard domain configurations.
     * @param siteConfig Site configuration
     * @param path The endpoint path (authorize or token)
     * @return Complete FIDM URL
     */
    fun getUrl(siteConfig: SiteConfig, path: String) =
        when (siteConfig.cname != null) {
            true -> "https://${siteConfig.cname}$fidmPath${siteConfig.apiKey ?: ""}/$path"
            false -> "$fidmUrl${siteConfig.domain}$fidmPath${siteConfig.apiKey ?: ""}/$path"
        }

    /**
     * Generates the OAuth 2.0 authorization URL with PKCE.
     * Used in Custom Tab for user authentication.
     * @param siteConfig Site configuration
     * @param params Optional additional parameters
     * @param redirectUri OAuth redirect URI
     * @param challenge PKCE code challenge
     * @return Complete authorization URL with query parameters
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
     * Parses SSO response into a Session object.
     * Extracts access token, device secret, and expiration from the OAuth response.
     * @param ssoResponseEntity The SSO OAuth response
     * @return Session object with authentication credentials
     */
    fun parseSessionInfo(ssoResponseEntity: SSOResponseEntity): Session {
        return Session(
            token = ssoResponseEntity.access_token!!,
            secret = ssoResponseEntity.device_secret!!,
            expiration = ssoResponseEntity.expires_in)
    }

}
