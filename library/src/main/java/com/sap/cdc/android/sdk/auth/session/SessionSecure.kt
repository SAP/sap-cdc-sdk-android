package com.sap.cdc.android.sdk.auth.session

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.CDCMessageEventBus
import com.sap.cdc.android.sdk.SessionEvent
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

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
        const val CDC_SESSION_EXPIRATIONS = "cdc_session_expirations"

        const val CDC_SESSION_EXPIRATION_WORKER = "cdc_session_expiration_worker"
    }

    // Holding reference to session entity in memory.
    private var sessionEntity: SessionEntity? = null

    init {
        subscribeToSessionEvents()
        loadToMem()
    }

    private fun subscribeToSessionEvents() {
        CDCDebuggable.log(LOG_TAG, "Subscribing to session events.")
        // Create a new CoroutineScope with a Job
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Main + job)

        CDCMessageEventBus.initializeSessionScope(scope)
        CDCMessageEventBus.subscribeToSessionEvents {
            when (it) {
                is SessionEvent.ExpiredSession -> {
                    CDCDebuggable.log(LOG_TAG, "Invalidate session event received from bus.")
                    clearSession(true)
                }

                is SessionEvent.VerifySession -> {
                    CDCDebuggable.log(LOG_TAG, "Verify session event received from bus.")
                    // Verify session
                }
            }
        }
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
                CDCDebuggable.log(LOG_TAG, "Session loaded to memory. $sessionEntity")

                // Enqueue session expiration worker if the session has expiration time.
                if (sessionEntity != null) {
                    val session = Json.decodeFromString<Session>(this.sessionEntity?.session!!)
                    // Check for session expiration.
                    if (session.expiration != null && session.expiration!! > 0) {
                        CDCDebuggable.log(
                            LOG_TAG,
                            "Session has expiration (${session.expiration}) time. Enqueue worker."
                        )
                        cancelRunningSessionExpirationWorker()
                        val expirationTime = getExpirationTime()
                        CDCDebuggable.log(LOG_TAG, "Expiration time to enqueue: $expirationTime")
                        if (expirationTime != null && expirationTime > 0) {
                            enqueueSessionExpirationWorker(expirationTime - System.currentTimeMillis())
                        }
                    }
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
        CDCDebuggable.log(LOG_TAG, "Setting session in memory: $session")
        val newSessionEntity = SessionEntity(
            session = Json.encodeToString(session),
        )
        secureSessionWith(newSessionEntity)
        // Replace memory session with new session.
        this.sessionEntity = newSessionEntity

        // Enqueue session expiration worker if the session has expiration time.
        if (session.expiration != null && session.expiration!! > 0) {
            CDCDebuggable.log(
                LOG_TAG,
                "Session has expiration (${session.expiration}) time. Enqueue worker."
            )
            // Determine time for session expiration.
            val expirationTime =
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(session.expiration!!)
            // Secure expiration time in encrypted shared preferences.
            secureExpirationWith(expirationTime)
            enqueueSessionExpirationWorker(expirationTime - System.currentTimeMillis())
        }
    }

    /**
     * Secure session expiration time in encrypted shared preferences.
     * SDK can handle saving session/expirations according to Site API key.
     * Multiple session support is enabled by default.
     */
    private fun secureExpirationWith(expirationTime: Long) {
        // Get reference to encrypted shared preferences.
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val json = esp.getString(CDC_SESSION_EXPIRATIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        CDCDebuggable.log(
            LOG_TAG,
            "Mapping expiration time to memory: $expirationTime for apiKey: ${siteConfig.apiKey}"
        )
        // Update session map.
        sessionMap[siteConfig.apiKey] = expirationTime.toString()

        // Write session map back to encrypted shared preferences.
        esp.edit().putString(CDC_SESSION_EXPIRATIONS, Json.encodeToString(sessionMap)).apply()
    }

    /**
     * Get session expiration time from encrypted shared preferences.
     */
    private fun getExpirationTime(): Long? {
        // Get reference to encrypted shared preferences.
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val json = esp.getString(CDC_SESSION_EXPIRATIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        return sessionMap[siteConfig.apiKey]?.toLong()
    }

    /**
     * Set biometric secured session. Biometric session is double encrypted with AES256 GCM and biometric authentication.
     */
    fun secureBiometricSession(
        encryptedSession: String, //Encrypted session JSON (Base64 encoded).
        iv: String // Initialization vector for decrypting the session (Base64 encoded).
    ) {
        CDCDebuggable.log(LOG_TAG, "Securing biometric session in memory.")
        val newSessionEntity = SessionEntity(
            session = encryptedSession, secureLevel = SecureLevel(
                encryptionType = SessionSecureLevel.BIOMETRIC, iv = iv
            )
        )
        secureSessionWith(newSessionEntity)

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
        CDCDebuggable.log(
            LOG_TAG,
            "Unlocking biometric session in memory with decrypted session: $decryptedSession."
        )
        this.sessionEntity?.session = decryptedSession
    }

    /**
     * Check if session is available.
     */
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
        CDCDebuggable.log(LOG_TAG, "Clearing session in memory.")
        this.sessionEntity = null
        if (invalidate) {
            // Invalidate session && expiration in encrypted shared preferences.
            CDCDebuggable.log(LOG_TAG, "Invalidating session in storage.")
            secureSessionWith(null)
            secureExpirationWith(0)

            // Cancel expiration worker if running.
            cancelRunningSessionExpirationWorker()
        }
    }

    /**
     * Write session object (as JSON) in encrypted shared preferences.
     */
    private fun secureSessionWith(sessionEntity: SessionEntity?) {
        CDCDebuggable.log(LOG_TAG, "Securing session in storage: $sessionEntity")
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
            CDCDebuggable.log(
                LOG_TAG,
                "Removing session from memory for apiKey: ${siteConfig.apiKey}"
            )
            sessionMap.remove(siteConfig.apiKey)
        } else {
            CDCDebuggable.log(LOG_TAG, "Mapping session to memory for apiKey: ${siteConfig.apiKey}")
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
        CDCDebuggable.log(LOG_TAG, "Checking if session is biometric locked")
        if (!availableSession()) {
            CDCDebuggable.log(LOG_TAG, "Session is not available. not locked")
            return false
        }
        if (sessionEntity?.secureLevel?.encryptionType == SessionSecureLevel.STANDARD) {
            CDCDebuggable.log(LOG_TAG, "Session is not biometric locked. standard secure level")
            return false
        }
        try {
            // Only default secure level can be decoded using JSON because it is not encrypted.
            Json.decodeFromString<Session>(sessionEntity?.session!!)
            CDCDebuggable.log(
                LOG_TAG,
                "Session is not biometric locked. Default session decoded successfully"
            )
            return false
        } catch (e: Exception) {
            return true
        }
    }

    /**
     * Enqueue session expiration worker.
     */
    private fun enqueueSessionExpirationWorker(enqueueDelay: Long) {
        // Determine session expiration time to set the worker delay.

        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .setRequiresBatteryNotLow(true)
            .build()

        val oneTimeWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(CDCSessionExpirationWorker::class.java)
                .setConstraints(constraints)
                .setInitialDelay(enqueueDelay, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(siteConfig.applicationContext).enqueueUniqueWork(
            CDC_SESSION_EXPIRATION_WORKER,
            ExistingWorkPolicy.KEEP,
            oneTimeWorkRequest
        )
    }

    /**
     * Cancel running session expiration worker.
     */
    private fun cancelRunningSessionExpirationWorker() {
        CDCDebuggable.log(LOG_TAG, "Canceling running session expiration worker.")
        val workManager = WorkManager.getInstance(siteConfig.applicationContext)
        workManager.cancelUniqueWork(CDC_SESSION_EXPIRATION_WORKER)
    }

}

/**
 * Work manager expiration worker.
 * Sends session expiration intent to the application.
 * The application can register a broadcast receiver to handle the session expiration event.
 * or send a message to the CDCMessageEventBus so the SDK will invalidate the session.
 */
internal class CDCSessionExpirationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        CDCDebuggable.log(SessionSecure.LOG_TAG, "Session expiration worker triggered.")
        CDCMessageEventBus.emitSessionEvent(SessionEvent.ExpiredSession)
        return Result.success()
    }

}