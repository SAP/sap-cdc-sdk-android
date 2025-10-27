package com.sap.cdc.android.sdk.feature.session.validation

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import java.util.concurrent.TimeUnit

/**
 * Main service for managing periodic session validation using WorkManager.
 *
 * This service provides comprehensive control over session validation:
 * - Enable/disable validation with configurable intervals
 * - Pause/resume validation temporarily
 * - Restart validation (useful after configuration changes)
 * - Update validation interval dynamically
 * - Check if validation is currently active
 *
 * The service uses WorkManager to ensure validation continues even when the app
 * is in the background or after the process is killed by the system.
 *
 * @param context Application context for WorkManager initialization
 * @param siteConfig CDC site configuration containing API key and domain
 *
 * Created by Mirmelshtein on 18/09/2024
 * Copyright: SAP LTD.
 */
class SessionValidationService(
    private val siteConfig: SiteConfig,
    private var currentConfig: SessionValidationConfig = SessionValidationConfig()
) {
    companion object {
        const val LOG_TAG = "SessionValidationService"
        const val WORK_NAME = "session_validation"
    }

    private val workManager = WorkManager.getInstance(siteConfig.applicationContext)

    /**
     * Enables session validation with the specified interval.
     * If validation is already running, it will be restarted with the new interval.
     *
     * @param intervalMinutes The interval in minutes between validation checks (default: 15)
     */
    fun enable(intervalMinutes: Long = 15L) {
        CDCDebuggable.log(LOG_TAG, "Enabling session validation with interval: $intervalMinutes minutes")

        currentConfig = SessionValidationConfig(intervalMinutes, enabled = true)
        startWorker()
    }

    /**
     * Disables session validation completely.
     * This cancels any scheduled validation work.
     */
    fun disable() {
        CDCDebuggable.log(LOG_TAG, "Disabling session validation")

        currentConfig = currentConfig.copy(enabled = false)
        workManager.cancelUniqueWork(WORK_NAME)
    }

    /**
     * Temporarily pauses session validation.
     * Validation can be resumed later using resume().
     */
    fun pause() {
        CDCDebuggable.log(LOG_TAG, "Pausing session validation")

        workManager.cancelUniqueWork(WORK_NAME)
    }

    /**
     * Resumes session validation if it was previously enabled.
     * Only works if validation was enabled before being paused.
     */
    fun resume() {
        CDCDebuggable.log(LOG_TAG, "Resuming session validation")

        if (currentConfig.enabled) {
            startWorker()
        } else {
            CDCDebuggable.log(LOG_TAG, "Cannot resume - validation was not previously enabled")
        }
    }

    /**
     * Restarts session validation.
     * This cancels any existing validation work and starts fresh.
     * Useful after configuration changes or to force immediate rescheduling.
     */
    fun restart() {
        CDCDebuggable.log(LOG_TAG, "Restarting session validation")

        workManager.cancelUniqueWork(WORK_NAME)
        if (currentConfig.enabled) {
            startWorker()
        }
    }

    /**
     * Updates the validation interval and restarts validation with the new interval.
     *
     * @param intervalMinutes The new interval in minutes between validation checks
     */
    fun updateInterval(intervalMinutes: Long) {
        CDCDebuggable.log(LOG_TAG, "Updating validation interval to: $intervalMinutes minutes")

        currentConfig = currentConfig.copy(intervalMinutes = intervalMinutes)
        if (currentConfig.enabled) {
            restart() // Apply new interval
        }
    }

    /**
     * Checks if session validation is currently active.
     *
     * @return true if validation work is enqueued or running, false otherwise
     */
    fun isActive(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            val isActive = workInfos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
            CDCDebuggable.log(LOG_TAG, "Session validation active: $isActive")
            isActive
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "Error checking validation status: ${e.message}")
            false
        }
    }

    /**
     * Gets the current validation configuration.
     *
     * @return The current SessionValidationConfig
     */
    fun getCurrentConfig(): SessionValidationConfig = currentConfig

    /**
     * Configures session validation with a full configuration object.
     * 
     * @param config The new configuration to apply
     */
    fun configure(config: SessionValidationConfig) {
        CDCDebuggable.log(LOG_TAG, "Configuring session validation: $config")
        
        currentConfig = config
        updateValidation()
    }

    /**
     * Called when a new session is established.
     * This method enables validation if it was configured to do so.
     */
    fun onNewSession() {
        CDCDebuggable.log(LOG_TAG, "New session detected")
        
        if (currentConfig.enabled) {
            CDCDebuggable.log(LOG_TAG, "Enabling session validation for new session")
            startWorker()
        }
    }

    /**
     * Internal method to start the WorkManager worker with current configuration.
     */
    private fun startWorker() {
        CDCDebuggable.log(LOG_TAG, "Starting session validation worker")

        // Create input data with site configuration
        val inputData = Data.Builder()
            .putString(SessionValidationWorker.INPUT_API_KEY, siteConfig.apiKey)
            .putString(SessionValidationWorker.INPUT_DOMAIN, siteConfig.domain)
            .putString(SessionValidationWorker.INPUT_CNAME, siteConfig.cname)
            .build()

        // Set constraints for the work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create periodic work request
        val workRequest = PeriodicWorkRequestBuilder<SessionValidationWorker>(
            currentConfig.intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        // Enqueue the work
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        CDCDebuggable.log(
            LOG_TAG,
            "Session validation worker started with interval: ${currentConfig.intervalMinutes} minutes"
        )
    }

    /**
     * Internal method to update validation based on current configuration.
     */
    private fun updateValidation() {
        workManager.cancelUniqueWork(WORK_NAME)

        if (currentConfig.enabled) {
            startWorker()
        }
    }
}
