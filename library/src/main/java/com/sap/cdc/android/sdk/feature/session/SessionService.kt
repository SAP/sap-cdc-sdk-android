package com.sap.cdc.android.sdk.feature.session

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_GMID

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 */
class SessionService(
    var siteConfig: SiteConfig,
) {
    companion object {
        const val LOG_TAG = "SessionService"
    }

    init {
        CDCDebuggable.log(LOG_TAG, "Initialized SessionService with siteConfig: $siteConfig")
    }

    private var sessionSecure: SessionSecure =
        SessionSecure(
            siteConfig
        )
    
    private var validationTrigger: (() -> Unit)? = null

    fun availableSession(): Boolean = sessionSecure.availableSession()

    fun getSession(): Session? = sessionSecure.getSession()

    fun setSession(session: Session) = sessionSecure.setSession(session).also {
        // Trigger validation when a new session is set
        validationTrigger?.invoke()
    }

    fun invalidateSession() = sessionSecure.clearSession(invalidate = true)

    fun clearSession() = sessionSecure.clearSession(invalidate = false)

    fun sessionSecureLevel(): SessionSecureLevel = sessionSecure.getSessionSecureLevel()

    fun secureBiometricSession(encryptedSession: String, iv: String) =
        sessionSecure.secureBiometricSession(encryptedSession, iv)

    fun unlockBiometricSession(decryptedSession: String) =
        sessionSecure.unlockBiometricSession(decryptedSession)

    fun biometricLocked(): Boolean = sessionSecure.biometricLocked()

    /**
     * "Re-Initialize" the SDK with a different site configuration.
     * Support for multiple session is available.
     */
    fun reloadWithSiteConfig(config: SiteConfig) = apply {
        this.siteConfig = config
        this.sessionSecure = SessionSecure(siteConfig)
    }

    /**
     * Get the latest GMID value from secured shared preferences file.
     */
    fun gmidLatest(): String {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        return esp.getString(CDC_GMID, "") ?: ""
    }

    /**
     * Sets a callback to be triggered when a new session is established.
     * Used internally by AuthenticationService to enable session validation.
     */
    internal fun setValidationTrigger(trigger: () -> Unit) {
        validationTrigger = trigger
    }

}
