package com.sap.cdc.android.sdk.feature

/**
 * Standardized error codes for authentication operations.
 *
 * This object provides factory methods for creating common AuthError instances
 * with predefined error codes and messages.
 *
 * @author SAP CIAM SDK Team
 * @since 2024
 *
 * Copyright: SAP LTD.
 */
object AuthErrorCodes {

    /**
     * Creates an AuthError for operation cancellation.
     *
     * This error is used when a user explicitly cancels an authentication operation,
     * such as closing a provider login dialog or canceling biometric authentication.
     *
     * @return AuthError with code 200001 and message "Operation canceled"
     */
    fun operationCanceled(): AuthError = AuthError(
        code = 200001,
        message = "Operation canceled",
        details = null
    )

    /**
     * Creates an AuthError for provider-specific errors.
     *
     * This error is used for issues related to authentication providers, including:
     * - Missing or invalid provider configuration
     * - Provider-specific errors (e.g., Facebook, Google login failures)
     * - Missing context required for provider operations
     *
     * @return AuthError with code 500023 and message "Provider error"
     */
    fun providerError(): AuthError = AuthError(
        code = 500023,
        message = "Provider error",
        details = null
    )
}
