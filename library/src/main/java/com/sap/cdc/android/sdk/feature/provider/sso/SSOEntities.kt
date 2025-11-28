package com.sap.cdc.android.sdk.feature.provider.sso

import kotlinx.serialization.Serializable

/**
 * SSO (Single Sign-On) response entity.
 * 
 * Contains OAuth 2.0 token response data from SSO authentication flow.
 * 
 * @property access_token OAuth access token
 * @property token_type Token type (typically "Bearer")
 * @property expires_in Token expiration time in seconds
 * @property id_token OpenID Connect ID token
 * @property refresh_token Refresh token for obtaining new access tokens
 * @property device_secret Device-specific secret for additional security
 * 
 * @author Tal Mirmelshtein
 * @since 15/12/2024
 * 
 * Copyright: SAP LTD.
 */
@Serializable
data class SSOResponseEntity(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Long = 0L,
    val id_token: String?,
    val refresh_token: String?,
    val device_secret: String?
)
