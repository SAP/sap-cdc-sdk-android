package com.sap.cdc.android.sdk.feature.screensets

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
