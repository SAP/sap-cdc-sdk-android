package com.sap.cdc.android.sdk.feature

import androidx.core.content.edit
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.CDCRequest
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.InvalidGMIDResponseEvaluator
import com.sap.cdc.android.sdk.core.api.model.GMIDEntity
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.extensions.prepareApiUrl
import com.sap.cdc.android.sdk.feature.session.SessionService
import io.ktor.http.HttpMethod


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Base class for authentication APIs.
 */
class AuthenticationApi(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : Api(coreClient) {

    companion object {
        const val LOG_TAG = "AuthenticationApi"
        const val PARAM_GMID = "gmid"
        const val MAX_RETRY_COUNT = 2
    }

    suspend fun send(
        api: String,
        parameters: MutableMap<String, String>? = mutableMapOf(),
        method: String? = HttpMethod.Post.value,
        headers: MutableMap<String, String>? = mutableMapOf()
    ): CDCResponse {
        if (!isLocalGmidValid()) {
            CDCDebuggable.log(LOG_TAG, "Local GMID not available or invalid - requesting new GMID")
            val ids = fetchIDs()
            if (ids.isError()) CDCDebuggable.log(LOG_TAG, "getIDs error: ${ids.errorCode()}")
        }

        parameters!![PARAM_GMID] = sessionService.gmidLatest()
        val cdcRequest = buildCDCRequest(api, parameters, method, headers)
        val response = send(request = cdcRequest, method)

        if (response.isError() && InvalidGMIDResponseEvaluator().evaluate(response)) {
            CDCDebuggable.log(
                LOG_TAG,
                "Remote GMID evaluation failed - blocking queue to request new GMID"
            )
            blockQueue()

            val ids = retryFetchIDs()
            if (ids.isError()) {
                CDCDebuggable.log(LOG_TAG, "getIDs failed after $MAX_RETRY_COUNT retries")
                return response
            }

            CDCDebuggable.log(LOG_TAG, "Re-signing all requests in queue")
            updateAndResignRequests { signRequestIfNeeded(it) }

            CDCDebuggable.log(LOG_TAG, "Injecting original request")
            // Make sure original request is going out with new GMID..
            cdcRequest.parameters[PARAM_GMID] = sessionService.gmidLatest()
            // Re-Sign original request
            signRequestIfNeeded(cdcRequest)

            val newResponse = injectRequest(cdcRequest)

            unblockQueue()
            return newResponse
        }
        return response
    }

    override suspend fun get(request: CDCRequest): CDCResponse {
        signRequestIfNeeded(request)
        return super.get(request)
    }

    override suspend fun post(request: CDCRequest): CDCResponse {
        signRequestIfNeeded(request)
        return super.post(request)
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
    private suspend fun fetchIDs(inject: Boolean = false): CDCResponse {
        CDCDebuggable.log(LOG_TAG, "Fetching IDs")
        val cdcRequest =
            buildCDCRequest(AuthEndpoints.Companion.EP_SOCIALIZE_GET_IDS, mutableMapOf(), HttpMethod.Post.value)
        val idsResponse = if (inject) injectRequest(cdcRequest) else send(cdcRequest)
        if (!idsResponse.isError()) {
            // Deserialize the response to GMIDEntity
            val gmidEntity = idsResponse.serializeTo<GMIDEntity>()
            CDCDebuggable.log(LOG_TAG, "gmid: ${gmidEntity?.gmid}")
            // Save the GMID to secure preferences
            if (gmidEntity != null) {
                val esp =
                    coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                        AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
                    )
                esp.edit {
                    putString(AuthenticationService.Companion.CDC_GMID, gmidEntity.gmid)
                        .putLong(AuthenticationService.Companion.CDC_GMID_REFRESH_TS, gmidEntity.refreshTime!!)
                }
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
            ids = fetchIDs(inject = true)
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
        val prefs = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
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