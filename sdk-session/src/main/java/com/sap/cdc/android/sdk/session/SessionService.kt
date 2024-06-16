package com.sap.cdc.android.sdk.session

import com.sap.cdc.android.sdk.session.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.session.network.NetworkClient
import com.sap.cdc.android.sdk.session.session.SessionSecure

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class SessionService(
    var siteConfig: SiteConfig,
) {
    companion object {
        const val LOG_TAG = "SessionService"
        const val CDC_SECURE_PREFS = "cdc_secure_prefs"
        const val CDC_GMID = "cdc_gmid"
        const val CDC_GMID_REFRESH_TS = "cdc_gmid_refresh_ts"
    }

    lateinit var networkClient: NetworkClient

    var sessionSecure: SessionSecure = SessionSecure(
        siteConfig
    )

    /**
     * "Re-Initialize" the SDK with a different site configuration.
     * Support for multiple session is available.
     */
    fun reloadWithSiteConfig(config: SiteConfig) = apply {
        this.siteConfig = config
        this.sessionSecure = SessionSecure(siteConfig)
    }

    /**
     * Create new instance of the network client.
     */
    fun newClient() = apply {
        this.networkClient = NetworkClient()
    }

    /**
     * Inject an instance of the network client.
     */
    fun withClient(networkClient: NetworkClient) = apply {
        this.networkClient = networkClient
    }

    /**
     * Check GMID validity according to refresh timestamp provided.
     */
    fun gmidValid(): Boolean {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(CDC_SECURE_PREFS)
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

    fun gmidLatest(): String? {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(CDC_SECURE_PREFS)
        return esp.getString(CDC_GMID, null)
    }

}
