package com.sap.cdc.android.sdk.feature.screensets

import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.extensions.parseQueryStringParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 13/06/2024
 * Copyright: SAP LTD.
 */
class WebBridgeJS(private val authenticationService: AuthenticationService) {

    companion object {
        const val LOG_TAG = "WebBridgeJS"

        const val URI_REDIRECT_SCHEME = "gsapi"

        const val JS_NAME = "__gigAPIAdapterSettings"
        const val JS_ADAPTER_NAME = "mobile"
        const val JS_EVALUATE = "gigya._.apiAdapters.mobile.mobileCallbacks"

        const val BASE_URL: String = "https://www.gigya.com"
        const val MIME_TYPE: String = "text/html"
        const val ENCODING: String = "utf-8"

        const val ACTION_GET_IDS = "get_ids"
        const val ACTION_IS_SESSION_VALID = "is_session_valid"
        const val ACTION_SEND_REQUEST = "send_request"
        const val ACTION_SEND_OAUTH_REQUEST = "send_oauth_request"
        const val ACTION_ON_PLUGIN_EVENT = "on_plugin_event"

    }

    private var bridgedWebView: WebView? = null
    private var bridgeEvents: ((WebBridgeJSEvent) -> Unit?)? = null
    private lateinit var bridgedApiService: WebBridgeJSApiService
    private var webBridgeJSConfig: WebBridgeJSConfig? = null

    /**
     * Trigger URL load for given bridged web view.
     * Helper method to avoid missing one of the following static parameters.
     */
    fun load(webView: WebView, url: String) {
        webView.loadDataWithBaseURL(
            BASE_URL,
            url,
            MIME_TYPE,
            ENCODING,
            null
        )
    }

    /**
     * Add specific web bridge configurations.
     */
    fun addConfig(webBridgeJSConfig: WebBridgeJSConfig) {
        this.webBridgeJSConfig = webBridgeJSConfig
    }

    /**
     * Register bridge for event forwarding.
     */
    fun registerForEvents(events: (WebBridgeJSEvent) -> Unit) {
        bridgeEvents = events
    }

    /**
     * Stream events forward.
     */
    fun streamEvent(event: WebBridgeJSEvent) = bridgeEvents?.invoke(event)

    /**
     * Set the native social providers.
     * Use provide lowercased name as key & the authenticator instance for the value.
     */
    fun setNativeSocialProviders(
        providerMap: MutableMap<String,
                IAuthenticationProvider>
    ) {
        bridgedApiService.nativeSocialProviders = providerMap
    }

    /**
     * Attach JS bridge to given WebView widget.
     */
    fun attachBridgeTo(webView: WebView, viewModelScope: CoroutineScope? = null) {
        bridgedWebView = webView
        bridgedApiService = WebBridgeJSApiService(
            weakHostActivity = WeakReference(webView.context as ComponentActivity?),
            authenticationService = authenticationService
        )
        bridgedWebView!!.addJavascriptInterface(
            ScreenSetsJavaScriptInterface(
                bridgedApiService.apiKey()
            ),
            JS_NAME
        )
        bridgedApiService.evaluateResult = { response, event ->
            val containerID = response.first
            val evaluationString = response.second
            if (evaluationString.isNotEmpty()) {
                evaluateJS(containerID, evaluationString)
            }
            if (event != null) {
                streamEvent(event)
            }
        }
    }

    /**
     * Detach the JS bridge from given WebView widget.
     */
    fun detachBridgeFrom(webView: WebView) {
        bridgedApiService.dispose()
        webView.loadUrl("about:blank")
        webView.clearCache(true);
        webView.clearHistory();
        webView.onPause();
        webView.removeAllViews();
        webView.pauseTimers();
        webView.webChromeClient = null
    }

    /**
     * Pass response data back the webSDK using the WebView's JS evaluation.
     */
    private fun evaluateJS(id: String, evaluation: String) {
        val value = when (webBridgeJSConfig?.obfuscate ?: true) {
            true -> obfuscate(evaluation)
            false -> evaluation
        }
        val invocation = "javascript:$JS_EVALUATE['$id']($value);"
        bridgedWebView?.post {
            bridgedWebView?.evaluateJavascript(
                invocation
            ) { value ->
                CDCDebuggable.log(LOG_TAG, "evaluateJS: onReceiveValue: $value")
            }
        }
    }

    /**
     * Parse URL and invoke bridge action.
     */
    fun invokeBridgeUrl(uri: Uri): Boolean {
        val scheme = uri.scheme
        if (scheme != URI_REDIRECT_SCHEME) return false
        val path = uri.path ?: return false
        val host = uri.host ?: return false
        return invokeBridgeAction(
            host, path.replace("/", ""), uri.encodedQuery
        )
    }

    /**
     * Perform JS bridge action.
     * Once the bridge is active, the web SDK will control the data flow. Injecting actions
     * via the "mobile adapter".
     * Networking will be performed by the mobile SDK, evaluating back all responses to allow
     * the webSDK to continue the screen-set flow.
     */
    fun invokeBridgeAction(
        action: String?,
        method: String,
        queryStringParams: String?
    ): Boolean {
        if (action == null) return false
        if (queryStringParams == null) return false

        val data = queryStringParams.parseQueryStringParams()
        val params = data["params"]?.let {
            when (webBridgeJSConfig?.obfuscate ?: false) {
                true -> deobfuscate(it).parseQueryStringParams()
                false -> it.parseQueryStringParams()
            }
        } ?: mapOf()
        val callbackID = data["callbackID"]

        when (action) {
            ACTION_GET_IDS -> {
                val ids = "{\"gmid\":\"${bridgedApiService.gmid()}\"}"
                CDCDebuggable.log(LOG_TAG, "$action: $ids")
                evaluateJS(callbackID!!, ids)
            }

            ACTION_IS_SESSION_VALID -> {
                val session = bridgedApiService.session()
                CDCDebuggable.log(LOG_TAG, "$action: ${session != null}")
                evaluateJS(callbackID!!, (session != null).toString())
            }

            ACTION_SEND_REQUEST, ACTION_SEND_OAUTH_REQUEST -> {
                CDCDebuggable.log(LOG_TAG, "$action: ")
                // Specific mapping is required to handle legacy & new apis.
                bridgedApiService.onRequest(action, method, params, callbackID!!)
            }

            ACTION_ON_PLUGIN_EVENT -> {
                CDCDebuggable.log(LOG_TAG, "$action: ${params.toString()}")
                val containerId = params["sourceContainerID"]
                if (containerId != null) {
                    // Stream plugin events.
                    streamEvent(WebBridgeJSEvent(params))
                }
            }
        }
        return true
    }

    /**
     * Traffic base64 encode.
     */
    private fun deobfuscate(base64String: String): String {
        val data = Base64.decode(base64String, Base64.NO_WRAP)
        return String(data, charset("UTF-8"))
    }

    /**
     * Traffic base64 decode.
     */
    private fun obfuscate(input: String): String {
        val data: ByteArray = input.toByteArray(charset("UTF-8"))
        val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
        return "\"$base64\""
    }

    /**
     * JS bridge interface used to communicate with the webSDK.
     */
    private inner class ScreenSetsJavaScriptInterface(val apiKey: String) {

        @JavascriptInterface
        fun getAPIKey(): String = apiKey

        @JavascriptInterface
        fun getAdapterName(): String = JS_ADAPTER_NAME

        @JavascriptInterface
        fun getObfuscationStrategy(): String = "base64"

        @JavascriptInterface
        fun getFeatures(): String = buildJsonArray {
            add("is_session_valid")
            add("send_request")
            add("send_oauth_request")
            add("get_ids")
            add("on_plugin_event")
            add("on_custom_event")
            add("register_for_namespace_events")
            add("on_js_exception")
        }.toString()

        @JavascriptInterface
        fun sendToMobile(
            action: String?,
            method: String,
            queryStringParams: String
        ): Boolean =
            invokeBridgeAction(
                action,
                method,
                queryStringParams
            )
    }

}
