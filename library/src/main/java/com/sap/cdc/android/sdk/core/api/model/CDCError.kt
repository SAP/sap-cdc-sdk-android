package com.sap.cdc.android.sdk.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Compact error class aligned with CDC response standard.
 */
@Serializable
data class CDCError(
    val errorCode: Int,
    val errorMessage: String? = null,
    var errorDetails: String? = null,
) {

    companion object {
        
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun contextError(): CDCError = CDCError(-1, "Application context error")

        fun operationCanceled(): CDCError = CDCError(200001, "Operation canceled")

        fun providerError(): CDCError = CDCError(400122, "Provider configuration error")

        fun fromJson(json: String): CDCError = CDCError.json.decodeFromString(json)
    }
}
