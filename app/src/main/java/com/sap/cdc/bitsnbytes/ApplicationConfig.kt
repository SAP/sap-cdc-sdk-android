package com.sap.cdc.bitsnbytes

/**
 * Application global configuration handler.
 */
object ApplicationConfig {

    var useWebViews: Boolean = false

    /**
     * Enable/disable web views usage. This config is used to change the behavior of the application
     * to use screen-set flows instead of native ui views.
     */
    fun useWebViews(use: Boolean) {
        useWebViews = use
    }
}