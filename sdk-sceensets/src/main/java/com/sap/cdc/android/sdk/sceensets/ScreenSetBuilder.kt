package com.sap.cdc.android.sdk.sceensets

import org.json.JSONObject

/**
 * Created by Tal Mirmelshtein on 14/06/2024
 * Copyright: SAP LTD.
 */
class ScreenSetBuilder private constructor(val screenSetUrl: String) {

    companion object {
        const val CONTAINER_ID_DEFAULT = "pluginContainer"
        const val REDIRECT_URI_SCHEME_DEFAULT = "gsapi"
        const val JS_LOAD_ERROR_SCHEME_DEFAULT = "on_js_load_error"
        const val JS_EXCEPTION_SCHEME_DEFAULT = "on_js_exception"
        const val JS_TIMEOUT_DEFAULT = 10000
        const val DOMAIN_DEFAULT = "us1.gigya.com"
        const val PLUGIN_DEFAULT = "accounts.screenSet"
    }

    data class Builder(
        var apiKey: String,
        var domain: String? = DOMAIN_DEFAULT,
        var plugin: String? = PLUGIN_DEFAULT,
        var containerID: String? = CONTAINER_ID_DEFAULT,
        var params: MutableMap<String, Any>? = mutableMapOf(),
        var jsTimeout: Int? = JS_TIMEOUT_DEFAULT,
        var redirectUrlScheme: String? = REDIRECT_URI_SCHEME_DEFAULT,
        var jsExceptionScheme: String? = JS_EXCEPTION_SCHEME_DEFAULT,
        var jsLoadErrorScheme: String? = JS_LOAD_ERROR_SCHEME_DEFAULT
    ) {

        fun domain(domain: String) = apply { this.domain = domain }
        fun plugin(plugin: String) = apply { this.plugin = plugin }
        fun containerID(id: String) = apply { this.containerID = id }
        fun jsTimeout(timeout: Int) = apply { this.jsTimeout = timeout }
        fun redirectUri(scheme: String) = apply { this.redirectUrlScheme = scheme }
        fun jsExceptionScheme(scheme: String) = apply { this.jsExceptionScheme = scheme }
        fun jsLoadErrorScheme(scheme: String) = apply { this.jsLoadErrorScheme = scheme }

        fun params(params: MutableMap<String, Any>) = apply {
            this.params = params
            params["containerID"] = CONTAINER_ID_DEFAULT

            // Check/Add baseline parameters.
            if (!params.containsKey("lang")) {
                params["lang"] = "en"
            }
            if (!params.containsKey("deviceType")) {
                params["deviceType"] = "mobile"
            }
            if (params.containsKey("commentsUI")) {
                params["hideShareButtons"] = true
                if (params["version"] != null && params["version"] as Int == -1) {
                    params["version"] = 2
                }
            }
            if (params.containsKey("RatingUI") && params["showCommentButton"] == null) {
                params["showCommentButton"] = false
            }
        }

        private val template: String = "<head>" +
                "<link rel=\"icon\" href=\"data:,\">" +
                "<meta name='viewport' content=" +
                "'initial-scale=1,maximum-scale=1,user-scalable=no' />" +
                "<script>" +
                "function onJSException(ex) {" +
                "document.location.href = '%s://%s?ex=' + encodeURIComponent(ex);" +
                "}" +
                "function onJSLoad() {" +
                "if (gigya && gigya.isGigya)" +
                "window.__wasSocializeLoaded = true;" +
                "}" +
                "setTimeout(function() {" +
                "if (!window.__wasSocializeLoaded)" +
                "document.location.href = '%s://%s';" +
                "}, %s);" +
                "</script>" +
                "<script src='https://cdns." +
                "%s" +
                "/JS/gigya.js?apikey=%s&lang=%s' type='text/javascript' onLoad='onJSLoad();'>" +
                "{" +
                "deviceType: 'mobile'" +
                "}" +
                "</script>" +
                "</head>" +
                "<body>" +
                "<div id='%s'></div>" +
                "<script>" +
                "%s" +
                "try {" +
                "gigya._.apiAdapters.mobile.showPlugin('%s', %s);" +
                "} catch (ex) { onJSException(ex); }" +
                "</script>" +
                "</body>"

        fun build(): ScreenSetBuilder = ScreenSetBuilder(
            String.format(
                template,
                redirectUrlScheme,
                jsExceptionScheme,
                redirectUrlScheme,
                jsLoadErrorScheme,
                jsTimeout,
                domain,
                apiKey,
                params!!["lang"],
                containerID,
                "",  // js script before showing the plugin
                plugin,
                JSONObject((params as Map<*, *>?)!!)
            )
        )
    }
}