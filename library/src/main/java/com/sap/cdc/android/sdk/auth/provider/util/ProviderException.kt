package com.sap.cdc.android.sdk.auth.provider.util

import com.sap.cdc.android.sdk.core.api.model.CDCError

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

enum class ProviderExceptionType {
    CANCELED, PROVIDER_FAILURE, HOST_NULL
}

data class ProviderException(
    val type: ProviderExceptionType,
    val error: CDCError? = null,
) : Exception(error?.errorDetails)