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
class SessionSecure(
    private val siteConfig: SiteConfig,
) {
    companion object {

        const val LOG_TAG = "CDC_SessionSecure"

        const val CDC_SESSIONS = "cdc_sessions"
    }

    private var session: com.sap.cdc.android.sdk.auth.session.Session? = null

    init {
        loadToMem()
    }

    /**
     * Set session object.
     * Given session will replace current session. Both in memory and secured storage.
     */
    fun setSession(session: com.sap.cdc.android.sdk.auth.session.Session) {
        this.session = session
        secure(session)
    }

    /**
     * Set session from String.
     * Given session will replace current session. Both in memory and secured storage.
     */
    fun setSession(sessionString: String) {
        val session = com.sap.cdc.android.sdk.auth.session.Session.Companion.fromJson(sessionString)
        this.session = session
        secure(session)
    }

    /**
     * Get current session.
     * Will load current secured session (if saved) if not available in memory.s
     */
    fun getSession(): com.sap.cdc.android.sdk.auth.session.Session? {
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
            ).edit().remove(com.sap.cdc.android.sdk.auth.session.SessionSecure.Companion.CDC_SESSIONS).apply()
    }

    fun updateSecureLevel(level: com.sap.cdc.android.sdk.auth.session.SessionSecureLevel) {

    }

    /**
     * Write session object (as JSON) in encrypted shared preferences.
     */
    private fun secure(session: com.sap.cdc.android.sdk.auth.session.Session) {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val json = esp.getString(com.sap.cdc.android.sdk.auth.session.SessionSecure.Companion.CDC_SESSIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        sessionMap[siteConfig.apiKey] = session.toJson()

        esp.edit().putString(com.sap.cdc.android.sdk.auth.session.SessionSecure.Companion.CDC_SESSIONS, Json.encodeToString(sessionMap)).apply()
    }

    /**
     * Load session from encrypted shared preferences file.
     */
    private fun loadToMem() {
        val esp =
            siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        if (esp.contains(com.sap.cdc.android.sdk.auth.session.SessionSecure.Companion.CDC_SESSIONS)) {
            val json = esp.getString(com.sap.cdc.android.sdk.auth.session.SessionSecure.Companion.CDC_SESSIONS, null)
            if (json != null) {
                val sessionMap = Json.decodeFromString<Map<String, String>>(json)
                this.session = sessionMap[siteConfig.apiKey]?.let {
                    com.sap.cdc.android.sdk.auth.session.Session.Companion.fromJson(it)
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