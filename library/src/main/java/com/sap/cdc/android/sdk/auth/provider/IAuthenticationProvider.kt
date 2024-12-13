package com.sap.cdc.android.sdk.auth.provider

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.session.Session

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Authentication provider type.
 * Native - used for native social provider authentication. Native providers are providers that required
 * their own SDK code base implementation for their oauth2 flows. Currently supporting Google, Facebook, WeChat, Line.
 * Web - Any other social provider that is not native implemented.
 * SSO - Single sign on authentication flow using a central login page.
 *
 */
enum class ProviderType {
    NATIVE, WEB, SSO
}

/**
 * Authentication provider default interface.
 */
interface IAuthenticationProvider {

    fun getProvider(): String

    suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult

    suspend fun signOut(hostActivity: ComponentActivity?)

    fun dispose()

}

/**
 * Result provided from provider authentication flow.
 * providerSessions - used for social provider authentication.
 * session - used for web based social provider authentication - sdk internal use.
 * ssoData - used for sso flow - sdk internal use.
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

data class SSOAuthenticationData(
    val code: String? = null,
    var redirectUri: String? = null,
    var verifier: String? = null,
)