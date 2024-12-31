package com.sap.cdc.android.sdk.auth.flow

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.model.SSOResponseEntity
import com.sap.cdc.android.sdk.auth.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.ProviderType
import com.sap.cdc.android.sdk.auth.provider.SSOAuthenticationData
import com.sap.cdc.android.sdk.auth.provider.util.ProviderException
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

    suspend fun signIn(parameters: MutableMap<String, String>): IAuthResponse {
        if (provider == null)
            return AuthResponse(CDCResponse().providerError())
        try {
            val signIn: AuthenticatorProviderResult = provider.signIn(weakActivity?.get())

            // Vary flow based on provider type.
            when (signIn.type) {
                // Native flows refer to social networks that require native SDK implementation
                // in order to authenticate the user (eg. Facebook, Google, etc.).
                ProviderType.NATIVE -> {
                    if (!parameters.containsKey("loginMode")) {
                        parameters["loginMode"] = "standard"
                    }
                    parameters["provider"] = signIn.provider
                    parameters["providerSessions"] = signIn.providerSessions!!
                    parameters["conflictHandling"] = "fail"
                    val notifySocialLogin =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                            parameters
                        )

                    // Prepare flow response
                    val authResponse = AuthResponse(notifySocialLogin)
                    val resolvableContext = initResolvableState(notifySocialLogin)
                    if (resolvableContext == null) {
                        // No interruption in flow - secure the session - flow is successful.
                        secureNewSession(notifySocialLogin)
                        dispose()
                        return authResponse
                    }

                    // Flow ends with resolvable interruption.
                    authResponse.resolvableContext = resolvableContext
                    dispose()
                    return authResponse
                }

                // Web flows refer to all social provider types that are not native SDK based.
                // These providers require a web view to authenticate the user.
                ProviderType.WEB -> {
                    //TODO: Possibility missing interruption or error handling.
                    // Secure new acquired session.
                    val session = signIn.session!!
                    // Session will be secured when set.
                    sessionService.setSession(session)

                    // Refresh account information for flow response.
                    val getAccountInfo =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_GET_ACCOUNT_INFO,
                            mutableMapOf("include" to "data,profile,emails")
                        )

                    if (!getAccountInfo.isError()) {
                        // Secure acquired session from account info.
                        secureNewSession(getAccountInfo)
                    }

                    dispose()
                    return AuthResponse(getAccountInfo)
                }

                // SSO provider authentication using a Central Login Page (CLP) only.
                ProviderType.SSO -> {
                    val ssoData = signIn.ssoData!!
                    val ssoUtil = SSOUtil()
                    val tokenResponse = ssoToken(ssoUtil, ssoData)
                    if (tokenResponse.containsKey("access_token")) {
                        // parse session info.
                        val ssoResponseEntity =
                            tokenResponse.serializeTo<SSOResponseEntity>() ?: return AuthResponse(
                                CDCResponse().providerError()
                            )
                        val session = ssoUtil.parseSessionInfo(ssoResponseEntity)
                        sessionService.setSession(session)

                        // Refresh account information for flow response.
                        val getAccountInfo =
                            AuthenticationApi(coreClient, sessionService).genericSend(
                                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                                mutableMapOf("include" to "data,profile,emails")
                            )
                        if (!getAccountInfo.isError()) {
                            // Secure acquired session from account info.
                            secureNewSession(getAccountInfo)
                        }
                        dispose()
                        return AuthResponse(getAccountInfo)
                    } else {
                        return AuthResponse(CDCResponse().providerError())
                    }
                }
            }
        } catch (exception: ProviderException) {
            Log.d(LOG_TAG, exception.type.ordinal.toString())
            return AuthResponse(CDCResponse().fromError(exception.error!!))
        }
    }

    suspend fun removeConnection(provider: String): CDCResponse =
        AuthenticationApi(coreClient, sessionService).genericSend(
            EP_SOCIALIZE_REMOVE_CONNECTION, mutableMapOf("provider" to provider)
        )

    override fun dispose() {
        weakActivity?.clear()
    }

    private suspend fun ssoToken(
        ssoUtil: SSOUtil,
        data: SSOAuthenticationData
    ): CDCResponse {
        val headers = hashMapOf(
            "apikey" to sessionService.siteConfig.apiKey
        )

        val parameters = mutableMapOf<String, String>()
        parameters["redirect_uri"] = data.redirectUri!!
        parameters["client_id"] = sessionService.siteConfig.apiKey
        parameters["grant_type"] = "authorization_code"
        parameters["code"] = data.code!!
        parameters["code_verifier"] = data.verifier!!
        val urlString = ssoUtil.getUrl(sessionService.siteConfig, SSOUtil.TOKEN)
        return Api(coreClient).post(
            CDCRequest(siteConfig = sessionService.siteConfig)
                .api(urlString)
                .parameters(parameters)
                .headers(headers)
        )

    }

}