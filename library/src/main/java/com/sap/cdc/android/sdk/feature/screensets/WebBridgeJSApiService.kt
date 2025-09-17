package com.sap.cdc.android.sdk.feature.screensets

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.extensions.capitalFirst
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthEndpoints
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_SOCIALIZE_ADD_CONNECTION
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_SOCIALIZE_LOGOUT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.AuthenticationService
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_GMID
import com.sap.cdc.android.sdk.feature.ResolvableContext
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS.Companion.ACTION_SEND_OAUTH_REQUEST
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS.Companion.ACTION_SEND_REQUEST
import com.sap.cdc.android.sdk.feature.session.Session
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
        const val LOG_TAG = "WebBridgeJSApiService"
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
    var evaluateJSResult: (WebBridgeJSEvaluation) -> Unit? =
        { _: WebBridgeJSEvaluation -> }

    /**
     * Set native social provider map.
     */
    var nativeSocialProviders: MutableMap<String, IAuthenticationProvider> = mutableMapOf()

    /**
     * Interruption coordinator instance. Used to handle complex flows that require multiple steps.
     */
    var interruptionCoordinator: WebBridgeJSInterruptionCoordinator =
        WebBridgeJSInterruptionCoordinator(authenticationService)

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
                    authenticationService.sessionService.invalidateSession()
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
                    evaluateJSResult(
                        WebBridgeJSEvaluation(
                            containerID = containerId,
                            evaluationString = "",
                            event = WebBridgeJSEvent.canceledEvent()
                        )
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
            ).send(
                api = api,
                parameters = params.toMutableMap(),
                method = HttpMethod.Post.value
            )
            if (response.isError()) {
                CDCDebuggable.log(
                    LOG_TAG,
                    "sendRequest: $api - request error: ${response.errorCode()} - ${response.errorDetails()}"
                )

                // Evaluate interruption if required.
                interruptionCoordinator.evaluateError(response.errorCode()!!)

                evaluateJSResult(
                    WebBridgeJSEvaluation(
                        containerID = containerId,
                        evaluationString = response.jsonResponse ?: "",
                        event = WebBridgeJSEvent.errorEvent(response.serializeTo<CDCError>())
                    )
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
                    evaluateJSResult(
                        WebBridgeJSEvaluation(
                            containerID = containerId,
                            evaluationString = response.jsonResponse ?: "",
                            event = WebBridgeJSEvent.canceledEvent()
                        )
                    )
                    this.coroutineContext.cancel()
                }
                authenticationService.session().setSession(sessionInfo!!)

                // Evaluate interruption if required.
                interruptionCoordinator.evaluateSuccess(
                    api = api,
                    params = params,
                    containerId = containerId,
                    evaluateJsResult = { evaluateJSResult(it) }
                )

                // Evaluate JS result with no event.
                evaluateJSResult(
                    WebBridgeJSEvaluation(
                        containerID = containerId,
                        evaluationString = response.jsonResponse ?: "",
                        event = null
                    )
                )
                coroutineContext.cancel()
            }

            //Optional completion handler.
            completion()

            // Evaluate response.
            evaluateJSResult(
                WebBridgeJSEvaluation(
                    containerID = containerId,
                    evaluationString = response.jsonResponse ?: "",
                    event = if (api == EP_ACCOUNTS_LOGOUT) WebBridgeJSEvent.logoutEvent() else null
                )
            )
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
            evaluateJSResult(
                WebBridgeJSEvaluation(
                    containerID = containerId,
                    evaluationString = "",
                    event = WebBridgeJSEvent.canceledEvent()
                )
            )
        }
        launchAsync { coroutineContext ->
            val provider = params["provider"]
            if (provider == null) {
                // Fail with error.
                CDCDebuggable.log(LOG_TAG, "Missing provider parameter in social login attempt. Flow broken")
                evaluateJSResult(
                    WebBridgeJSEvaluation(
                        containerID = containerId,
                        evaluationString = "",
                        event = WebBridgeJSEvent.canceledEvent()
                    )
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
            authenticationService.authenticate().provider().signIn(
                weakHostActivity.get()!!, authProvider, params.toMutableMap()
            ) {
                onError = { error ->
                    // Fail with error.
                    CDCDebuggable.log(
                        LOG_TAG,
                        "sendRequest: $api - request error: ${
                            error.code
                        } - ${error.message}"
                    )

                    // Evaluate interruption if required.
                    interruptionCoordinator.evaluateError(error.code!!.toInt())


                    evaluateJSResult(
                        WebBridgeJSEvaluation(
                            containerID = containerId,
                            evaluationString = error.asJson!!,
                            event = WebBridgeJSEvent.errorEvent(CDCError.fromJson(error.asJson))
                        )
                    )
                    coroutineContext.cancel()
                }

                onSuccess = { authResponse ->
                    //Optional completion handler.
                    completion()


                    // Specific evaluation is required by WebSDK for oAuthRequest.
                    val evaluationString =
                        "{\"errorCode\":" + authResponse.userData["errorCode"] + ",\"userInfo\":" + authResponse.jsonData + "}"

                    // Evaluate interruption if required.
                    launchAsync {
                        interruptionCoordinator.evaluateSuccess(
                            api = api,
                            params = params,
                            containerId = containerId,
                            evaluateJsResult = { evaluateJSResult(it) }
                        )
                    }

                    // Evaluate response.
                    evaluateJSResult(
                        WebBridgeJSEvaluation(
                            containerID = containerId,
                            evaluationString = evaluationString,
                            event = null
                        )
                    )
                }
            }
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

/**
 * Coordinator to handle interruption complex flows.
 */
class WebBridgeJSInterruptionCoordinator(
    val authenticationService: AuthenticationService
) {

    var activeInterruption: WebBridgeInterruption? = null
        private set


    suspend fun evaluateSuccess(
        api: String,
        params: Map<String, String>,
        containerId: String,
        evaluateJsResult: (WebBridgeJSEvaluation) -> Unit = { _ -> }
    ) {
        if (activeInterruption == null) {
            evaluateJsResult(
                WebBridgeJSEvaluation(
                    containerID = containerId,
                    evaluationString = "",
                    event = WebBridgeJSEvent.loginEvent()
                )
            )
            return
        }
        activeInterruption!!.resolve(
            api = api,
            params = params,
            containerId = containerId,
            evaluateJsResult = evaluateJsResult,
            completion = {
                activeInterruption = null
            }
        )
    }

    fun evaluateError(errorCode: Int) {
        when (errorCode) {
            ResolvableContext.ERR_ENTITY_EXIST_CONFLICT -> {
                // Handle conflict error.
                activeInterruption = WebBridgeJSLinkingInterruption(authenticationService, dispose = {
                    dispose()
                })
            }
        }
    }

    fun evaluateEvent(event: WebBridgeJSEvent): Boolean {
        if (event.content?.get("eventName") == WebBridgeJSEvent.HIDE) {
            if (activeInterruption == null) return false
            return activeInterruption!!.isActive()
        }
        return false
    }

    fun dispose() {
        activeInterruption = null
    }

}


/**
 * Coordinator to handle link account interruption complex flow.
 */
interface WebBridgeInterruption {

    fun isActive(): Boolean

    suspend fun resolve(
        api: String,
        params: Map<String, String>,
        containerId: String,
        evaluateJsResult: (WebBridgeJSEvaluation) -> Unit = { _ -> },
        completion: () -> Unit,
    )

}

class WebBridgeJSLinkingInterruption(
    val authenticationService: AuthenticationService,
    val dispose: () -> Unit
) : WebBridgeInterruption {

    private var _active = false

    override fun isActive(): Boolean = _active
    override suspend fun resolve(
        api: String,
        params: Map<String, String>,
        containerId: String,
        evaluateJsResult: (WebBridgeJSEvaluation) -> Unit,
        completion: () -> Unit,
    ) {
        if (api == AuthEndpoints.EP_ACCOUNTS_FINALIZE_REGISTRATION) {
            notifyAccount(
                api = api,
                params = params,
                containerId = containerId,
                evaluateJsResult = evaluateJsResult,
                completion = completion
            )
            return
        }
        if (params["loginMode"] == "connect") {
            notifyAccount(
                api = api,
                params = params,
                containerId = containerId,
                evaluateJsResult = evaluateJsResult,
                completion = completion
            )
        }
    }

    private suspend fun notifyAccount(
        api: String,
        params: Map<String, String>,
        containerId: String,
        evaluateJsResult: (WebBridgeJSEvaluation) -> Unit,
        completion: () -> Unit,
    ) {
        val response = AuthenticationApi(
            authenticationService.coreClient,
            authenticationService.sessionService
        ).send(
            api = api,
            parameters = params.toMutableMap(),
            method = HttpMethod.Post.value
        )
        if (response.isError()) {
            CDCDebuggable.log(
                WebBridgeJSApiService.LOG_TAG,
                "notifyAccount: $api - request error: ${response.errorCode()} - ${response.errorDetails()}"
            )
            evaluateJsResult(
                WebBridgeJSEvaluation(
                    containerID = containerId,
                    evaluationString = response.jsonResponse ?: "",
                    event = WebBridgeJSEvent.errorEvent(CDCError.fromJson(json = response.jsonResponse!!))
                )
            )
            completion()
            _active = false
            dispose()
            return
        }
        evaluateJsResult(
            WebBridgeJSEvaluation(
                containerID = containerId,
                evaluationString = response.jsonResponse ?: "",
                event = WebBridgeJSEvent.loginEvent()
            )
        )
        _active = false
        dispose()
        completion()
    }


}