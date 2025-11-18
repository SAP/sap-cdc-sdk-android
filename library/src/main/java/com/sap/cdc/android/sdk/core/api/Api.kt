package com.sap.cdc.android.sdk.core.api

import android.util.Log
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.utils.toEncodedQuery
import com.sap.cdc.android.sdk.core.network.HttpExceptions
import com.sap.cdc.android.sdk.extensions.isOnline
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

        const val HEADER_DATE = "date"
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

        return try {
            val result: HttpResponse = coreClient.networkClient.http().get(request.api) {
                headers {
                    request.headers.forEach { (k, v) ->
                        append(k, v)
                    }
                }
                url {
                    request.parameters.forEach { (k, v) ->
                        parameters.append(k, v)
                    }
                }
            }
            val serverDate: String? = result.headers[HEADER_DATE]
            // Set server offset.
            coreClient.siteConfig.setServerOffset(serverDate)
            CDCResponse().fromJSON(result.body())
        } catch (e: HttpExceptions) {
            CDCResponse().fromHttpException(e)
        }
    }

    /**
     * Perform generic post request.
     */
    open suspend fun post(request: CDCRequest): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }

        return try {
            val result: HttpResponse = coreClient.networkClient.http().post(request.api) {
                headers {
                    request.headers.forEach { (k, v) ->
                        append(k, v)
                    }
                }
                setBody(request.parameters.toEncodedQuery())
            }
            val serverDate: String? = result.headers[HEADER_DATE]
            // Set server offset.
            coreClient.siteConfig.setServerOffset(serverDate)
            CDCResponse().fromJSON(result.body())
        } catch (e: HttpExceptions) {
            CDCResponse().fromHttpException(e)
        }
    }

    /**
     * Generic send request function or REST operation.
     */
    @JvmOverloads
    suspend fun send(
        request: CDCRequest,
        method: String? = HttpMethod.Post.value,
    ): CDCResponse {
        return try {
            when (method!!) {
                HttpMethod.Get.value -> {
                    get(request)
                }

                else -> post(request)
            }
        } catch (e: HttpExceptions) {
            Log.e(LOG_TAG, e.message)
            return CDCResponse().fromHttpException(e)
        }
    }

}
