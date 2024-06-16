package com.sap.cdc.android.sdk.example.cdc

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.AuthenticationService
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.WebAuthenticationProvider
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.SiteConfig
import com.sap.cdc.android.sdk.session.api.Api
import com.sap.cdc.android.sdk.session.api.CDCResponse
import com.sap.cdc.android.sdk.session.session.Session
import io.ktor.http.HttpMethod

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
    private var sessionService = SessionService(SiteConfig(context)).newClient()

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

    fun getConfig() = sessionService.siteConfig

    //endregion

    //region SESSION MANAGEMENT

    fun setSession(session: Session) {
        sessionService.sessionSecure.setSession(session)
    }

    fun getSession(): Session? {
        return sessionService.sessionSecure.getSession()
    }

    //endregion

    //region AUTHENTICATION FLOWS

    suspend fun register(email: String, password: String): IAuthResponse {
        val params = mutableMapOf("email" to email, "password" to password)
        return authenticationService.apis().register(params)
    }

    suspend fun getAccountInfo(): IAuthResponse {
        return authenticationService.apis().getAccountInfo()
    }

    suspend fun nativeSocialSignIn(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider
    ): IAuthResponse {
        return authenticationService.apis().providerLogin(
            hostActivity, provider
        )
    }

    suspend fun webSocialSignIn(
        hostActivity: ComponentActivity,
        socialProvider: String
    ) : IAuthResponse {
        val webAuthenticationProvider = WebAuthenticationProvider(socialProvider, sessionService)
        return authenticationService.apis().providerLogin(
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

    //region GENERIC

    /**
     * Identity cloud generic send request.
     */
    suspend fun send(
        api: String,
        parameters: MutableMap<String, String>,
        method: String? = HttpMethod.Post.value
    ): CDCResponse {
        return Api(sessionService).genericSend(
            api,
            parameters,
            method
        )
    }

    //endregion
}