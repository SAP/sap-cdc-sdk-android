package com.sap.cdc.android.sdk

import android.util.Log
import android.webkit.WebView

object CDCDebuggable {

    private var logs: Boolean = false
    private var webViewDebuggable: Boolean = false

    fun setLogs(value: Boolean) {
        logs = value
    }

    fun setWebViewDebuggable(value: Boolean) {
        WebView.setWebContentsDebuggingEnabled(value)
    }

    fun log(tag: String, message: String) {
        if (logs) {
            Log.d(tag, message)
        }
    }
}