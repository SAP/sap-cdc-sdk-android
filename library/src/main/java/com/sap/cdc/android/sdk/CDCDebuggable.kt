package com.sap.cdc.android.sdk

import android.util.Log
import android.webkit.WebView
import com.sap.cdc.android.sdk.core.network.NetworkClient

/**
 * Debug logging utility for SAP CDC Android SDK.
 * 
 * Centralized control for SDK logging. All logs are prefixed with "CDC_" for easy filtering.
 * 
 * ```kotlin
 * // Enable in Application.onCreate()
 * if (BuildConfig.DEBUG) {
 *     CDCDebuggable.debugLogging(true)
 *     CDCDebuggable.httpLogging(true)
 *     CDCDebuggable.setWebViewDebuggable(true)
 * }
 * 
 * // Filter logs: adb logcat | grep CDC_
 * ```
 */
object CDCDebuggable {

    private var debug: Boolean = false
    private var http: Boolean = false

    /**
     * Enable/disable SDK debug logging.
     * @param value true to enable, false to disable
     */
    fun debugLogging(value: Boolean) {
        debug = value
    }

    /**
     * Enable/disable HTTP request/response logging.
     * Requires [debugLogging] to also be enabled.
     * @param value true to enable, false to disable
     */
    fun httpLogging(value: Boolean) {
        http = value
    }

    /**
     * Check if debug logging is enabled.
     * @return true if enabled, false otherwise
     */
    fun isDebugEnabled(): Boolean = debug

    /**
     * Enable Chrome DevTools for WebView debugging.
     * Use `chrome://inspect` to debug ScreenSets.
     * @param value true to enable, false to disable
     */
    fun setWebViewDebuggable(value: Boolean) {
        WebView.setWebContentsDebuggingEnabled(value)
    }

    /**
     * Internal logging method. Logs are prefixed with "CDC_".
     * @param tag Component tag
     * @param message Log message
     */
    fun log(tag: String, message: String) {
        if (debug) {
            if (tag == NetworkClient.LOG_TAG && !http) {
                return
            }
            Log.d("CDC_$tag", message)
        }
    }
}
