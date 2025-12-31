package com.sap.cdc.android.sdk.feature.session.validation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.CIAMEventBusProvider
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date

/**
 * WorkManager worker that performs periodic session validation.
 * 
 * Scheduled by SessionValidationService to run at configured intervals. The worker
 * validates active sessions against the CIAM API and emits events for the results.
 * 
 * Features:
 * - Automatically stops when no active session exists
 * - Stops on authentication errors (SecurityException, IllegalStateException)
 * - Retries on network errors (IOException, SocketTimeoutException, UnknownHostException)
 * 
 * @author Tal Mirmelshtein
 * @since 18/09/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see SessionValidationService
 * @see SessionValidator
 */
class SessionValidationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val LOG_TAG = "SessionValidationWorker"
        const val INPUT_API_KEY = "api_key"
        const val INPUT_DOMAIN = "domain"
        const val INPUT_CNAME = "cname"
    }

    override suspend fun doWork(): Result {
        CIAMDebuggable.log(LOG_TAG, "SessionValidationWorker starting work: ${Date()}")

        return try {
            // Get configuration from input data
            val apiKey = inputData.getString(INPUT_API_KEY)
            val domain = inputData.getString(INPUT_DOMAIN)
            val cname = inputData.getString(INPUT_CNAME)

            if (apiKey == null || domain == null) {
                CIAMDebuggable.log(LOG_TAG, "Missing required configuration parameters")
                return Result.failure()
            }

            // Create site configuration
            val siteConfig = SiteConfig(
                applicationContext = applicationContext,
                apiKey = apiKey,
                domain = domain,
                cname = cname
            )

            // Initialize services
            val coreClient = CoreClient(siteConfig)
            val sessionService = SessionService(siteConfig)
            
            // Primary check: Is there an active session?
            if (!sessionService.availableSession()) {
                CIAMDebuggable.log(LOG_TAG, "No active session - stopping validation worker")
                cancelRecurringWork()
                return Result.failure()
            }

            val authenticationApi = AuthenticationApi(coreClient, sessionService)
            val eventBus = CIAMEventBusProvider.getEventBus()

            // Create validator and perform validation
            val validator = SessionValidator(authenticationApi, sessionService, eventBus)
            validator.validateSession()

            CIAMDebuggable.log(LOG_TAG, "SessionValidationWorker completed successfully")
            Result.success()

        } catch (e: Exception) {
            CIAMDebuggable.log(LOG_TAG, "SessionValidationWorker failed: ${e.message}")
            
            // Don't stop on network errors, but stop on auth errors
            when (e) {
                is SecurityException,
                is IllegalStateException -> {
                    CIAMDebuggable.log(LOG_TAG, "Authentication error - stopping validation worker")
                    cancelRecurringWork()
                    Result.failure()
                }
                is UnknownHostException,
                is SocketTimeoutException,
                is IOException -> {
                    CIAMDebuggable.log(LOG_TAG, "Recoverable network error, will retry")
                    Result.retry()
                }
                else -> {
                    CIAMDebuggable.log(LOG_TAG, "Non-recoverable error")
                    Result.failure()
                }
            }
        }
    }

    /**
     * Helper method to cancel the recurring session validation work.
     * This ensures the validation worker stops when sessions are no longer available.
     */
    private fun cancelRecurringWork() {
        WorkManager.getInstance(applicationContext)
            .cancelUniqueWork(SessionValidationService.WORK_NAME)
    }
}
