package com.sap.cdc.android.sdk.feature.screensets

import com.sap.cdc.android.sdk.core.api.model.CDCError

/**
 * Event wrapper for JavaScript bridge communication.
 * 
 * Encapsulates events from web-based ScreenSets, providing type-safe access
 * to event data and factory methods for common events.
 * 
 * @property content Map containing event data including eventName and payload
 * 
 * @author Tal Mirmelshtein
 * @since 13/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see WebBridgeJS
 * @see ScreenSetsEventData
 */
class WebBridgeJSEvent(
    val content: Map<String, Any?>? = mapOf()
) {
    companion object {
        const val BEFORE_SCREEN_LOAD: String = "beforeScreenLoad"
        const val LOAD: String = "load"
        const val AFTER_SCREEN_LOAD: String = "afterScreenLoad"
        const val FIELD_CHANGED: String = "fieldChanged"
        const val BEFORE_VALIDATION: String = "beforeValidation"
        const val AFTER_VALIDATION: String = "afterValidation"
        const val BEFORE_SUBMIT: String = "beforeSubmit"
        const val SUBMIT: String = "submit"
        const val AFTER_SUBMIT: String = "afterSubmit"
        const val ERROR: String = "error"
        const val HIDE: String = "hide"

        const val LOGIN_STARTED: String = "login_started"
        const val LOGIN: String = "login"
        const val LOGOUT: String = "logout"
        const val ADD_CONNECTION: String = "addConnection"
        const val REMOVE_CONNECTION: String = "removeConnection"
        const val CANCELED: String = "canceled"

        fun canceledEvent(): WebBridgeJSEvent = WebBridgeJSEvent(
            mapOf("eventName" to CANCELED)
        )

        fun errorEvent(error: CDCError?): WebBridgeJSEvent = WebBridgeJSEvent(
            mapOf("eventName" to ERROR, "error" to error)
        )

        fun loginEvent(): WebBridgeJSEvent = WebBridgeJSEvent(
            mapOf("eventName" to LOGIN)
        )

        fun logoutEvent(): WebBridgeJSEvent = WebBridgeJSEvent(
            mapOf("eventName" to LOGOUT)
        )

    }

    fun name(): String? = content!!["eventName"]?.toString()

}

/**
 * Container for JavaScript evaluation requests.
 * 
 * Holds data required to evaluate JavaScript in the WebView and
 * optionally trigger follow-up events.
 * 
 * @property containerID The container ID where evaluation should occur
 * @property evaluationString The JavaScript code to evaluate
 * @property event Optional event to trigger after evaluation
 */
data class WebBridgeJSEvaluation(
    val containerID: String,
    val evaluationString: String,
    val event: WebBridgeJSEvent?
)
