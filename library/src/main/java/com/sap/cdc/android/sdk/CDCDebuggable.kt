package com.sap.cdc.android.sdk

import android.util.Log
import android.webkit.WebView
import com.sap.cdc.android.sdk.core.network.NetworkClient

object CDCDebuggable {

    private var debug: Boolean = false
    private var http: Boolean = false

    fun debugLogging(value: Boolean) {
        debug = value
    }

    fun httpLogging(value: Boolean) {
        http = value
    }

    /**
     * Check if debug logging is currently enabled.
     * Used by extension functions to conditionally print stack traces.
     * 
     * @return true if debug logging is enabled, false otherwise
     */
    fun isDebugEnabled(): Boolean = debug

    fun setWebViewDebuggable(value: Boolean) {
        WebView.setWebContentsDebuggingEnabled(value)
    }

    fun log(tag: String, message: String) {
        if (debug) {
            if (tag == NetworkClient.LOG_TAG && !http) {
                return
            }
            Log.d("CDC_$tag", message)
        }
    }
}
