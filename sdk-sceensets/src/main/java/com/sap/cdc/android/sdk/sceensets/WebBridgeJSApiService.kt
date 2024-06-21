package com.sap.cdc.android.sdk.sceensets

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_ADD_CONNECTION
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_LOGOUT
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.authentication.AuthenticationApi
import com.sap.cdc.android.sdk.authentication.AuthenticationService
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.WebAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.session.Session
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS.Companion.LOG_TAG
import com.sap.cdc.android.sdk.sceensets.extensions.capitalFirst
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.lang.ref.WeakReference


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

class WebBridgeJSApiService(
    private val weakHostActivity: WeakReference<ComponentActivity>,
    private val authenticationService: AuthenticationService
) {

    companion object {
        const val LOG_TAG = "CDC_WebBridgeJSApiService"
    }

    fun apiKey(): String = authenticationService.sessionService.siteConfig.apiKey

    fun gmid(): String? = authenticationService.sessionService.gmidLatest()

    fun session(): Session? = authenticationService.sessionService.sessionSecure.getSession()

    var evaluateResult: (response: (Pair<String, String>)) -> Unit? = { }

    var nativeSocialProviders: MutableMap<String, IAuthenticationProvider> = mutableMapOf()

    fun onRequest(
        action: String,
        api: String,
        params: Map<String, String>,
        containerId: String
    ) {
        when (api) {
            EP_ACCOUNTS_LOGOUT, EP_SOCIALIZE_LOGOUT -> {
                sendRequest(
                    api =
                    EP_ACCOUNTS_LOGOUT,
                    params = params,
                    containerId
                ) {
                    authenticationService.sessionService.clearSession()
                }
            }

            EP_SOCIALIZE_ADD_CONNECTION -> {
                val parameters = params.toMutableMap()
                parameters["loginMode"] = "connect"
                sendOAuthRequest(api = api, params = params, containerId, {})
            }

            EP_SOCIALIZE_REMOVE_CONNECTION -> {
                val provider = params["provider"]
                if (provider == null) {
                    //TODO: throttle error.
                }
                sendRequest(
                    api = EP_SOCIALIZE_REMOVE_CONNECTION,
                    params = mutableMapOf("provider" to provider!!),
                    containerId,
                    {})
            }

            else -> {
                when (action) {
                    "send_request" -> {
                        sendRequest(api = api, params = params, containerId, {})
                    }

                    "send_oauth_request" -> {
                        sendOAuthRequest(api = api, params = params, containerId, {})
                    }
                }
            }
        }
    }

    /**
     * Throttle request received from webSDK via the mobile adapter.
     * Json response will be evaluated back to the WebSDK (success & error).
     */
    private fun sendRequest(
        api: String,
        params: Map<String, String>,
        containerId: String,
        completion: () -> Unit
    ) {
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

            // Check if response contains a session object.
            if (responseContainsSession(response.asJson())) {
                // Set new session.
                authenticationService.sessionService.setSession(response.asJson()!!)
                //TODO: Throttle event login.
            }

            //Optional completion handler.
            completion()

            // Evaluate response.
            evaluateResult(Pair(containerId, response.jsonResponse ?: ""))
        }
    }

    private fun sendOAuthRequest(
        api: String,
        params: Map<String, String>,
        containerId: String,
        completion: () -> Unit
    ) {
        if (weakHostActivity.get() == null) {
            // Fail with error.
            Log.d(LOG_TAG, "Context host error. Flow broken")
        }
        CoroutineScope(Dispatchers.IO).launch {
            val provider = params["provider"]
            if (provider == null) {
                // Fail with error.
                Log.d(LOG_TAG, "Missing provider parameter in social login attempt. Flow broken")
                //TODO: throttle error
            }

            // Vary selected authentication provider. If no native provider is available, the
            // service will create a new instance of the WebAuthenticationProvider.
            var authProvider = getNativeProviderAuthenticator(provider!!)
            if (authProvider == null) {
                authProvider = WebAuthenticationProvider(
                    socialProvider = provider,
                    sessionService = authenticationService.sessionService
                )
            }

            val authResponse = authenticationService.authenticate().providerLogin(
                weakHostActivity.get()!!, authProvider, params.toMutableMap()
            )
            if (authResponse.authenticationError() != null) {
                // Fail with error.
                //TODO: throttle error
            }

            //Optional completion handler.
            completion()

            // Evaluate response.
            evaluateResult(Pair(containerId, authResponse.authenticationJson() ?: ""))
        }
    }

    /**
     * Get native provider class if a native provider is used for social login.
     * 1. Try to fetch class from pre-set provider map.
     * 2. Try to instantiate the class if not present in the provider map.
     */
    private fun getNativeProviderAuthenticator(provider: String): IAuthenticationProvider? {
        if (nativeSocialProviders.isEmpty() || !nativeSocialProviders.containsKey(provider))
            return getNewNativeProviderInstance(provider)
        return nativeSocialProviders[provider]
    }

    /**
     * Instantiate new native provider class according to the naming template:
     * "${capitalized name}AuthenticatorProvider" String.
     * Will use Java reflection to instantiate new instance.
     */
    private fun getNewNativeProviderInstance(provider: String): IAuthenticationProvider? {
        try {
            val context = authenticationService.sessionService.siteConfig.applicationContext
            val kClass = Class.forName("${provider.capitalFirst()}AuthenticationProvider").kotlin
            return kClass.java.getDeclaredConstructor().newInstance() as IAuthenticationProvider?
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    /**
     * Check if api response contains a session required fo authentication flows.
     */
    private fun responseContainsSession(json: String?): Boolean {
        if (json == null) return false
        val jsonObject = Json.parseToJsonElement(json).jsonObject
        return jsonObject.containsKey("sessionInfo")
    }

    /**
     * Dispose of service saved references
     */
    fun dispose() {
        weakHostActivity.clear()
    }
}