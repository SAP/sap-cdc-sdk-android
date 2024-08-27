package com.sap.cdc.android.sdk.auth.session

import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_GMID
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 */
class SessionService(
    var siteConfig: SiteConfig,
) {
    companion object {
        const val LOG_TAG = "CDC_SessionService"
    }

    var sessionSecure: com.sap.cdc.android.sdk.auth.session.SessionSecure =
        com.sap.cdc.android.sdk.auth.session.SessionSecure(
            siteConfig
        )

    fun validSession() : Boolean = sessionSecure.getSession() != null

    fun getSession() : Session? = sessionSecure.getSession()

    fun clearSession() = sessionSecure.clearSession()

    fun setSession(session: Session) = sessionSecure.setSession(session)

    fun setSession(sessionJson: String) = sessionSecure.setSession(sessionJson)

    /**
     * "Re-Initialize" the SDK with a different site configuration.
     * Support for multiple session is available.
     */
    fun reloadWithSiteConfig(config: SiteConfig) = apply {
        this.siteConfig = config
        this.sessionSecure = com.sap.cdc.android.sdk.auth.session.SessionSecure(siteConfig)
    }

    /**
     * Get the latest GMID value from secured shared preferences file.
     */
    fun gmidLatest(): String? {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        return esp.getString(CDC_GMID, null)
    }

}
