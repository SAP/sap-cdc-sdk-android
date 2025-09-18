package com.sap.cdc.android.sdk.feature.session

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.EventScope
import com.sap.cdc.android.sdk.events.EventSubscription
import com.sap.cdc.android.sdk.events.SessionEvent
import com.sap.cdc.android.sdk.events.emitSessionExpired
import com.sap.cdc.android.sdk.events.subscribeToSessionEventsManual
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import kotlinx.coroutines.Dispatchers
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

        const val LOG_TAG = "SessionSecure"

        const val CDC_SESSIONS = "cdc_sessions"
        const val CDC_SESSION_EXPIRATIONS = "cdc_session_expirations"

        const val CDC_SESSION_EXPIRATION_WORKER = "cdc_session_expiration_worker"
    }

    // Holding reference to session entity in memory.
    private var sessionEntity: SessionEntity? = null
    private var eventSubscription: EventSubscription? = null

    init {
        subscribeToSessionEvents()
        loadToMem()
    }

    // Clean up subscription when SessionSecure is no longer needed
    fun dispose() {
        eventSubscription?.unsubscribe()
        eventSubscription = null
    }

    private fun subscribeToSessionEvents() {
        CDCDebuggable.log(LOG_TAG, "Subscribing to session events.")

        // Use manual subscription for SDK internal components
        eventSubscription = subscribeToSessionEventsManual(
            scope = EventScope.GLOBAL,
            dispatcher = Dispatchers.IO // Use IO dispatcher for background work
        ) { event ->
            when (event) {
                is SessionEvent.SessionExpired -> {
                    CDCDebuggable.log(LOG_TAG, "Session expired event received from bus.")
                    clearSession(true)
                }

                is SessionEvent.VerifySession -> {
                    CDCDebuggable.log(LOG_TAG, "Verify session event received from bus.")
                    // Handle session verification
                }

                is SessionEvent.SessionRefreshed -> {
                    CDCDebuggable.log(LOG_TAG, "Session refreshed event received from bus.")
                    // Handle session refresh
                }

                is SessionEvent.ValidationStarted -> {
                    CDCDebuggable.log(LOG_TAG, "Session invalidated event received from bus.")
                }

                is SessionEvent.ValidationSucceeded -> {
                    CDCDebuggable.log(LOG_TAG, "Session validation succeeded event received from bus.")
                }

                is SessionEvent.ValidationFailed -> {
                    CDCDebuggable.log(LOG_TAG, "Session validation failed event received from bus.")
                    clearSession(true)
                }
            }
        }
    }

    /**
     * Load session from encrypted shared preferences file.
     */
    private fun sessionFromEncryptedPrefs(): SessionEntity? {
        // Get reference to encrypted shared preferences.
        var sessionEntity: SessionEntity? = null
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        // Get session map from encrypted shared preferences.
        if (esp.contains(CDC_SESSIONS)) {
            val json = esp.getString(CDC_SESSIONS, null)
            if (json != null) {
                val sessionMap = Json.decodeFromString<Map<String, String>>(json)
                if (sessionMap.contains(siteConfig.apiKey)) {
                    // Get session entity from session map.
                    sessionEntity = sessionMap[siteConfig.apiKey]?.let {
                        Json.decodeFromString(it)
                    }
                    CDCDebuggable.log(LOG_TAG, "Session loaded to memory. $sessionEntity")
                }
            }
        }
        return sessionEntity
    }

    /**
     * Parse session from encrypted shared preferences and load to memory.
     * If session has expiration time - enqueue session expiration worker.
     */
    private fun loadToMem() {
        this.sessionEntity = sessionFromEncryptedPrefs()
        // Enqueue session expiration worker if the session has expiration time.
        if (sessionEntity != null) {
            if (getSessionSecureLevel() == SessionSecureLevel.BIOMETRIC) {
                // Session is biometric locked. it cannot be decoded and expiration worker cannot be set.
                CDCDebuggable.log(
                    LOG_TAG,
                    "Session is biometric locked."
                )
                return
            }

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
                    // Setting expiration time to the worker. The worker takes delay only
                    // so we are subtracting current time from expiration time.l
                    enqueueSessionExpirationWorker(expirationTime - System.currentTimeMillis())
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
            // The worker takes delay only so we are using the original expiration time value.
            enqueueSessionExpirationWorker(session.expiration!!)
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
        if (json != null && json.isNotEmpty()) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        CDCDebuggable.log(
            LOG_TAG,
            "Mapping expiration time to memory: $expirationTime for apiKey: ${siteConfig.apiKey}"
        )
        // Update session map.
        sessionMap[siteConfig.apiKey] = expirationTime.toString()

        // Write session map back to encrypted shared preferences.
        esp.edit() { putString(CDC_SESSION_EXPIRATIONS, Json.encodeToString(sessionMap)) }
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

        // Check if session has expired.
        val sessionExpiration = getExpirationTime()

        val currentTime = System.currentTimeMillis()
        if (sessionExpiration != null && sessionExpiration > 0) {
            if (sessionExpiration <= currentTime) {
                CDCDebuggable.log(LOG_TAG, "Session has expired. Clearing session.")
                emitSessionExpired(
                    sessionId = "", // Session ID is not tracked in this implementation.
                    scope = EventScope.GLOBAL
                )
            }
            clearSession(true)
            return
        } else {
            CDCDebuggable.log(
                LOG_TAG,
                "Session is valid. Enqueueing expiration worker."
            )
            // Enqueue session expiration worker.
            enqueueSessionExpirationWorker(sessionExpiration!! - currentTime)
        }
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
        try {
            return Json.decodeFromString(this.sessionEntity?.session!!)
        } catch (ex: Exception) {
            CDCDebuggable.log(
                LOG_TAG,
                "Failed to decode session. Session might be biometric locked. $ex"
            )
            return null
        }
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
        if (json != null && json.isNotEmpty()) {
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
        esp.edit() { putString(CDC_SESSIONS, Json.encodeToString(sessionMap)) }
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
        if (availableSession()) {
            CDCDebuggable.log(LOG_TAG, "Session is not available. not locked")
            return false
        }
        val sessionEntity = sessionFromEncryptedPrefs()
        if (sessionEntity != null && sessionEntity.secureLevel.encryptionType == SessionSecureLevel.BIOMETRIC) {
            CDCDebuggable.log(LOG_TAG, "Session is biometric locked.")
            return true
        }
        return false
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
        emitSessionExpired(
            sessionId = "", // Session ID is not tracked in this implementation.
            scope = EventScope.GLOBAL
        )
        return Result.success()
    }

}
