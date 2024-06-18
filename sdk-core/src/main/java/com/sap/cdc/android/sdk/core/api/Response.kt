package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.core.api.model.CDCError
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

    fun fromJSON(json: String) = apply {
        jsonResponse = json
        jsonObject = Json.parseToJsonElement(jsonResponse!!).jsonObject
    }

    fun fromError(code: Int, message: String, description: String) = apply {
        fromJSON(
            "{" +
                    "  \"errorCode\":  $code" +
                    "  \"errorMessage\": $message" +
                    "  \"errorDetails\": $description" +
                    "}"
        )
    }

    fun noNetwork() = apply {
        fromError(
            400106,
            "Not connected",
            "User is not connected to the required network or to any network"
        )
    }

    fun asJson(): String? = jsonResponse

    fun callId(): String? = jsonObject?.get("callId")?.jsonPrimitive?.contentOrNull

    fun errorCode(): Int? = jsonObject?.get("errorCode")?.jsonPrimitive?.intOrNull

    fun errorMessage(): String? = jsonObject?.get("errorMessage")?.jsonPrimitive?.contentOrNull

    fun errorDetails(): String? = jsonObject?.get("errorDetails")?.jsonPrimitive?.contentOrNull

    fun isError(): Boolean = errorCode() != null && errorCode() != 0

    fun toCDCError(): CDCError = CDCError(errorCode()!!, errorDetails(), errorMessage())

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
     * Check if the CxResponse Json contains an element with key.
     */
    fun containsKey(key: String): Boolean {
        return jsonObject?.containsKey(key) ?: false
    }

    /**
     * Parse string field from response json.
     */
    fun stringField(key: String): String? = jsonObject?.get(key)?.jsonPrimitive?.contentOrNull

}


