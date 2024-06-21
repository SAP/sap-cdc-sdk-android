package com.sap.cdc.android.sdk.example.cdc

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.AuthenticationService
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.WebAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.session.Session
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS

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
    private var authenticationService = AuthenticationService(sessionService)

    init {
        val v6Migrator = V6SessionMigrator(context)
        // v6 -> tryMigrateSession (needs identityService as injection)
        if (v6Migrator.sessionAvailableForMigration()) {
            v6Migrator.getSession(
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

    //endregion

    //region AUTHENTICATION FLOWS

    /**
     * Initiate cdc SDK credentials registration.
     */
    suspend fun register(email: String, password: String): IAuthResponse {
        val params = mutableMapOf("email" to email, "password" to password)
        return authenticationService.authenticate().register(params)
    }

    /**
     * Initiate cdc SDL credentials login.
     */
    suspend fun login(email: String, password: String): IAuthResponse {
        val params = mutableMapOf("email" to email, "password" to password)
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

    //endregion

    //region WEB BRIDGE

    /**
     * Instantiate a new WebBridgeJS element.
     */
    fun getWebBridge(): WebBridgeJS = WebBridgeJS(authenticationService)

    //endregion
}