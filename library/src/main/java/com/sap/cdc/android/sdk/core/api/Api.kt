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
 * Base API client for SAP CDC (Customer Data Cloud) REST API operations.
 * 
 * This class provides the core HTTP communication layer for interacting with SAP CDC services.
 * It handles network connectivity checks, HTTP GET/POST requests, server time synchronization,
 * and error handling for all CDC API operations.
 * 
 * The Api class:
 * - Manages HTTP GET and POST requests to CDC endpoints
 * - Handles network connectivity validation before requests
 * - Extracts and synchronizes server timestamps from response headers
 * - Converts HTTP responses to CDCResponse objects
 * - Provides a unified send() method for flexible REST operations
 * 
 * This class is designed to be extended by feature-specific API clients that implement
 * higher-level CDC operations (authentication, account management, etc.).
 * 
 * @property coreClient The CoreClient instance providing site configuration and network client access
 * 
 * @constructor Creates an Api instance with the specified CoreClient.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see com.sap.cdc.android.sdk.core.api.CDCRequest
 * @see com.sap.cdc.android.sdk.core.api.CDCResponse
 * @see com.sap.cdc.android.sdk.core.CoreClient
 */
open class Api(private val coreClient: CoreClient) {

    companion object {
        /**
         * Log tag for API-related logging operations.
         */
        const val LOG_TAG = "Api"

        /**
         * HTTP header name for extracting server date/time information.
         * Used for server time synchronization.
         */
        const val HEADER_DATE = "date"
    }

    /**
     * Checks if network connectivity is available.
     * 
     * This method verifies that the device has an active network connection before
     * attempting to make API requests. It's automatically called by [get], [post],
     * and [send] methods to prevent unnecessary request attempts when offline.
     * 
     * @return true if a network connection is available, false otherwise
     */
    fun networkAvailable(): Boolean =
        coreClient.siteConfig.applicationContext.isOnline()

    /**
     * Performs a generic HTTP GET request to a CDC API endpoint.
     * 
     * This method executes an HTTP GET request with the provided request parameters and headers.
     * It automatically:
     * - Checks network connectivity before making the request
     * - Appends request parameters to the URL query string
     * - Extracts the server date header and updates the server time offset
     * - Converts the HTTP response to a CDCResponse object
     * - Handles HTTP exceptions and network errors
     * 
     * @param request The CDCRequest containing the API endpoint, parameters, and headers
     * @return CDCResponse containing the parsed response data or error information
     * 
     * @see CDCRequest
     * @see CDCResponse
     * @see networkAvailable
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
     * Performs a generic HTTP POST request to a CDC API endpoint.
     * 
     * This method executes an HTTP POST request with the provided request parameters and headers.
     * It automatically:
     * - Checks network connectivity before making the request
     * - Encodes request parameters as form data in the request body
     * - Extracts the server date header and updates the server time offset
     * - Converts the HTTP response to a CDCResponse object
     * - Handles HTTP exceptions and network errors
     * 
     * @param request The CDCRequest containing the API endpoint, parameters, and headers
     * @return CDCResponse containing the parsed response data or error information
     * 
     * @see CDCRequest
     * @see CDCResponse
     * @see networkAvailable
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
     * Sends a generic HTTP request with the specified method to a CDC API endpoint.
     * 
     * This is a convenience method that routes requests to either [get] or [post] based
     * on the specified HTTP method. It provides a unified interface for making CDC API
     * requests regardless of the HTTP verb.
     * 
     * Supported HTTP methods:
     * - GET: Routes to [get] method
     * - POST (default): Routes to [post] method
     * - Other methods: Default to POST
     * 
     * @param request The CDCRequest containing the API endpoint, parameters, and headers
     * @param method The HTTP method to use. Defaults to POST if not specified.
     *               Use HttpMethod.Get.value for GET requests or HttpMethod.Post.value for POST.
     * @return CDCResponse containing the parsed response data or error information
     * 
     * @see get
     * @see post
     * @see CDCRequest
     * @see CDCResponse
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
