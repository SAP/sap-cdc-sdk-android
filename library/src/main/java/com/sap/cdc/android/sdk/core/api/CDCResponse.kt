package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.core.network.HttpExceptions
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
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
     * Parse response from json string.
     */
    fun fromJSON(json: String) = apply {
        jsonResponse = json
        jsonObject = Json.parseToJsonElement(jsonResponse!!).jsonObject
    }

    /**
     * From provided CDCError class initializer
     */
    fun fromError(error: CDCError)  = apply {
        fromError(error.errorCode, error.errorDescription ?: "", error.errorDetails ?: "")
    }

    /**
     * Custom error initializer.
     * Internally used.
     */
    fun fromError(code: Int, message: String, description: String) = apply {
        fromJSON(
            "{" +
                    "  \"errorCode\":  \"$code\"," +
                    "  \"errorMessage\": \"$message\"," +
                    "  \"errorDetails\": \"$description\"" +
                    "}"
        )
    }


    fun fromException(e: Exception) = apply {
        fromError(
            -1,
            e.localizedMessage ?: "Internal error",
            e.message ?: "Internal error"
        )
    }

    internal fun fromHttpException(e: HttpExceptions) = apply {
        val statusCode: HttpStatusCode = e.response.status
        fromError(
            statusCode.value,
            statusCode.description,
            ""
        )
    }

    /**
     * No network error initializer.
     */
    internal fun noNetwork() = apply {
        fromError(
            400106,
            "Not connected",
            "User is not connected to the required network or to any network"
        )
    }

    internal fun providerError() = apply {
        fromError(
            400122,
            "Provider error",
            "Provider configuration error"
        )
    }



    /**
     * Get response as json string.
     */
    fun asJson(): String? = jsonResponse

    /**
     * Get call id from response.
     * Unique identifier of the transaction, for debugging purposes.
     * Call id may be used to track the request/response data to identify
     * possible issues.
     */
    fun callId(): String? = jsonObject?.get("callId")?.jsonPrimitive?.contentOrNull

    /**
     * Get error code from response.
     * The result code of the operation. Code '0' indicates success, any other number indicates failure.
     * @see [Response Codes and Errors](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/416d41b170b21014bbc5a10ce4041860.html?q=error%20code)
     */
    fun errorCode(): Int? = jsonObject?.get("errorCode")?.jsonPrimitive?.intOrNull

    /**
     * Get error message from response.
     * A short textual description of an error associated with the errorCode for logging purposes.
     */
    fun errorMessage(): String? = jsonObject?.get("errorMessage")?.jsonPrimitive?.contentOrNull

    /**
     * Get error details from response.
     * This field will appear in the response only in case of an error and will contain the exception info, if available.
     */
    fun errorDetails(): String? = jsonObject?.get("errorDetails")?.jsonPrimitive?.contentOrNull

    /**
     * Get error flags from response.
     */
    fun errorFlags(): String? = jsonObject?.get("errorFlags")?.jsonPrimitive?.contentOrNull

    /**
     * Get an instance of the CDCError class. This class's data is limited and should only be used for display options.
     * To handle resolvable errors, use the CDCResponse class as it will contain all the necessary flow data.
     */
    fun toCDCError(): CDCError = CDCError(errorCode()!!, errorMessage(), errorDetails())

    /**
     * Determine of the response is considered as an error according to current backend schema.
     */
    fun isError(): Boolean = errorCode() != null && errorCode() != 0

    /**
     * Check if the CxResponse Json contains an element with key.
     */
    fun containsKey(key: String): Boolean {
        return jsonObject?.containsKey(key) ?: false
    }

    /**
     *  Custom serialization of the entire CxResponse object to a custom serializable object.
     */

    inline fun <reified T> serializeTo(): T? {
        if (jsonResponse != null) {
            try {
                return json.decodeFromString<T>(jsonResponse!!)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return null
    }

    /**
     * Custom serialization of a specific Json element within the CxResponse.
     */
    inline fun <reified T> serializeObject(key: String): T? {
        if (jsonObject != null) {
            if (!jsonObject!!.contains(key)) return null
            val jsonObjectString = jsonObject!![key].toString()
            try {
                return json.decodeFromString<T>(jsonObjectString)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return null
    }

    /**
     * Parse string field from response json.
     */
    fun stringField(key: String): String? = jsonObject?.get(key)?.jsonPrimitive?.contentOrNull

    /**
     * Parse integer field from response json.
     */
    fun intField(key: String): Int? = jsonObject?.get(key)?.jsonPrimitive?.intOrNull

}


