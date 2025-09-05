package com.sap.cdc.android.sdk.feature.screensets

import com.sap.cdc.android.sdk.core.api.model.CDCError

/**
 * Created by Tal Mirmelshtein on 13/06/2024
 * Copyright: SAP LTD.
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
    }

    fun name(): String? = content!!["eventName"]?.toString()

}