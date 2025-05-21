package com.sap.cdc.android.sdk.core.api

interface CDCResponseEvaluator {

    fun evaluate(response: CDCResponse): Boolean
}

class InvalidGMIDResponseEvaluator : CDCResponseEvaluator {

    companion object {
        const val DETAILS_CAUSE_MISSING_COOKIE = "missing cookie"
        const val DETAILS_SESSION_IS_INVALID = "Session is invalid (Missing DeviceId)"
        const val DETAILS_MISSING_GCID_OR_UCID = "Missing required parameter: gcid or ucid cookie"

        const val FLAGS_MISSING_KEY = "missingKey"

        const val ERROR_INVALID_PARAMETER_VALUE = 400006
    }

    override fun evaluate(response: CDCResponse): Boolean {
        if (!response.isError()) return false

        val errorCode = response.errorCode()
        val errorDetails = response.errorDetails()
        val errorFlags = response.errorFlags()

        // Check detail clauses
        if (errorDetails in listOf(
                DETAILS_CAUSE_MISSING_COOKIE,
                DETAILS_SESSION_IS_INVALID,
                DETAILS_MISSING_GCID_OR_UCID
            )
        ) {
            return true
        }

        // Check error code/flag pair
        return errorCode == ERROR_INVALID_PARAMETER_VALUE && errorFlags == FLAGS_MISSING_KEY
    }
}