package com.sap.cdc.android.sdk.sceensets

import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sap.cdc.android.sdk.sceensets.ScreenSetBuilder.Companion.JS_EXCEPTION_SCHEME_DEFAULT
import com.sap.cdc.android.sdk.sceensets.ScreenSetBuilder.Companion.JS_LOAD_ERROR_SCHEME_DEFAULT
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS.Companion.URI_REDIRECT_SCHEME

/**
 * Created by Tal Mirmelshtein on 13/06/2024
 * Copyright: SAP LTD.
 */
class WebBridgeJSWebViewClient(
    private val webBridge: WebBridgeJS,
    private val onBrowserIntent: (Uri?) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request!!.url
        if (isJsLoadError(uri.scheme, uri.host)) {
            webBridge.streamEvent(
                errorEvent(
                    500032,
                    "Failed loading socialize.js"
                )
            )
        }
        if (isJsException(uri.scheme, uri.host)) {
            webBridge.streamEvent(
                errorEvent(
                    405001,
                    "Javascript error while loading plugin. Please make sure the plugin name is correct"
                )
            )
        }
        if (allowedInvocation(uri)) {
            // Stream event to bridge.
            webBridge.invokeBridgeUrl(uri)
        } else {
            // Use as ACTION_VIEW intent.
            onBrowserIntent(uri)
        }
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (error == null) return
        webBridge.streamEvent(
            errorEvent(
                error.errorCode,
                error.description.toString()
            )
        )
    }

    /**
     * Check for verified JS bridge invocation URL.
     */
    private fun allowedInvocation(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        if (scheme != URI_REDIRECT_SCHEME) return false
        val path = uri.path ?: return false
        val host = uri.host ?: return false
        return true
    }

    private fun isJsException(scheme: String?, host: String?): Boolean {
        if (scheme == null || host == null) return false
        if (scheme == URI_REDIRECT_SCHEME && host == JS_EXCEPTION_SCHEME_DEFAULT) return true
        return false
    }

    private fun isJsLoadError(scheme: String?, host: String?): Boolean {
        if (scheme == null || host == null) return false
        if (scheme == URI_REDIRECT_SCHEME && host == JS_LOAD_ERROR_SCHEME_DEFAULT) return true
        return false
    }

    private fun errorEvent(code: Int, description: String): WebBridgeJSEvent =
        WebBridgeJSEvent(
            mapOf(
                "eventName" to "error",
                "errorCode" to code,
                "description" to description,
                "dismiss" to true
            )
        )

}