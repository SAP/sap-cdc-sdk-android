package com.sap.cdc.android.sdk.authentication.session

import com.sap.cdc.android.sdk.authentication.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.extensions.getEncryptedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class SessionSecure(
    private val siteConfig: SiteConfig,
) {
    companion object {

        const val LOG_TAG = "CDC_SessionSecure"

        const val CDC_SESSIONS = "cdc_sessions"
    }

    private var session: Session? = null

    init {
        loadToMem()
    }

    /**
     * Set session object.
     * Given session will replace current session. Both in memory and secured storage.
     */
    fun setSession(session: Session) {
        this.session = session
        secure(session)
    }

    /**
     * Set session from String.
     * Given session will replace current session. Both in memory and secured storage.
     */
    fun setSession(sessionString: String) {
        val session = Session.fromJson(sessionString)
        this.session = session
        secure(session)
    }

    /**
     * Get current session.
     * Will load current secured session (if saved) if not available in memory.s
     */
    fun getSession(): Session? {
        if (this.session == null) {
            loadToMem()
        }
        return this.session
    }

    /**
     * Clears current session heap and remove secured session from encrypted preferences.
     */
    fun clearSession() {
        this.session = null
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            ).edit().remove(CDC_SESSIONS).apply()
    }

    fun updateSecureLevel(level: SessionSecureLevel) {

    }

    /**
     * Write session object (as JSON) in encrypted shared preferences.
     */
    private fun secure(session: Session) {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val json = esp.getString(CDC_SESSIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        sessionMap[siteConfig.apiKey] = session.toJson()

        esp.edit().putString(CDC_SESSIONS, Json.encodeToString(sessionMap)).apply()
    }

    /**
     * Load session from encrypted shared preferences file.
     */
    private fun loadToMem() {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        if (esp.contains(CDC_SESSIONS)) {
            val json = esp.getString(CDC_SESSIONS, null)
            if (json != null) {
                val sessionMap = Json.decodeFromString<Map<String, String>>(json)
                this.session = sessionMap[siteConfig.apiKey]?.let {
                    Session.fromJson(it)
                }
            }
        }
    }

}

enum class SessionSecureLevel(val value: Int) {
    STANDARD(0), BIOMETRIC(1);

    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}