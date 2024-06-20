package com.sap.cdc.android.sdk.sceensets

import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.sap.cdc.android.sdk.authentication.AuthenticationService
import com.sap.cdc.android.sdk.core.extensions.parseQueryStringParams
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

/**
 * Created by Tal Mirmelshtein on 13/06/2024
 * Copyright: SAP LTD.
 */
class WebBridgeJS(authenticationService: AuthenticationService) {

    companion object {
        const val LOG_TAG = "CDC_WebBridgeJS"

        const val URI_REDIRECT_SCHEME = "gsapi"

        const val JS_NAME = "__gigAPIAdapterSettings"
        const val JS_ADAPTER_NAME = "mobile"
        const val JS_EVALUATE = "gigya._.apiAdapters.mobile.mobileCallbacks"

        const val BASE_URL: String = "https://www.gigya.com"
        const val MIME_TYPE: String = "text/html"
        const val ENCODING: String = "utf-8"
    }

    private var bridgedWebView: WebView? = null
    private var bridgeEvents: ((WebBridgeJSEvent) -> Unit?)? = null

    var apiService: WebBridgeJSApiService = WebBridgeJSApiService(authenticationService)

    fun streamEvent(event: WebBridgeJSEvent) = bridgeEvents?.invoke(event)

    /**
     * Attach JS bridge to given WebView widget.
     */
    fun attachBridgeTo(webView: WebView, events: (WebBridgeJSEvent) -> Unit) {
        bridgeEvents = events
        bridgedWebView = webView
        bridgedWebView!!.addJavascriptInterface(
            ScreenSetsJavaScriptInterface(
                apiService.apiKey()
            ),
            JS_NAME
        )
    }

    fun detachBridgeFrom(webView: WebView) {
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
        val value = obfuscate(evaluation)
        val invocation = "javascript:$JS_EVALUATE['$id']($value);"
        bridgedWebView?.post {
            bridgedWebView?.evaluateJavascript(
                invocation
            ) { value ->
                Log.d(LOG_TAG, "evaluateJS: onReceiveValue: $value")
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
        val params = data["params"]?.let { deobfuscate(it).parseQueryStringParams() } ?: mapOf()
        val callbackID = data["callbackID"]

        when (action) {
            "get_ids" -> {
                val ids = "{\"gmid\":\"${apiService.gmid()}\"}"
                Log.d(LOG_TAG, "$action: $ids")
                evaluateJS(callbackID!!, ids)
            }

            "is_session_valid" -> {
                val session = apiService.session()
                Log.d(LOG_TAG, "$action: ${session != null}")
                evaluateJS(callbackID!!, (session != null).toString())
            }

            "send_request", "send_oauth_request" -> {
                Log.d(LOG_TAG, "$action: ")
                // Specific mapping is required to handle legacy & new apis.
                //TODO: A callback id is needed to JS evaluation of responses.
                apiService.onRequest(action, method, params)
            }

            "on_plugin_event" -> {
                Log.d(LOG_TAG, "$action: ${params.toString()}")
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
