package com.sap.cdc.android.sdk.feature.provider

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.feature.session.Session

/**
 * Authentication provider interfaces and types for SAP CDC.
 * 
 * Defines the contract for authentication providers including native social providers,
 * web-based providers, and SSO flows.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */

/**
 * Authentication provider type.
 * 
 * - NATIVE: Native social provider (Google, Facebook, WeChat, Line)
 * - WEB: Web-based social providers
 * - SSO: Single sign-on authentication flow
 */
enum class ProviderType {
    NATIVE, WEB, SSO
}

/**
 * Authentication provider interface.
 * 
 * Defines the contract for all authentication providers in the SDK.
 */
interface IAuthenticationProvider {

    /**
     * Gets the provider identifier.
     * @return Provider name string
     */
    fun getProvider(): String

    /**
     * Initiates sign-in flow for this provider.
     * @param hostActivity Optional host activity for launching UI
     * @return AuthenticatorProviderResult containing authentication data
     */
    suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult

    /**
     * Signs out from this provider.
     * @param hostActivity Optional host activity
     */
    suspend fun signOut(hostActivity: ComponentActivity?)

    /**
     * Cleans up resources used by the provider.
     */
    fun dispose()

}

/**
 * Result from provider authentication flow.
 * 
 * Contains authentication data specific to the provider type:
 * - providerSessions: For social provider authentication
 * - session: For web-based authentication (internal)
 * - ssoData: For SSO flow (internal)
 * 
 * @property provider Provider identifier
 * @property type Provider type
 */
class AuthenticatorProviderResult(val provider: String, val type: ProviderType) {

    var providerSessions: String? = null
    internal var session: Session? = null
    internal var ssoData: SSOAuthenticationData? = null

    /**
     * Public constructor for native social provider authentication.
     */
    constructor(provider: String, type: ProviderType, providerSessions: String) : this(
        provider,
        type
    ) {
        this.providerSessions = providerSessions
    }

    /**
     * Internal constructor for web based social provider authentication.
     */
    internal constructor(provider: String, type: ProviderType, session: Session) : this(
        provider,
        type
    ) {
        this.session = session
    }

    /**
     * Internal constructor for sso flow.
     */
    internal constructor(
        provider: String,
        type: ProviderType,
        ssoData: SSOAuthenticationData
    ) : this(provider, type) {
        this.ssoData = ssoData
    }
}

/**
 * SSO authentication data container.
 * 
 * @property code Authorization code from SSO flow
 * @property redirectUri Redirect URI used in the flow
 * @property verifier PKCE code verifier
 */
data class SSOAuthenticationData(
    val code: String? = null,
    var redirectUri: String? = null,
    var verifier: String? = null,
)
