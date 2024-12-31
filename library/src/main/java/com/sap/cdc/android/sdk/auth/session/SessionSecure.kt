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
        registerForSessionEvents()
        loadToMem()
    }

    private fun registerForSessionEvents() {

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

                // Enqueue session expiration worker if the session has expiration time.
                val session = getSession()
                if (session?.expiration != null) {
                    cancelRunningSessionExpirationWorker()
                    val expirationTime = getExpirationTime()
                    enqueueSessionExpirationWorker(expirationTime!! - System.currentTimeMillis())
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

        // Enqueue session expiration worker if the session has expiration time.
        if (session.expiration != null) {
            // Determine time for session expiration.
            val expirationTime =
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(session.expiration!!)
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
        this.sessionEntity = null
        if (invalidate) {
            // Invalidate session in encrypted shared preferences.
            secure(null)

            // Cancel expiration worker if running.
            cancelRunningSessionExpirationWorker()
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

        //TODO: Add worker to invalidate the session if it expires.
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
        CDCMessageEventBus.emitSessionEvent(SessionEvent.ExpiredSession)
        return Result.success()
    }

}