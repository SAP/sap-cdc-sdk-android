package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthenticationService(val sessionService: SessionService) {

    val coreClient: CoreClient = CoreClient(sessionService.siteConfig)

    companion object {
        const val CDC_AUTHENTICATION_SERVICE_SECURE_PREFS =
            "cdc_secure_prefs_authentication_service"

        const val CDC_GMID = "cdc_gmid"
        const val CDC_GMID_REFRESH_TS = "cdc_gmid_refresh_ts"
    }

    init {
        // Check async/await.
        CoroutineScope(Dispatchers.IO).launch {
            com.sap.cdc.android.sdk.auth.AuthenticationApi(coreClient, sessionService).getIDs()
        }
    }

    fun authenticate(): com.sap.cdc.android.sdk.auth.IAuthApis =
        com.sap.cdc.android.sdk.auth.AuthApis(coreClient, sessionService)

    fun resolve(): com.sap.cdc.android.sdk.auth.IAuthResolvers =
        com.sap.cdc.android.sdk.auth.AuthResolvers(coreClient, sessionService)

    fun get(): com.sap.cdc.android.sdk.auth.IAuthApisGet =
        com.sap.cdc.android.sdk.auth.AuthApisGet(coreClient, sessionService)

    fun set(): com.sap.cdc.android.sdk.auth.IAuthApisSet =
        com.sap.cdc.android.sdk.auth.AuthApisSet(coreClient, sessionService)



}