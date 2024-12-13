package com.sap.cdc.android.sdk.auth.flow

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.util.ProviderException
import com.sap.cdc.android.sdk.auth.provider.ProviderType
import com.sap.cdc.android.sdk.auth.provider.SSOAuthenticationData
import com.sap.cdc.android.sdk.auth.provider.util.SSOUtil
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.CDCRequest
import com.sap.cdc.android.sdk.core.api.CDCResponse
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class ProviderAuthFow(
    coreClient: CoreClient,
    sessionService: SessionService,
    private val provider: IAuthenticationProvider? = null,
    private val weakActivity: WeakReference<ComponentActivity>? = null
) : AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "CDC_ProviderAuthFow"
    }

    suspend fun signIn(): IAuthResponse {
        if (provider == null)
            return AuthResponse(CDCResponse().providerError())
        try {
            val result: AuthenticatorProviderResult = provider.signIn(weakActivity?.get())

            when (result.type) {
                ProviderType.NATIVE -> {
                    if (!parameters.containsKey("loginMode")) {
                        parameters["loginMode"] = "standard"
                    }
                    parameters["provider"] = result.provider
                    parameters["providerSessions"] = result.providerSessions!!
                    parameters["conflictHandling"] = "fail"
                    val notifyResponse =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                            parameters
                        )
                    if (!notifyResponse.isError()) {
                        secureNewSession(notifyResponse)
                    }
                    dispose()
                    val authResponse = AuthResponse(notifyResponse)
                    initResolvableState(authResponse)
                    return authResponse
                }

                ProviderType.WEB -> {
                    // Secure new acquired session.
                    val session = result.session!!
                    sessionService.setSession(session)

                    // Refresh account information for flow response.
                    val accountResponse =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_GET_ACCOUNT_INFO,
                            mutableMapOf("include" to "data,profile,emails")
                        )
                    if (!accountResponse.isError()) {
                        secureNewSession(accountResponse)
                    }
                    dispose()
                    return AuthResponse(accountResponse)
                }

                ProviderType.SSO -> {
                    val ssoData = result.ssoData!!
                    val ssoUtil = SSOUtil()
                    val tokenResponse = onSSOCodeReceived(ssoUtil, ssoData)
                    if (tokenResponse.containsKey("access_token")) {
                        // parse session info.

                        // Refresh account information for flow response.
                        val accountResponse =
                            AuthenticationApi(coreClient, sessionService).genericSend(
                                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                                mutableMapOf("include" to "data,profile,emails")
                            )
                        if (!accountResponse.isError()) {
                            secureNewSession(accountResponse)
                        }
                        dispose()
                        return AuthResponse(accountResponse)
                    } else {
                        return AuthResponse(CDCResponse().providerError())
                    }
                }
            }
        } catch (exception: ProviderException) {
            //TODO: Generify provider exception or remove it.
            Log.d(LOG_TAG, exception.type.ordinal.toString())
            return AuthResponse(CDCResponse().providerError())
        }
    }

    suspend fun removeConnection(provider: String): CDCResponse =
        AuthenticationApi(coreClient, sessionService).genericSend(
            EP_SOCIALIZE_REMOVE_CONNECTION, mutableMapOf("provider" to provider)
        )

    override fun dispose() {
        weakActivity?.clear()
    }

    private suspend fun onSSOCodeReceived(
        ssoUtil: SSOUtil,
        data: SSOAuthenticationData
    ): CDCResponse {
        val headers = hashMapOf(
            "apikey" to sessionService.siteConfig.apiKey
        )

        val serverParams = mutableMapOf<String, String>()
        serverParams["redirect_uri"] = data.redirectUri!!
        serverParams["client_id"] = sessionService.siteConfig.apiKey
        serverParams["grant_type"] = "authorization_code"
        serverParams["code"] = data.code!!
        serverParams["code_verifier"] = data.verifier!!
        val urlString = ssoUtil.getUrl(sessionService.siteConfig, SSOUtil.TOKEN)
        return Api(coreClient).post(
            CDCRequest(siteConfig = sessionService.siteConfig)
                .api(urlString)
                .parameters(serverParams)
                .headers(headers)
        )

    }
}