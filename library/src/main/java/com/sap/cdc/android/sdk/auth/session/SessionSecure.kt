package com.sap.cdc.android.sdk.auth.session

import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
internal class SessionSecure(
    private val siteConfig: SiteConfig,
) {
    companion object {

        const val LOG_TAG = "CDC_SessionSecure"

        const val CDC_SESSIONS = "cdc_sessions"
    }

    // Holding reference to session entity in memory.
    private var sessionEntity: SessionEntity? = null

    init {
        loadToMem()
    }

    /**
     * Load session from encrypted shared preferences file.
     */
    private fun loadToMem() {
        // Get reference to encrypted shared preferences.
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        // Get session map from encrypted shared preferences.
        if (esp.contains(CDC_SESSIONS)) {
            val json = esp.getString(CDC_SESSIONS, null)
            if (json != null) {
                val sessionMap = Json.decodeFromString<Map<String, String>>(json)
                // Get session entity from session map.
                this.sessionEntity = sessionMap[siteConfig.apiKey]?.let {
                    Json.decodeFromString(it)
                }
            }
        }
    }

    /**
     * Set session object.
     * Given session will replace current session. Both in memory and secured storage.
     */
    fun setSession(
        session: Session,
    ) {
        val newSessionEntity = SessionEntity(
            session = Json.encodeToString(session),
        )
        secure(newSessionEntity)
        // Replace memory session with new session.
        this.sessionEntity = newSessionEntity
    }

    /**
     * Set biometric secured session. Biometric session is double encrypted with AES256 GCM and biometric authentication.
     */
    fun secureBiometricSession(
        encryptedSession: String, //Encrypted session JSON (Base64 encoded).
        iv: String // Initialization vector for decrypting the session (Base64 encoded).
    ) {
        val newSessionEntity = SessionEntity(
            session = encryptedSession, secureLevel = SecureLevel(
                encryptionType = SessionSecureLevel.BIOMETRIC, iv = iv
            )
        )
        secure(newSessionEntity)

        // Update memory session with new bio-secured session parameters except the session itself.
        // The original session will remain in memory until locked or cleared.
        this.sessionEntity?.secureLevel = SecureLevel(
            encryptionType = SessionSecureLevel.BIOMETRIC, iv = iv
        )
    }

    /**
     * Unlock biometric secured session.
     * Given decrypted session will replace current session in memory only.
     *
     */
    fun unlockBiometricSession(
        decryptedSession: String, // Decrypted session JSON.
    ) {
        this.sessionEntity?.session = decryptedSession
    }

    fun availableSession(): Boolean {
        return this.sessionEntity != null
    }

    /**
     * Get current session.
     * If session is secured with biometric authentication, null will be returned (as it is not possible to decrypt it
     * without user biometric authentication).
     */
    fun getSession(): Session? {
        if (this.sessionEntity == null) {
            loadToMem()
        }
        if (this.sessionEntity == null) {
            return null
        }
        if (this.sessionEntity?.secureLevel?.encryptionType == SessionSecureLevel.BIOMETRIC) {
            return null
        }
        return Json.decodeFromString(this.sessionEntity?.session!!)
    }

    /**
     * Clears current session heap and remove secured session entry from encrypted preferences session map
     */
    fun clearSession(invalidate: Boolean = false) {
        this.sessionEntity = null
        if (invalidate) {
            // Invalidate session in encrypted shared preferences.
            secure(null)
        }
    }

    /**
     * Write session object (as JSON) in encrypted shared preferences.
     */
    private fun secure(sessionEntity: SessionEntity?) {
        // Get reference to encrypted shared preferences.
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        // Get current session map.
        val json = esp.getString(CDC_SESSIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        // Update session map.
        if (sessionEntity == null) {
            sessionMap.remove(siteConfig.apiKey)
        } else {
            sessionMap[siteConfig.apiKey] = Json.encodeToString(sessionEntity)
        }
        // Write session map back to encrypted shared preferences.
        esp.edit().putString(CDC_SESSIONS, Json.encodeToString(sessionMap)).apply()
    }

    /**
     * Get the session encryption level.
     */
    fun getSessionSecureLevel(): SessionSecureLevel {
        if (sessionEntity == null) {
            return SessionSecureLevel.STANDARD
        }
        return sessionEntity!!.secureLevel.encryptionType
    }

    /**
     * Check if session is biometric locked.
     * Will try to decode session object, if failed - session is biometric locked.
     */
    fun biometricLocked(): Boolean {
        if (!availableSession()) {
            return false
        }
        if (sessionEntity?.secureLevel?.encryptionType == SessionSecureLevel.STANDARD) {
            return false
        }
        try {
            Json.decodeFromString<Session>(sessionEntity?.session!!)
            return false
        } catch (e: Exception) {
            return true
        }
    }

}