package com.sap.cdc.android.sdk.authentication

import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_SOCIALIZE_GET_IDS
import com.sap.cdc.android.sdk.authentication.model.GMIDEntity
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.extensions.getEncryptedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            AuthenticationApi(coreClient, sessionService).getIDs()
        }
    }

    fun authenticate(): IAuthApis = AuthApis(coreClient, sessionService)

    fun resolve(): IAuthResolvers = AuthResolvers(coreClient, sessionService)

    fun get(): IAuthApis = AuthApis(coreClient, sessionService)

    fun set(): IAuthApis = AuthApis(coreClient, sessionService)


}