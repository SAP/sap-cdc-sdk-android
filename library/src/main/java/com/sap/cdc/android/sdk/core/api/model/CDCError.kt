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
    val errorDescription: String?,
    var errorDetails: String? = null,
) {

    companion object {

        fun contextError(): CDCError = CDCError(-1, "Application context error")

        fun operationCanceled(): CDCError = CDCError(200001, "Operation canceled")

        fun providerError(): CDCError = CDCError(400122, "Provider configuration error")

        fun fromJson(json: String): CDCError = Json.decodeFromString(json)
    }
}

