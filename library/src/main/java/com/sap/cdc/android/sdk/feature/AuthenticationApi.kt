package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.AndroidResourceProvider
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.ResourceProvider
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.CDCRequest
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.InvalidGMIDResponseEvaluator
import com.sap.cdc.android.sdk.core.api.model.GMIDEntity
import com.sap.cdc.android.sdk.extensions.prepareApiUrl
import com.sap.cdc.android.sdk.feature.session.SessionService
import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Internal API client for authenticated CDC requests.
 * 
 * Handles CDC API communication with automatic GMID management, request signing,
 * and retry logic for invalidated identifiers.
 * 
 * ## Key Features
 * - Automatic GMID (Gigya Mobile ID) validation and renewal
 * - Request signing for authenticated sessions
 * - Retry logic on GMID invalidation
 * - Thread-safe GMID operations with mutex synchronization
 * - Comprehensive exception handling with typed error responses
 * 
 * ## Usage
 * This is an internal class used by authentication flows. Developers typically don't
 * interact with this class directly, but rather through higher-level APIs like
 * `AuthenticationService.authenticate()` or `AuthenticationService.account()`.
 * 
 * ```kotlin
 * // Internal usage example
 * val authApi = AuthenticationApi(coreClient, sessionService)
 * val response = authApi.send(
 *     api = "accounts.getAccountInfo",
 *     parameters = mutableMapOf("include" to "profile,data")
 * )
 * ```
 * 
 * @param coreClient Core API client for network operations
 * @param sessionService Service for managing user sessions
 * @param resourceProvider Provider for accessing encrypted storage (defaults to Android resources)
 * @see AuthenticationService
 * @see CDCRequest
 * @see CDCResponse
 */
class AuthenticationApi(
    private val coreClient: CoreClient,
    private val sessionService: SessionService,
    private val resourceProvider: ResourceProvider = AndroidResourceProvider(coreClient.siteConfig.applicationContext)
) : Api(coreClient) {

    companion object {
        const val LOG_TAG = "AuthenticationApi"
        const val PARAM_GMID = "gmid"
        const val MAX_RETRY_COUNT = 2
        
        // Singleton mutex to synchronize GMID operations across all AuthenticationApi instances
        private val gmidMutex = Mutex()
    }

    /**
     * Sends an authenticated API request to the CDC service with comprehensive exception handling.
     *
     * This method handles the complete lifecycle of CDC API requests including:
     * - GMID validation and renewal
     * - Request signing for authenticated sessions
     * - Automatic retry on GMID invalidation
     * - Graceful handling of coroutine cancellations and timeouts
     *
     * @param api The CDC API endpoint to call (e.g., "accounts.getAccountInfo")
     * @param parameters Optional map of request parameters. Defaults to empty map if null.
     * @param method HTTP method to use. Defaults to POST if null.
     * @param headers Optional map of additional headers. Defaults to empty map if null.
     *
     * @return CDCResponse containing either:
     *         - Success response with data from the CDC service
     *         - Error response with code 504001 for timeout cancellations
     *         - Error response with code 200001 for request cancellations (including JobCancellationException)
     *         - Error response with code 500001 for other unexpected errors
     *
     * @throws Nothing - All exceptions are caught and converted to error CDCResponse objects
     *
     * @since 1.0.0
     */
    suspend fun send(
        api: String,
        parameters: MutableMap<String, String>? = mutableMapOf(),
        method: String? = HttpMethod.Post.value,
        headers: MutableMap<String, String>? = mutableMapOf()
    ): CDCResponse {
        return try {
            // Synchronize GMID validation and fetching across all coroutines
            gmidMutex.withLock {
                if (!isLocalGmidValid()) {
                    CDCDebuggable.log(LOG_TAG, "Local GMID not available or invalid - requesting new GMID")
                    val ids = fetchIDs()
                    if (ids.isError()) {
                        CDCDebuggable.log(LOG_TAG, "getIDs error: ${ids.errorCode()}")
                        return ids
                    }
                }
            }

            // At this point, GMID is guaranteed valid
            parameters!![PARAM_GMID] = sessionService.gmidLatest()
            val cdcRequest = buildCDCRequest(api, parameters, method, headers)
            var response = send(request = cdcRequest, method)

            // Handle GMID invalidation with retry
            if (response.isError() && InvalidGMIDResponseEvaluator().evaluate(response)) {
                CDCDebuggable.log(LOG_TAG, "Remote GMID evaluation failed - fetching new GMID")
                
                // Synchronize GMID refresh to avoid duplicate fetches
                gmidMutex.withLock {
                    // Double-check: another coroutine might have already refreshed
                    val currentGmid = sessionService.gmidLatest()
                    if (cdcRequest.parameters[PARAM_GMID] == currentGmid) {
                        // Still stale, we need to refresh
                        CDCDebuggable.log(LOG_TAG, "GMID still stale, fetching new one")
                        val ids = retryFetchIDs()
                        if (ids.isError()) {
                            CDCDebuggable.log(LOG_TAG, "getIDs failed after $MAX_RETRY_COUNT retries")
                            return response
                        }
                    } else {
                        CDCDebuggable.log(LOG_TAG, "GMID already refreshed by another coroutine")
                    }
                }
                
                // Retry with fresh GMID
                cdcRequest.parameters[PARAM_GMID] = sessionService.gmidLatest()
                signRequestIfNeeded(cdcRequest)
                response = send(request = cdcRequest, method)
            }
            
            response
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CDCDebuggable.log(LOG_TAG, "Request timed out: ${e.message}")
            CDCResponse().fromError(504002, "Request Timeout", "A timeout that was defined in the request is reached.")
        } catch (e: kotlinx.coroutines.CancellationException) {
            CDCDebuggable.log(LOG_TAG, "Request was cancelled: ${e.message}")
            CDCResponse().fromError(200001, "Operation canceled", null)
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "Unexpected error in network request: ${e.message}")
            CDCResponse().fromError(500001, "General Server error", e.message)
        }
    }

    /**
     * Executes a GET request with authentication and comprehensive exception handling.
     *
     * This method overrides the base Api.get() method to add:
     * - Automatic request signing for authenticated sessions
     * - Graceful handling of coroutine cancellations and timeouts
     * - Consistent error response formatting
     *
     * @param request The CDCRequest object containing the request details including URL, parameters, and headers
     *
     * @return CDCResponse containing either:
     *         - Success response with data from the CDC service
     *         - Error response with code 504001 for timeout cancellations
     *         - Error response with code 200001 for request cancellations (including JobCancellationException)
     *         - Error response with code 500001 for other unexpected errors
     *
     * @throws Nothing - All exceptions are caught and converted to error CDCResponse objects
     *
     * @see Api.get
     * @since 1.0.0
     */
    override suspend fun get(request: CDCRequest): CDCResponse {
        return try {
            signRequestIfNeeded(request)
            super.get(request)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CDCDebuggable.log(LOG_TAG, "GET request timed out: ${e.message}")
            CDCResponse().fromError(504001, "Timeout", "The GET request timed out and was cancelled")
        } catch (e: kotlinx.coroutines.CancellationException) {
            CDCDebuggable.log(LOG_TAG, "GET request was cancelled: ${e.message}")
            CDCResponse().fromError(200001, "Operation canceled", "The GET request was cancelled before completion")
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "Unexpected error in GET request: ${e.message}")
            CDCResponse().fromError(500001, "General Server error", "An unexpected error occurred during the GET request: ${e.message}")
        }
    }

    /**
     * Executes a POST request with authentication and comprehensive exception handling.
     *
     * This method overrides the base Api.post() method to add:
     * - Automatic request signing for authenticated sessions
     * - Graceful handling of coroutine cancellations and timeouts
     * - Consistent error response formatting
     *
     * @param request The CDCRequest object containing the request details including URL, parameters, and headers
     *
     * @return CDCResponse containing either:
     *         - Success response with data from the CDC service
     *         - Error response with code 504001 for timeout cancellations
     *         - Error response with code 200001 for request cancellations (including JobCancellationException)
     *         - Error response with code 500001 for other unexpected errors
     *
     * @throws Nothing - All exceptions are caught and converted to error CDCResponse objects
     *
     * @see Api.post
     * @since 1.0.0
     */
    override suspend fun post(request: CDCRequest): CDCResponse {
        return try {
            signRequestIfNeeded(request)
            super.post(request)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CDCDebuggable.log(LOG_TAG, "POST request timed out: ${e.message}")
            CDCResponse().fromError(504001, "Timeout", "The POST request timed out and was cancelled")
        } catch (e: kotlinx.coroutines.CancellationException) {
            CDCDebuggable.log(LOG_TAG, "POST request was cancelled: ${e.message}")
            CDCResponse().fromError(200001, "Operation canceled", "The POST request was cancelled before completion")
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "Unexpected error in POST request: ${e.message}")
            CDCResponse().fromError(500001, "General Server error", "An unexpected error occurred during the POST request: ${e.message}")
        }
    }

    private fun signRequestIfNeeded(request: CDCRequest) {
        CDCDebuggable.log(LOG_TAG, "Signing request if needed")
        if (sessionService.availableSession()) {
            sessionService.getSession()?.let { session ->
                request.authenticated(session.token)
                request.sign(session.secret)
            }
        }
    }

    /**
     * Get IDs from the server.
     */
    private suspend fun fetchIDs(): CDCResponse {
        CDCDebuggable.log(LOG_TAG, "Fetching IDs")
        val cdcRequest =
            buildCDCRequest(AuthEndpoints.Companion.EP_SOCIALIZE_GET_IDS, mutableMapOf(), HttpMethod.Post.value)
        val idsResponse = send(cdcRequest)
        if (!idsResponse.isError()) {
            // Deserialize the response to GMIDEntity
            val gmidEntity = idsResponse.serializeTo<GMIDEntity>()
            CDCDebuggable.log(LOG_TAG, "gmid: ${gmidEntity?.gmid}")
            // Save the GMID to secure preferences
            if (gmidEntity != null) {
                val esp = resourceProvider.getEncryptedSharedPreferences(
                    AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
                )
                val editor = esp.edit()
                editor.putString(AuthenticationService.Companion.CDC_GMID, gmidEntity.gmid)
                editor.putLong(AuthenticationService.Companion.CDC_GMID_REFRESH_TS, gmidEntity.refreshTime!!)
                editor.apply()
            }
            CDCDebuggable.log(LOG_TAG, "gmidEntity: $gmidEntity")
        }
        return idsResponse
    }

    /**
     * Retry fetching IDs if the initial attempt fails.
     * This method will retry up to MAX_RETRY_COUNT times.
     */
    private suspend fun retryFetchIDs(): CDCResponse {
        var retryCount = 0
        var ids: CDCResponse
        do {
            ids = fetchIDs()
            if (!ids.isError()) break
            retryCount++
            CDCDebuggable.log(LOG_TAG, "Retrying getIDs... Attempt: $retryCount")
        } while (retryCount < MAX_RETRY_COUNT)
        return ids
    }

    /**
     * Check if the local GMID is valid.
     */
    private fun isLocalGmidValid(): Boolean {
        CDCDebuggable.log(LOG_TAG, "Validating local GMID")
        val prefs = resourceProvider.getEncryptedSharedPreferences(
            AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val gmid = prefs.getString(AuthenticationService.Companion.CDC_GMID, null)
        val refreshTimestamp = prefs.getLong(AuthenticationService.Companion.CDC_GMID_REFRESH_TS, 0L)
        return gmid != null && refreshTimestamp > System.currentTimeMillis()
    }

    /**
     * Builds a CDCRequest object.
     */
    private fun buildCDCRequest(
        api: String,
        parameters: MutableMap<String, String>,
        method: String?,
        headers: MutableMap<String, String>? = mutableMapOf()
    ): CDCRequest {
        return CDCRequest(coreClient.siteConfig)
            .method(method ?: HttpMethod.Post.value)
            .api(api.prepareApiUrl(coreClient.siteConfig))
            .parameters(parameters)
            .timestamp(coreClient.siteConfig.getServerTimestamp())
            .headers(headers ?: mutableMapOf())
    }

}
