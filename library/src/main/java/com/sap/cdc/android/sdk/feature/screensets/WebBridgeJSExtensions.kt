package com.sap.cdc.android.sdk.feature.screensets

import android.view.ViewGroup
import android.webkit.WebView
import com.sap.cdc.android.sdk.CIAMDebuggable

/**
 * Extension methods for WebBridgeJS to simplify ScreenSetsCallbacks usage
 */

/**
 * Configure event callbacks with builder pattern
 */
fun WebBridgeJS.configureEvents(configure: ScreenSetsCallbacks.() -> Unit) {
    val callbacks = ScreenSetsCallbacks().apply(configure)
    this.attachCallbacks(callbacks)
}

/**
 * Quick setup for common event patterns
 */
fun WebBridgeJS.onScreenSetEvents(
    onLoad: ((ScreenSetsEventData) -> Unit)? = null,
    onSubmit: ((ScreenSetsEventData) -> Unit)? = null,
    onError: ((ScreenSetsError) -> Unit)? = null,
    configure: (ScreenSetsCallbacks.() -> Unit)? = null
) {
    configureEvents {
        onLoad?.let { this.onLoad = it }
        onSubmit?.let { this.onSubmit = it }
        onError?.let { this.onError = it }
        configure?.invoke(this)
    }
}

/**
 * Immediately dispose and cleanup the WebView from both the WebBridgeJS and the view hierarchy.
 * This method ensures the WebView is completely removed before any navigation occurs.
 * 
 * Use this when you need to explicitly cleanup the WebView, such as before navigating away
 * from a screen-set view in response to events like onLogin, onLogout, etc.
 * 
 * @param webView The WebView instance to dispose
 */
fun WebBridgeJS.disposeWebViewImmediately(webView: WebView) {
    try {
        // 1. Detach the WebBridgeJS (handles internal cleanup: about:blank, cache, history, timers)
        this.detachBridgeFrom(webView)
        
        // 2. Forcibly remove from parent ViewGroup to ensure immediate UI removal
        (webView.parent as? ViewGroup)?.removeView(webView)
        
        CIAMDebuggable.log(WebBridgeJS.LOG_TAG, "WebView disposed immediately and removed from view hierarchy")
    } catch (e: Exception) {
        CIAMDebuggable.log(WebBridgeJS.LOG_TAG, "Error during immediate WebView disposal: ${e.message}")
    }
}
