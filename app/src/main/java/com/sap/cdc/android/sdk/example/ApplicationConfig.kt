package com.sap.cdc.android.sdk.example

/**
 * Application global configuration handler.
 */
object ApplicationConfig {

    var useWebViews: Boolean = false

    fun useWebViews(use: Boolean) {
        this.useWebViews = use
    }
}