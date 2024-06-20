package com.sap.cdc.android.sdk.sceensets

import android.util.Log
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_ADD_CONNECTION
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_LOGOUT
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.authentication.AuthenticationApi
import com.sap.cdc.android.sdk.authentication.AuthenticationService
import com.sap.cdc.android.sdk.authentication.session.Session
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS.Companion.LOG_TAG
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

class WebBridgeJSApiService(private val authenticationService: AuthenticationService) {

    fun apiKey(): String = authenticationService.sessionService.siteConfig.apiKey

    fun gmid(): String? = authenticationService.sessionService.gmidLatest()

    fun session(): Session? = authenticationService.sessionService.sessionSecure.getSession()

    var evaluateResult: (response: String) -> Unit? = { }

    fun onRequest(
        action: String,
        api: String,
        params: Map<String, String>,
    ) {
        when (api) {
            EP_ACCOUNTS_LOGOUT, EP_SOCIALIZE_LOGOUT -> {

            }

            EP_SOCIALIZE_ADD_CONNECTION -> {

            }

            EP_SOCIALIZE_REMOVE_CONNECTION -> {

            }

            else -> {
                when (action) {
                    "send_request" -> {
                        sendRequest(api = api, params = params)
                    }

                    "send_oauth_request" -> {

                    }
                }
            }
        }
    }

    /**
     * Throttle request received from webSDK via the mobile adapter.
     * Json response will be evaluated back to the WebSDK (success & error).
     */
    private fun sendRequest(api: String, params: Map<String, String>) {
        Log.d(LOG_TAG, "sendRequest: $api")
        CoroutineScope(Dispatchers.IO).launch {
            val response = AuthenticationApi(
                authenticationService.coreClient,
                authenticationService.sessionService
            ).genericSend(
                api = api,
                parameters = params.toMutableMap(),
                method = HttpMethod.Post.value
            )
            if (response.isError()) {
                Log.d(
                    LOG_TAG,
                    "sendRequest: $api - request error: ${response.errorCode()} - ${response.errorDetails()}"
                )
            }
            evaluateResult(response.jsonResponse ?: "")
        }
    }

}