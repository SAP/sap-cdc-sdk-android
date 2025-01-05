package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_GET_IDS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_GMID
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_GMID_REFRESH_TS
import com.sap.cdc.android.sdk.auth.model.GMIDEntity
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.CDCRequest
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences


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
    }

    /**
     * Generic send request method.
     */
    override suspend fun genericSend(
        api: String,
        parameters: MutableMap<String, String>,
        method: String?,
        headers: MutableMap<String, String>?
    ): CDCResponse {
        if (!gmidValid()) {
            val ids = getIDs()
            if (ids.isError()) {
                CDCDebuggable.log(LOG_TAG, "getIds error: ${ids.errorCode()}")
            }
        }
        parameters["gmid"] = sessionService.gmidLatest()
        return super.genericSend(api, parameters, method, headers)
    }

    /**
     * Perform get request.
     */
    override suspend fun get(request: CDCRequest): CDCResponse {
        signRequestIfNeeded(request)
        return super.get(request)
    }

    /**
     * Perform post request.
     */
    override suspend fun post(request: CDCRequest): CDCResponse {
        signRequestIfNeeded(request)
        return super.post(request)
    }

    /**
     * Sign the request if session is valid.
     */
    private fun signRequestIfNeeded(request: CDCRequest) {
        CDCDebuggable.log(LOG_TAG, "signRequestIfNeeded")
        if (sessionService.availableSession()) {
            val session = sessionService.getSession()
            request.authenticated(session!!.token)
            request.sign(session.secret)
        }
    }

    /**
     * Request GMID from server required for authentication/security.
     */
    private suspend fun getIDs(): CDCResponse {
        CDCDebuggable.log(LOG_TAG, "getIDs")
        val response = Api(coreClient).genericSend(EP_SOCIALIZE_GET_IDS)
        val gmidEntity = response.serializeTo<GMIDEntity>()
        CDCDebuggable.log(LOG_TAG, "gmid: ${gmidEntity?.gmid}")
        if (gmidEntity != null) {
            val esp =
                coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                    CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
                )
            esp.edit().putString(CDC_GMID, gmidEntity.gmid)
                .putLong(CDC_GMID_REFRESH_TS, gmidEntity.refreshTime!!).apply()
        }
        CDCDebuggable.log(LOG_TAG, "gmidEntity: $gmidEntity")
        return response
    }

    /**
     * Check GMID validity according to refresh timestamp provided.
     */
    private fun gmidValid(): Boolean {
        CDCDebuggable.log(LOG_TAG, "gmidValid")
        val esp =
            coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        if (!esp.contains(CDC_GMID)) {
            CDCDebuggable.log(LOG_TAG, "Invalid - no gmid found")
            return false
        }
        val gmidRefreshTimestamp = esp.getLong(CDC_GMID_REFRESH_TS, 0L)
        if (gmidRefreshTimestamp == 0L) {
            CDCDebuggable.log(LOG_TAG, "Invalid - no refresh timestamp found")
            return false
        }
        val currentTimestamp = System.currentTimeMillis()
        if (gmidRefreshTimestamp < currentTimestamp) {
            CDCDebuggable.log(LOG_TAG, "Invalid - refresh timestamp expired")
            return false
        }
        return true
    }
}