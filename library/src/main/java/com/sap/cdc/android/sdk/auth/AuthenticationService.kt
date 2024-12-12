package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthenticationService(
    val siteConfig: SiteConfig,
) {
    val coreClient: CoreClient = CoreClient(siteConfig)
    val sessionService: SessionService = SessionService(siteConfig)

    companion object {
        const val CDC_AUTHENTICATION_SERVICE_SECURE_PREFS =
            "cdc_secure_prefs_authentication_service"

        const val CDC_GMID = "cdc_gmid"
        const val CDC_GMID_REFRESH_TS = "cdc_gmid_refresh_ts"
    }

    fun authenticate(): IAuthApis =
        AuthApis(coreClient, sessionService)

    fun resolve(): IAuthResolvers =
        AuthResolvers(coreClient, sessionService)

    fun get(): IAuthApisGet =
        AuthApisGet(coreClient, sessionService)

    fun set(): IAuthApisSet =
        AuthApisSet(coreClient, sessionService)

    fun session(): IAuthSession = AuthSession(
        sessionService
    )

}