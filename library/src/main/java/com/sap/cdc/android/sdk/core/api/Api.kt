package com.sap.cdc.android.sdk.core.api

import android.content.Context
import android.util.Log
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.CoreClient.Companion.CDC_CODE_CLIENT_SECURED_PREF
import com.sap.cdc.android.sdk.core.CoreClient.Companion.CDC_SERVER_OFFSET
import com.sap.cdc.android.sdk.core.network.HttpExceptions
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.extensions.isOnline
import com.sap.cdc.android.sdk.extensions.prepareApiUrl
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
open class Api(private val coreClient: CoreClient) {

    companion object {

        const val LOG_TAG = "Api"
        const val CDC_SERVER_OFFSET_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"

        fun getServerTimestamp(context: Context): String {
            val esp =
                context.getEncryptedPreferences(CDC_SERVER_OFFSET_FORMAT)
            val timestamp: String =
                ((System.currentTimeMillis() / 1000) + esp.getLong(CDC_SERVER_OFFSET, 0)).toString()
            Log.d(LOG_TAG, "serverOffset - get: $timestamp")
            return timestamp
        }
    }

    /**
     * Check network connectivity.
     */
    private fun networkAvailable(): Boolean =
        coreClient.siteConfig.applicationContext.isOnline()

    /**
     * Perform generic get request.
     */
    open suspend fun get(request: CDCRequest): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }
        val result: HttpResponse = coreClient.networkClient.http()
            .get(request.api) {
                headers {
                    request.headers.map { (k, v) ->
                        headers.append(k, v)
                    }
                }
                url {
                    request.parameters.map { (k, v) ->
                        parameters.append(k, v)
                    }

                }
            }
        val serverDate: String? = result.headers["date"]
        // Set server offset.
        setServerOffset(serverDate)
        // Forward response.
        return CDCResponse().fromJSON(result.body())
    }

    /**
     * Perform generic post request.
     */
    open suspend fun post(
        request: CDCRequest
    ): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }
        val result: HttpResponse = coreClient.networkClient.http().post(request.api) {
            headers {
                request.headers.map { (k, v) ->
                    headers.append(k, v)
                }
            }
            setBody(request.parameters.toEncodedQuery())
        }
        val serverDate: String? = result.headers["date"]
        // Set server offset.
        setServerOffset(serverDate)
        // Forward response.
        return CDCResponse().fromJSON(result.body())
    }

    /**
     * Set server offset parameter to ensure correct time alignment.
     */
    private fun setServerOffset(date: String?) {
        if (date == null) return
        val format = SimpleDateFormat(
            CDC_SERVER_OFFSET_FORMAT,
            Locale.ENGLISH
        )
        val serverDate = format.parse(date) ?: return
        val offset = (serverDate.time - System.currentTimeMillis()) / 1000
        Log.d(LOG_TAG, "serverOffset - set: $offset")
        val esp =
            coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_CODE_CLIENT_SECURED_PREF
            )
        esp.edit().putLong(CDC_SERVER_OFFSET, offset).apply()
    }

    /**
     * Generic send request function or REST operation.
     */
    @JvmOverloads
    open suspend fun genericSend(
        api: String,
        parameters: MutableMap<String, String> = mutableMapOf(),
        method: String? = HttpMethod.Post.value,
        headers: MutableMap<String, String>? = mutableMapOf()
    ): CDCResponse {
        return try {
            when (method!!) {
                HttpMethod.Get.value -> {
                    get(
                        CDCRequest(coreClient.siteConfig)
                            .method(HttpMethod.Get.value)
                            .api(api.prepareApiUrl(coreClient.siteConfig))
                            .timestamp(getServerTimestamp(coreClient.siteConfig.applicationContext))
                            .parameters(parameters)
                            .headers(headers)
                    )
                }

                else -> post(
                    CDCRequest(coreClient.siteConfig)
                        .method(HttpMethod.Post.value)
                        .api(api.prepareApiUrl(coreClient.siteConfig))
                        .parameters(parameters)
                        .timestamp(getServerTimestamp(coreClient.siteConfig.applicationContext))
                        .headers(headers)
                )
            }
        } catch (e: HttpExceptions) {
            Log.e(LOG_TAG, e.message)
            return CDCResponse().fromHttpException(e)
        }
    }

}

