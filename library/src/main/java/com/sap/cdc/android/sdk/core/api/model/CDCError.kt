package com.sap.cdc.android.sdk.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


/**
 * CDC API error response.
 * 
 * Represents errors returned by SAP CDC services with standardized error codes and messages.
 * 
 * ## Usage
 * ```kotlin
 * authService.authenticate().login().credentials(creds) {
 *     onError = { authError ->
 *         // AuthError wraps CDCError
 *         println("Error ${authError.code}: ${authError.message}")
 *         
 *         // Access underlying CDC error if needed
 *         val cdcError = response.toCDCError()
 *         when (cdcError.errorCode) {
 *             400003 -> // Invalid credentials
 *             206001 -> // Account pending registration
 *             // Handle specific codes
 *         }
 *     }
 * }
 * ```
 * 
 * ## Common Error Codes
 * - `400003` - Invalid login credentials
 * - `206001` - Account pending registration
 * - `206002` - Account pending verification
 * - `403043` - Invalid or expired token
 * - `403120` - Account linking required
 * 
 * @property errorCode CDC error code
 * @property errorMessage Human-readable error message
 * @property errorDetails Additional error details (optional)
 * @see AuthError
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
