package com.sap.cdc.android.sdk.feature.provider

import com.sap.cdc.android.sdk.feature.AuthError

/**
 * Authentication provider exception types and exception class.
 * 
 * Defines exception types and error handling for authentication provider failures.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */

/**
 * Provider exception types.
 * 
 * - CANCELED: User canceled the authentication flow
 * - PROVIDER_FAILURE: Provider encountered an error
 * - HOST_NULL: Required host activity was null
 */
enum class ProviderExceptionType {
    CANCELED, PROVIDER_FAILURE, HOST_NULL
}

/**
 * Exception thrown during authentication provider operations.
 * 
 * @property type The type of provider exception
 * @property error Optional authentication error details
 */
data class ProviderException(
    val type: ProviderExceptionType,
    val error: AuthError? = null,
) : Exception(error?.details)
