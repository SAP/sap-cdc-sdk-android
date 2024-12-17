package com.sap.cdc.android.sdk.screensets

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_ADD_CONNECTION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_LOGOUT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.AuthenticationService
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_GMID
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.WebAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.extensions.capitalFirst
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.screensets.WebBridgeJS.Companion.ACTION_SEND_OAUTH_REQUEST
import com.sap.cdc.android.sdk.screensets.WebBridgeJS.Companion.ACTION_SEND_REQUEST
import com.sap.cdc.android.sdk.screensets.WebBridgeJSEvent.Companion.LOGIN
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.annotations.ApiStatus.Experimental
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

class WebBridgeJSApiService(
    private val weakHostActivity: WeakReference<ComponentActivity>,
    private val viewModelScope: CoroutineScope? = null,
    private val authenticationService: AuthenticationService
) {

    companion object {
        const val LOG_TAG = "CDC_WebBridgeJSApiService"
    }

    fun apiKey(): String = authenticationService.siteConfig.apiKey

    fun gmid(): String? {
        val esp =
            authenticationService.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        return esp.getString(CDC_GMID, null)
    }

    fun session(): Session? = authenticationService.session().getSession()

    /**
     * Forward result for JS evaluation.
     */
    var evaluateResult: (response: (Pair<String, String>), event: WebBridgeJSEvent?) -> Unit? =
        { _: Pair<String, String>, _: WebBridgeJSEvent? -> }

    /**
     * Set native social provider map.
     */
    var nativeSocialProviders: MutableMap<String, IAuthenticationProvider> = mutableMapOf()

    /**
     * Bridge web request handler.
     */
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
                    CDCDebuggable.log(
                        LOG_TAG,
                        "Missing provider parameter in social remove connection attempt. Flow broken"
                    )
                    evaluateResult(
                        Pair(containerId, ""),
                        WebBridgeJSEvent.canceledEvent()
                    )
                }
                sendRequest(
                    api = EP_SOCIALIZE_REMOVE_CONNECTION,
                    params = mutableMapOf("provider" to provider!!),
                    containerId
                ) {
                    // Stub.
                }
            }

            else -> {
                when (action) {
                    ACTION_SEND_REQUEST -> {
                        sendRequest(api = api, params = params, containerId, {})
                    }

                    ACTION_SEND_OAUTH_REQUEST -> {
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
        CDCDebuggable.log(LOG_TAG, "sendRequest: $api")

        launchAsync { coroutineContext ->
            val response = AuthenticationApi(
                authenticationService.coreClient,
                authenticationService.sessionService
            ).genericSend(
                api = api,
                parameters = params.toMutableMap(),
                method = HttpMethod.Post.value
            )
            if (response.isError()) {
                CDCDebuggable.log(
                    LOG_TAG,
                    "sendRequest: $api - request error: ${response.errorCode()} - ${response.errorDetails()}"
                )
                evaluateResult(
                    Pair(containerId, response.jsonResponse ?: ""),
                    WebBridgeJSEvent.errorEvent(response.serializeTo<CDCError>())
                )
                coroutineContext.cancel()
            }

            // Check if response contains a session object.
            if (responseContainsSession(response.asJson())) {
                // Set new session.
                val sessionInfo = response.serializeObject<Session>("sessionInfo")
                if (sessionInfo == null) {
                    CDCDebuggable.log(
                        LOG_TAG,
                        "sendRequest: $api - request error: failed to serialize session Info"
                    )
                    evaluateResult(
                        Pair(containerId, response.jsonResponse ?: ""),
                        WebBridgeJSEvent.canceledEvent()
                    )
                    this.coroutineContext.cancel()
                }
                authenticationService.session().setSession(sessionInfo!!)
                evaluateResult(
                    Pair(containerId, response.jsonResponse ?: ""),
                    WebBridgeJSEvent(
                        mapOf(
                            "eventName" to LOGIN, "data" to response.asJson()
                        )
                    )
                )
                coroutineContext.cancel()
            }

            //Optional completion handler.
            completion()

            // Evaluate response.
            evaluateResult(Pair(containerId, response.jsonResponse ?: ""), null)
        }
    }

    /**
     * Throttle oauth request (social) received from webSDK via the mobile adapter.
     */
    private fun sendOAuthRequest(
        api: String,
        params: Map<String, String>,
        containerId: String,
        completion: () -> Unit
    ) {
        if (weakHostActivity.get() == null) {
            // Fail with error.
            CDCDebuggable.log(LOG_TAG, "Context host error. Flow broken")
            evaluateResult(
                Pair(containerId, ""),
                WebBridgeJSEvent.canceledEvent()
            )
        }
        launchAsync { coroutineContext ->
            val provider = params["provider"]
            if (provider == null) {
                // Fail with error.
                CDCDebuggable.log(LOG_TAG, "Missing provider parameter in social login attempt. Flow broken")
                evaluateResult(
                    Pair(containerId, ""),
                    WebBridgeJSEvent.canceledEvent()
                )
                coroutineContext.cancel()
            }

            // Vary selected authentication provider. If no native provider is available, the
            // service will create a new instance of the WebAuthenticationProvider.
            var authProvider = getNativeProviderAuthenticator(provider!!)
            if (authProvider == null) {
                authProvider = WebAuthenticationProvider(
                    socialProvider = provider,
                    siteConfig = authenticationService.siteConfig,
                    session = authenticationService.session().getSession()
                )
            }

            // Initiate provider login flow.
            val authResponse = authenticationService.authenticate().providerSignIn(
                weakHostActivity.get()!!, authProvider, params.toMutableMap()
            )
            if (authResponse.cdcResponse().isError()) {
                // Fail with error.
                CDCDebuggable.log(
                    LOG_TAG,
                    "sendRequest: $api - request error: ${
                        authResponse.cdcResponse().errorCode()
                    } - ${authResponse.cdcResponse().errorDetails()}"
                )
                evaluateResult(
                    Pair(containerId, authResponse.cdcResponse().jsonResponse ?: ""),
                    WebBridgeJSEvent.errorEvent(authResponse.cdcResponse().serializeTo<CDCError>())
                )
                coroutineContext.cancel()
            }

            //Optional completion handler.
            completion()

            // Evaluate response.
            evaluateResult(Pair(containerId, authResponse.cdcResponse().asJson() ?: ""), null)
        }
    }

    /**
     * Get native provider class if a native provider is used for social login.
     * 1. Try to fetch class from pre-set provider map.
     * 2. Try to instantiate the class if not present in the provider map.
     */
    private fun getNativeProviderAuthenticator(provider: String): IAuthenticationProvider? {
        if (nativeSocialProviders.isEmpty() || !nativeSocialProviders.containsKey(provider))
            return null
        return nativeSocialProviders[provider]
    }

    /**
     * Instantiate new native provider class according to the naming template:
     * "${capitalized name}AuthenticatorProvider" String.
     * Will use Java reflection to instantiate new instance.
     */
    @Experimental
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
     * It is preferred to launch asynchronous code within a view model scope so that the coroutine lifecycle
     * will be aligned with the view models. If a viewmodel scope was not registered with this instance of the
     * web bridge then a new scope will be created for this block run.
     */
    private fun launchAsync(asyncBlock: suspend CoroutineScope.(CoroutineContext) -> Unit) {
        if (viewModelScope != null) {
            viewModelScope.launch(Dispatchers.IO) {
                asyncBlock(this.coroutineContext)
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                asyncBlock(this.coroutineContext)
            }
        }
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