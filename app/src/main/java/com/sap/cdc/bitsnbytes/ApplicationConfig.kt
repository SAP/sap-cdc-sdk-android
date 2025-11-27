package com.sap.cdc.bitsnbytes

/**
 * Application global configuration handler.
 */
object ApplicationConfig {

    var useWebViews: Boolean = false
    
    private var _debugNavigationLogging: Boolean = false
    val debugNavigationLogging: Boolean
        get() = _debugNavigationLogging

    /**
     * Enable/disable web views usage. This config is used to change the behavior of the application
     * to use screen-set flows instead of native ui views.
     */
    fun useWebViews(use: Boolean) {
        useWebViews = use
    }

    /**
     * Enable/disable debug navigation logging. When enabled, all navigation events will be logged
     * to logcat with the tag "NAV_DEBUG" including from/to routes and back stack information.
     * Default: false
     * 
     * Usage: adb logcat -s NAV_DEBUG
     */
    fun setDebugNavigationLogging(enabled: Boolean) {
        _debugNavigationLogging = enabled
    }
}
