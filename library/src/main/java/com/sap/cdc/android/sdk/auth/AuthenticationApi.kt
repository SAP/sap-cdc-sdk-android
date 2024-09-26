package com.sap.cdc.android.sdk.auth

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
class AuthenticationApi(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : Api(coreClient) {

    override suspend fun genericSend(
        api: String,
        parameters: MutableMap<String, String>,
        method: String?,
        headers: MutableMap<String, String>?
    ): CDCResponse {
        if (!gmidValid()) {
            val ids = getIDs()
        }
        return super.genericSend(api, parameters, method, headers)
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
        if (sessionService.validSession()) {
            val session = sessionService.getSession()
            request.authenticated(session!!.token)
            request.sign(session.secret)
        }
    }

    private suspend fun getIDs(): CDCResponse {
        val response = Api(coreClient).genericSend(EP_SOCIALIZE_GET_IDS)
        val gmidEntity = response.serializeTo<GMIDEntity>()
        if (gmidEntity != null) {
            val esp =
                coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                    CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
                )
            esp.edit().putString(CDC_GMID, gmidEntity.gmid)
                .putLong(CDC_GMID_REFRESH_TS, gmidEntity.refreshTime!!).apply()
        }
        return response
    }

    /**
     * Check GMID validity according to refresh timestamp provided.
     */
    private fun gmidValid(): Boolean {
        val esp =
            coreClient.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        if (!esp.contains(CDC_GMID)) {
            return false
        }
        val gmidRefreshTimestamp = esp.getLong(CDC_GMID_REFRESH_TS, 0L)
        if (gmidRefreshTimestamp == 0L) {
            return false
        }
        val currentTimestamp = System.currentTimeMillis()
        return gmidRefreshTimestamp >= currentTimestamp
    }
}