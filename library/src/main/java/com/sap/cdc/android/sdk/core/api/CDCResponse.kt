package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.core.network.HttpExceptions
import com.sap.cdc.android.sdk.extensions.printDebugStackTrace
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Response wrapper for SAP CDC API operations.
 * 
 * This class encapsulates responses from CDC API calls and provides convenient methods
 * for parsing, error handling, and data extraction. It handles:
 * - JSON response parsing and serialization
 * - Error code and message extraction
 * - Custom error response generation
 * - Response validation and status checking
 * - Type-safe deserialization of response data
 * 
 * The CDCResponse provides multiple ways to access response data:
 * - Raw JSON string via [asJson]
 * - Parsed JsonObject via [jsonObject]
 * - Typed deserialization via [serializeTo] and [serializeObject]
 * - Field accessors like [stringField], [intField]
 * - Error information via [errorCode], [errorMessage], [errorDetails]
 * 
 * @property jsonResponse The raw JSON response string from the CDC API
 * @property jsonObject The parsed JSON object representation of the response
 * @property json The JSON serializer instance configured for CDC responses
 * 
 * @constructor Creates an empty CDCResponse. Use builder methods like [fromJSON], [fromError], etc.
 *              to populate the response data.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see com.sap.cdc.android.sdk.core.api.CDCRequest
 * @see com.sap.cdc.android.sdk.core.api.Api
 */
class CDCResponse {

    var jsonResponse: String? = null
    var jsonObject: JsonObject? = null

    val json: Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    /**
     * Parses and initializes the response from a JSON string.
     * 
     * This method converts the raw JSON string into both a stored string representation
     * and a parsed JsonObject for convenient data access.
     * 
     * @param json The JSON string to parse
     * @return This CDCResponse instance for method chaining
     */
    fun fromJSON(json: String) = apply {
        jsonResponse = json
        jsonObject = Json.parseToJsonElement(jsonResponse!!).jsonObject
    }

    /**
     * Initializes the response with custom error information.
     * 
     * This method constructs an error response with the specified code, message, and description.
     * It's primarily used internally by the SDK to create standardized error responses.
     * Uses kotlinx.serialization's buildJsonObject to properly handle special characters
     * and prevent JSON parsing errors.
     * 
     * @param code The error code (0 indicates success, non-zero indicates failure)
     * @param message A short error message describing the error
     * @param details Detailed error description with additional context
     * @return This CDCResponse instance for method chaining
     */
    fun fromError(code: Int, message: String, details: String?) = apply {
        val errorJson = buildJsonObject {
            put("errorCode", code.toString())
            put("errorMessage", message)
            put("errorDetails", details)
        }
        fromJSON(errorJson.toString())
    }


    /**
     * Initializes the response from an exception.
     * 
     * This method converts a generic exception into a CDC error response, using error code -1
     * to indicate an internal error. The exception's localized message and message are used
     * for the error message and details respectively.
     * 
     * @param e The exception to convert to an error response
     * @return This CDCResponse instance for method chaining
     */
    fun fromException(e: Exception) = apply {
        fromError(
            -1,
            e.localizedMessage ?: "Internal error",
            e.message ?: "Internal error"
        )
    }

    /**
     * Initializes the response from an HTTP exception.
     * 
     * This internal method converts an HttpExceptions instance into a CDC error response,
     * using the HTTP status code and description as the error information.
     * 
     * @param e The HttpExceptions instance containing HTTP error details
     * @return This CDCResponse instance for method chaining
     */
    internal fun fromHttpException(e: HttpExceptions) = apply {
        val statusCode: HttpStatusCode = e.response.status
        fromError(
            statusCode.value,
            statusCode.description,
            ""
        )
    }

    /**
     * Initializes the response with a network unavailable error.
     * 
     * This internal method creates a standardized error response (code 400106) indicating
     * that the device is not connected to a network.
     * 
     * @return This CDCResponse instance for method chaining
     */
    internal fun noNetwork() = apply {
        fromError(
            400106,
            "Not connected",
            "User is not connected to the required network or to any network"
        )
    }

    /**
     * Initializes the response with a provider configuration error.
     * 
     * This internal method creates a standardized error response (code 400122) indicating
     * a problem with social provider configuration (e.g., Facebook, Google login setup).
     * 
     * @param message Optional custom error message. Defaults to "Provider error" if not specified.
     * @return This CDCResponse instance for method chaining
     */
    internal fun providerError(message: String? = null) = apply {
        fromError(
            400122,
            message ?: "Provider error",
            "Provider configuration error"
        )
    }



    /**
     * Returns the raw JSON response as a string.
     * 
     * @return The JSON string representation of the response, or null if not set
     */
    fun asJson(): String? = jsonResponse

    /**
     * Extracts the call ID from the response.
     * 
     * The call ID is a unique identifier for the transaction, used primarily for debugging
     * and support purposes. It can be used to track request/response data in CDC logs
     * to identify and troubleshoot issues.
     * 
     * @return The call ID string, or null if not present in the response
     */
    fun callId(): String? = jsonObject?.get("callId")?.jsonPrimitive?.contentOrNull

    /**
     * Extracts the error code from the response.
     * 
     * The error code indicates the result of the operation:
     * - 0: Success
     * - Non-zero: Failure (specific error codes are documented in CDC documentation)
     * 
     * @return The error code as an integer, or null if not present in the response
     * 
     * @see [Response Codes and Errors](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/416d41b170b21014bbc5a10ce4041860.html?q=error%20code)
     */
    fun errorCode(): Int? = jsonObject?.get("errorCode")?.jsonPrimitive?.intOrNull

    /**
     * Extracts the error message from the response.
     * 
     * The error message provides a short textual description of the error associated with
     * the error code, suitable for logging and debugging purposes.
     * 
     * @return The error message string, or null if not present in the response
     */
    fun errorMessage(): String? = jsonObject?.get("errorMessage")?.jsonPrimitive?.contentOrNull

    /**
     * Extracts detailed error information from the response.
     * 
     * This field appears only when an error occurs and may contain additional technical
     * details, exception information, or context about the failure.
     * 
     * @return The error details string, or null if not present in the response
     */
    fun errorDetails(): String? = jsonObject?.get("errorDetails")?.jsonPrimitive?.contentOrNull

    /**
     * Extracts error flags from the response.
     * 
     * Error flags provide additional context about the error, such as whether it's
     * recoverable or requires specific user actions.
     * 
     * @return The error flags string, or null if not present in the response
     */
    fun errorFlags(): String? = jsonObject?.get("errorFlags")?.jsonPrimitive?.contentOrNull

    /**
     * Determines if the response represents an error.
     * 
     * A response is considered an error if it contains an error code that is non-null
     * and non-zero. Error code 0 or null indicates success.
     * 
     * @return true if the response contains an error, false otherwise
     */
    fun isError(): Boolean = errorCode() != null && errorCode() != 0

    /**
     * Checks if the response JSON contains a specific key.
     * 
     * This method is useful for verifying the presence of optional response fields
     * before attempting to extract their values.
     * 
     * @param key The JSON key to check for
     * @return true if the key exists in the response, false otherwise
     */
    fun containsKey(key: String): Boolean {
        return jsonObject?.containsKey(key) ?: false
    }

    /**
     * Deserializes the entire response to a custom data class.
     * 
     * This method converts the complete JSON response into a specified Kotlin data class
     * or serializable object. It's useful when you want to work with typed objects instead
     * of raw JSON.
     * 
     * Example:
     * ```
     * data class AccountInfo(val UID: String, val profile: Profile)
     * val accountInfo: AccountInfo? = response.serializeTo<AccountInfo>()
     * ```
     * 
     * @param T The type to deserialize the response into (must be serializable)
     * @return An instance of T containing the deserialized data, or null if deserialization fails
     */
    inline fun <reified T> serializeTo(): T? {
        if (jsonResponse != null) {
            try {
                return json.decodeFromString<T>(jsonResponse!!)
            } catch (ex: Exception) {
                ex.printDebugStackTrace("CDCResponse")
            }
        }
        return null
    }

    /**
     * Deserializes a specific field from the response to a custom data class.
     * 
     * This method extracts a nested JSON object or array from the response and deserializes
     * it into the specified type. It's useful for extracting and typing specific response fields.
     * 
     * Example:
     * ```
     * data class Profile(val firstName: String, val lastName: String)
     * val profile: Profile? = response.serializeObject<Profile>("profile")
     * ```
     * 
     * @param key The JSON key of the field to deserialize
     * @param T The type to deserialize the field into (must be serializable)
     * @return An instance of T containing the deserialized field data, or null if the key
     *         doesn't exist or deserialization fails
     */
    inline fun <reified T> serializeObject(key: String): T? {
        if (jsonObject != null) {
            if (!jsonObject!!.contains(key)) return null
            val jsonObjectString = jsonObject!![key].toString()
            try {
                return json.decodeFromString<T>(jsonObjectString)
            } catch (ex: Exception) {
                ex.printDebugStackTrace("CDCResponse")
            }
        }
        return null
    }

    /**
     * Extracts a string field from the response.
     * 
     * This is a convenience method for accessing string values directly from the JSON response.
     * 
     * @param key The JSON key of the field to extract
     * @return The string value, or null if the key doesn't exist or the value is not a string
     */
    fun stringField(key: String): String? = jsonObject?.get(key)?.jsonPrimitive?.contentOrNull

    /**
     * Extracts an integer field from the response.
     * 
     * This is a convenience method for accessing integer values directly from the JSON response.
     * 
     * @param key The JSON key of the field to extract
     * @return The integer value, or null if the key doesn't exist or the value is not an integer
     */
    fun intField(key: String): Int? = jsonObject?.get(key)?.jsonPrimitive?.intOrNull

}
