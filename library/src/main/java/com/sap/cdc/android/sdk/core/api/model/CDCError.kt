package com.sap.cdc.android.sdk.core.api.model

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
data class CDCError(
    val errorCode: Int,
    val errorDetails: String?,
    val errorDescription: String? = null
) {

    private var dynamicParameters: MutableMap<String, Any?>? = null

    fun addDynamic(key: String, value: Any?) {
        if (dynamicParameters == null) {
            dynamicParameters = mutableMapOf()
        }
        dynamicParameters!![key] = value
    }

    fun getDynamic(key: String): Any? = dynamicParameters?.get(key)

    companion object {

        fun contextError(): CDCError = CDCError(-1, "Application context error")

        fun operationCanceled(): CDCError = CDCError(200001, "Operation canceled")

        fun providerError(): CDCError = CDCError(400122, "Provider configuration error")
    }
}
