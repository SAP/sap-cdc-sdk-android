package com.sap.cdc.android.sdk.example.cdc

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.WebAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.example.social.FacebookAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.GoogleAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.LineAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.WeChatAuthenticationProvider
import com.sap.cdc.android.sdk.screensets.WebBridgeJS

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class IdentityServiceRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: IdentityServiceRepository? = null

        fun getInstance(context: Context): IdentityServiceRepository {
            return instance ?: synchronized(this) {
                instance ?: IdentityServiceRepository(context = context).also { instance = it }
            }
        }
    }

    /**
     * Initialize session service.
     */
    private var sessionService = SessionService(SiteConfig(context))

    /**
     * Initialize authentication service.
     */
    private var authenticationService =
        com.sap.cdc.android.sdk.auth.AuthenticationService(sessionService)

    /**
     * Authentication providers map.
     */
    private var authenticationProviderMap: MutableMap<String, IAuthenticationProvider> =
        mutableMapOf()


    init {
        val sessionMigrator = SessionMigrator(context)
        // v6 -> tryMigrateSession (needs identityService as injection)
        if (sessionMigrator.sessionAvailableForMigration()) {
            sessionMigrator.getSession(
                success = { session ->
                    if (session != null) {
                        // Set the session.
                        sessionService.sessionSecure.setSession(session)
                    }
                },
                error = { message ->
                    Log.e(SessionService.LOG_TAG, message)
                }
            )
        }

        // Register application specific authentication providers.
        registerAuthenticationProvider("facebook", FacebookAuthenticationProvider())
        registerAuthenticationProvider("google", GoogleAuthenticationProvider())
        registerAuthenticationProvider("line", LineAuthenticationProvider())
        registerAuthenticationProvider("weChat", WeChatAuthenticationProvider())
    }

    //region CONFIGURATION

    /**
     * Re-init the session service with new site configuration.
     */
    fun reinitializeSessionService(siteConfig: SiteConfig) {
        sessionService.reloadWithSiteConfig(siteConfig)
    }

    /**
     * Get current instance of the current SiteConfig class.
     */
    fun getConfig() = sessionService.siteConfig

    //endregion

    //region SESSION MANAGEMENT

    /**
     * Set a new session (secure it).
     */
    fun setSession(session: Session) {
        sessionService.sessionSecure.setSession(session)
    }

    /**
     * Get current secured session.
     */
    fun getSession(): Session? {
        return sessionService.sessionSecure.getSession()
    }

    /**
     * Clear session secure session.
     */
    fun invalidateSession() {
        sessionService.sessionSecure.clearSession()
    }

    fun validSession(): Boolean = sessionService.sessionSecure.getSession() != null

    //endregion

    //region AUTHENTICATION FLOWS

    /**
     * Initiate cdc SDK credentials registration.
     */
    suspend fun register(email: String, password: String, profileObject: String): IAuthResponse {
        val params =
            mutableMapOf("email" to email, "password" to password, "profile" to profileObject)
        return authenticationService.authenticate().register(params)
    }

    /**
     * Initiate cdc SDK credentials login.
     */
    suspend fun login(email: String, password: String): IAuthResponse {
        val params = mutableMapOf("loginID" to email, "password" to password)
        return authenticationService.authenticate().login(params)
    }

    /**
     * Request cdc SDK latest account information.
     */
    suspend fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        return authenticationService.get().getAccountInfo(parameters!!)
    }

    /**
     * Update cdc SDK account information.
     */
    suspend fun setAccountInfo(parameters: MutableMap<String, String>): IAuthResponse {
        return authenticationService.set().setAccountInfo(parameters)
    }

    /**
     * Logout from current CDC session.
     */
    suspend fun logout(): IAuthResponse {
        return authenticationService.authenticate().logout()
    }

    /**
     * Initiate cdc SDK native social provider login flow.
     */
    suspend fun nativeSocialSignIn(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider
    ): IAuthResponse {
        return authenticationService.authenticate().providerLogin(
            hostActivity, provider
        )
    }

    /**
     * Initiate cdc Web social provider login (any provider that is currently noy native).
     */
    suspend fun webSocialSignIn(
        hostActivity: ComponentActivity,
        socialProvider: String
    ): IAuthResponse {
        val webAuthenticationProvider = WebAuthenticationProvider(socialProvider, sessionService)
        return authenticationService.authenticate().providerLogin(
            hostActivity, webAuthenticationProvider
        )
    }

    /**
     * Initiate cdc phone number sign in flow.
     * This is a 2 step flow.
     * 1. call signInWithPhone to request authentication code to be sent to the phone number provided.
     * 2. login/update using the code received.
     */
    suspend fun otpSignIn(
        parameters: MutableMap<String, String>
    ): IAuthResponse = authenticationService.authenticate().otpSignIn(parameters)

    //endregion

    //region RESOLVE INTERRUPTIONS

    /**
     * Attempt to resolve "Account Pending Registration" interruption by providing the necessary
     * missing fields.
     * Note: regToken is required for authenticating the request.
     */
    suspend fun resolvePendingRegistrationWithMissingFields(
        key: String,
        serializedJsonValue: String,
        regToken: String,
    ): IAuthResponse {
        val params = mutableMapOf(key to serializedJsonValue)
        return authenticationService.resolve().pendingRegistrationWith(regToken, params)
    }

    /**
     * Attempt to resolve account linking interruption to an existing site account.
     */
    suspend fun resolveLinkToSiteAccount(
        loginId: String,
        password: String,
        authResolvable: AuthResolvable
    ): IAuthResponse {
        return authenticationService.resolve().linkSiteAccount(
            mutableMapOf("loginID" to loginId, "password" to password),
            authResolvable,
        )
    }

    /**
     * Attempt to resolve account linking interruption to an existing social account.
     */
    suspend fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        authResolvable: AuthResolvable
    ): IAuthResponse {
        return authenticationService.resolve().linkSocialAccount(
            hostActivity, authenticationProvider, authResolvable,
        )
    }

    /**
     * Attempt to resolve phone number sign in flow.
     */
    suspend fun resolveLoginWithCode(
        code: String,
        authResolvable: AuthResolvable
    ): IAuthResponse {
        return authenticationService.resolve().otpLogin(code, authResolvable)
    }

    //endregion

    //region SOCIAL PROVIDERS

    /**
     * Get reference to registered authentication provider.
     */
    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        if (!authenticationProviderMap.containsKey(name)) {
            return WebAuthenticationProvider(name, sessionService)
        }
        return authenticationProviderMap[name]
    }

    /**
     * Register new authentication provider.
     */
    private fun registerAuthenticationProvider(name: String, provider: IAuthenticationProvider) {
        authenticationProviderMap[name] = provider
    }

    fun getAuthenticatorMap() = authenticationProviderMap

    //endregion

    //region WEB BRIDGE

    /**
     * Instantiate a new WebBridgeJS element.
     */
    fun getWebBridge(): WebBridgeJS = WebBridgeJS(authenticationService)

    //endregion
}