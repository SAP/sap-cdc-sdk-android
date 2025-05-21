package com.sap.cdc.android.sdk.core.api

import android.util.Log
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.utils.toEncodedQuery
import com.sap.cdc.android.sdk.core.network.HttpExceptions
import com.sap.cdc.android.sdk.core.network.RequestQueue
import com.sap.cdc.android.sdk.extensions.isOnline
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CompletableDeferred

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
open class Api(private val coreClient: CoreClient) {

    companion object {
        const val LOG_TAG = "Api"

        const val HEADER_DATE = "date"
    }

    // Use the com.sap.cdc.android.sdk.core.network.RequestQueue singleton directly.
    private val requestQueue = RequestQueue

    /**
     * Block the request queue.
     */
    fun blockQueue() {
        requestQueue.blockQueue = CompletableDeferred()
    }

    /**
     * Unblock the request queue.
     */
    fun unblockQueue() {
        if (!requestQueue.blockQueue.isCompleted) {
            requestQueue.blockQueue.complete(Unit)
        }
    }

    /**
     * Update and resign requests in the queue.
     */
    fun updateAndResignRequests(signRequest: (CDCRequest) -> Unit) {
        requestQueue.updateAndResignRequests(signRequest)
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

        if (!requestQueue.blockQueue.isCompleted) {
            requestQueue.blockQueue.await()
        }

        return try {
            val deferredResponse = CompletableDeferred<CDCResponse>()
            requestQueue.addRequest(request) { req ->
                val result: HttpResponse = coreClient.networkClient.http().get(req.api) {
                    headers {
                        req.headers.map { (k, v) ->
                            headers.append(k, v)
                        }
                    }
                    url {
                        req.parameters.map { (k, v) ->
                            parameters.append(k, v)
                        }
                    }
                }
                val serverDate: String? = result.headers[HEADER_DATE]
                // Set server offset.
                coreClient.siteConfig.setServerOffset(serverDate)
                val cdcResult = CDCResponse().fromJSON(result.body())
                deferredResponse.complete(cdcResult)
                // Forward response.
                result
            }
            deferredResponse.await()
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

        if (!requestQueue.blockQueue.isCompleted) {
            requestQueue.blockQueue.await()
        }
        return try {
            val deferredResponse = CompletableDeferred<CDCResponse>()
            requestQueue.addRequest(request) { req ->
                val result: HttpResponse = coreClient.networkClient.http().post(req.api) {
                    headers {
                        req.headers.map { (k, v) ->
                            headers.append(k, v)
                        }
                    }
                    setBody(req.parameters.toEncodedQuery())
                }
                val serverDate: String? = result.headers[HEADER_DATE]
                // Set server offset.
                coreClient.siteConfig.setServerOffset(serverDate)
                val cdcResult = CDCResponse().fromJSON(result.body())
                deferredResponse.complete(cdcResult)
                // Forward response.
                result
            }
            deferredResponse.await()
        } catch (e: HttpExceptions) {
            CDCResponse().fromHttpException(e)
        }
    }

    /**
     * Perform generic inject request.
     * Injected requests excluded from the blocking state of the queue
     */
    open suspend fun injectRequest(request: CDCRequest): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }
        return try {
            val deferredResponse = CompletableDeferred<CDCResponse>()
            requestQueue.injectRequest(request) { req ->
                val result: HttpResponse = coreClient.networkClient.http().post(req.api) {
                    headers {
                        req.headers.map { (k, v) ->
                            headers.append(k, v)
                        }
                    }
                    setBody(req.parameters.toEncodedQuery())
                }
                val serverDate: String? = result.headers[HEADER_DATE]
                // Set server offset.
                coreClient.siteConfig.setServerOffset(serverDate)
                val cdcResult = CDCResponse().fromJSON(result.body())
                deferredResponse.complete(cdcResult)
                // Forward response.
                result
            }
            deferredResponse.await()
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

