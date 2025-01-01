package com.sap.cdc.android.sdk.core.api

import android.util.Log
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.network.HttpExceptions
import com.sap.cdc.android.sdk.extensions.isOnline
import com.sap.cdc.android.sdk.extensions.prepareApiUrl
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
open class Api(private val coreClient: CoreClient) {

    companion object {
        const val LOG_TAG = "Api"
    }

    /**
     * Check network connectivity.
     */
    fun networkAvailable(): Boolean =
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
        coreClient.siteConfig.setServerOffset(serverDate)
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
        coreClient.siteConfig.setServerOffset(serverDate)
        // Forward response.
        return CDCResponse().fromJSON(result.body())
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
                            .timestamp(coreClient.siteConfig.getServerTimestamp())
                            .parameters(parameters)
                            .headers(headers)
                    )
                }

                else -> post(
                    CDCRequest(coreClient.siteConfig)
                        .method(HttpMethod.Post.value)
                        .api(api.prepareApiUrl(coreClient.siteConfig))
                        .parameters(parameters)
                        .timestamp(coreClient.siteConfig.getServerTimestamp())
                        .headers(headers)
                )
            }
        } catch (e: HttpExceptions) {
            Log.e(LOG_TAG, e.message)
            return CDCResponse().fromHttpException(e)
        }
    }

}

