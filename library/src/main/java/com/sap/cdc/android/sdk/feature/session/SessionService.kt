package com.sap.cdc.android.sdk.feature.session

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_GMID

/**
 * Session management service providing high-level session operations.
 * 
 * Facade for session operations including storage, retrieval, biometric security,
 * and session lifecycle management. Supports multiple site configurations.
 * 
 * @property siteConfig Site configuration for this session service
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see SessionSecure
 * @see Session
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

    /**
     * Checks if a session is available in memory or storage.
     * @return true if session exists, false otherwise
     */
    fun availableSession(): Boolean = sessionSecure.availableSession()

    /**
     * Retrieves the current session.
     * @return Session object or null if no session exists or if biometric locked
     */
    fun getSession(): Session? = sessionSecure.getSession()

    /**
     * Sets a new session and triggers validation if configured.
     * @param session The session to store
     */
    fun setSession(session: Session) = sessionSecure.setSession(session).also {
        // Trigger validation when a new session is set
        validationTrigger?.invoke()
    }

    /**
     * Invalidates the current session, clearing it from memory and storage.
     */
    fun invalidateSession() = sessionSecure.clearSession(invalidate = true)

    /**
     * Clears the session from memory without invalidating storage.
     */
    fun clearSession() = sessionSecure.clearSession(invalidate = false)

    /**
     * Gets the current session security level.
     * @return SessionSecureLevel (STANDARD or BIOMETRIC)
     */
    fun sessionSecureLevel(): SessionSecureLevel = sessionSecure.getSessionSecureLevel()

    /**
     * Secures a session with biometric authentication.
     * @param encryptedSession Base64-encoded encrypted session JSON
     * @param iv Initialization vector for decryption
     */
    fun secureBiometricSession(encryptedSession: String, iv: String) =
        sessionSecure.secureBiometricSession(encryptedSession, iv)

    /**
     * Unlocks a biometrically secured session.
     * @param decryptedSession Decrypted session JSON
     */
    fun unlockBiometricSession(decryptedSession: String) =
        sessionSecure.unlockBiometricSession(decryptedSession)

    /**
     * Checks if the session is biometrically locked.
     * @return true if biometric locked, false otherwise
     */
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
