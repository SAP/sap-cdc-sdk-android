package com.sap.cdc.android.sdk.auth.provider

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.session.Session

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

enum class ProviderType {
    NATIVE, WEB, SSO
}

interface IAuthenticationProvider {

    fun getProvider(): String

    suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult

    suspend fun signOut(hostActivity: ComponentActivity?)

    fun dispose()

}

class AuthenticatorProviderResult(val provider: String, val type: ProviderType) {

    var providerSessions: String? = null
    var session: Session? = null
    var ssoData: SSOAuthenticationData? = null

    constructor(provider: String, type: ProviderType, providerSessions: String) : this(
        provider,
        type
    ) {
        this.providerSessions = providerSessions
    }

    constructor(provider: String, type: ProviderType, session: Session) : this(provider, type) {
        this.session = session
    }
}

data class SSOAuthenticationData(
    val code: String? = null,
    var redirectUri: String? = null,
    var verifier: String? = null,
)