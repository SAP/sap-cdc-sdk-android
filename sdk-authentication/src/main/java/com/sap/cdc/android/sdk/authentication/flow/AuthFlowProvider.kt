package com.sap.cdc.android.sdk.authentication.flow

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.authentication.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.ProviderException
import com.sap.cdc.android.sdk.authentication.provider.ProviderType
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.api.Api
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class ProviderAuthFow(
    sessionService: SessionService,
    private val provider: IAuthenticationProvider,
    private val weakActivity: WeakReference<ComponentActivity>
) : AuthFlow(sessionService) {

    companion object {
        const val LOG_TAG = "CDC_ProviderAuthFow"
    }

    override suspend fun authenticate(): IAuthResponse {
        try {
            val result: AuthenticatorProviderResult = provider.providerSignIn(weakActivity.get())
            parameters["loginMode"] = "standard"
            parameters["provider"] = result.provider

            when (result.type) {
                ProviderType.NATIVE -> {
                    parameters["providerSessions"] = result.providerSessions!!
                    val notifyResponse =
                        Api(sessionService).genericSend(EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN, parameters)
                    dispose()
                    return response.withAuthenticationData(notifyResponse.asJson()!!)
                }

                ProviderType.WEB -> {
                    // Set new acquired session.
                    val session = result.session!!
                    sessionService.sessionSecure.setSession(session)

                    // Refresh account information for flow response.
                    val accountResponse =
                        Api(sessionService).genericSend(
                            EP_ACCOUNTS_GET_ACCOUNT_INFO,
                            parameters
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

    override fun dispose() {
        weakActivity.clear()
    }

    suspend fun link() {

    }
}