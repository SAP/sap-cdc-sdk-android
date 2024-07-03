package com.sap.cdc.android.sdk.authentication.flow

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.authentication.AuthenticationApi
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.authentication.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.ProviderException
import com.sap.cdc.android.sdk.authentication.provider.ProviderType
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.model.CDCError
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

    override suspend fun authenticate(): IAuthResponse {
        if (provider == null)
            return response.failedAuthenticationWith(CDCError.providerError())
        try {
            val result: AuthenticatorProviderResult = provider.providerSignIn(weakActivity?.get())
            parameters["loginMode"] = "standard"
            parameters["provider"] = result.provider

            when (result.type) {
                ProviderType.NATIVE -> {
                    parameters["providerSessions"] = result.providerSessions!!
                    val notifyResponse =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                            parameters
                        )
                    if (notifyResponse.isError()) {
                        val error = notifyResponse.toCDCError()
                        Log.d(LOG_TAG, error.errorDetails ?: "")
                        response.failedAuthenticationWith(error)
                    }
                    dispose()
                    return response.withAuthenticationData(notifyResponse.asJson()!!)
                }

                ProviderType.WEB -> {
                    // Secure new acquired session.
                    val session = result.session!!
                    sessionService.sessionSecure.setSession(session)

                    // Refresh account information for flow response.
                    val accountResponse =
                        AuthenticationApi(coreClient, sessionService).genericSend(
                            EP_ACCOUNTS_GET_ACCOUNT_INFO,
                            mutableMapOf("include" to "data,profile,emails")
                        )
                    dispose()
                    return response.withAuthenticationData(accountResponse.asJson()!!)
                }
            }
        } catch (exception: ProviderException) {
            Log.d(LOG_TAG, exception.type.ordinal.toString())
            return response.failedAuthenticationWith(exception.error!!)
        }
    }

    suspend fun removeConnection(provider: String): CDCResponse =
        AuthenticationApi(coreClient, sessionService).genericSend(
            EP_SOCIALIZE_REMOVE_CONNECTION, mutableMapOf("provider" to provider)
        )


    override fun dispose() {
        weakActivity?.clear()
    }

    suspend fun link() {

    }
}