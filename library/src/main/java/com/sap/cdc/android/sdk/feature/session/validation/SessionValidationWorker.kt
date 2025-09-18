package com.sap.cdc.android.sdk.feature.session.validation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.CDCEventBusProvider
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date

/**
 * WorkManager worker that performs periodic session validation.
 * This worker is scheduled by SessionValidationService to run at configured intervals.
 * 
 * The worker retrieves the current session and validates it against the CDC API.
 * Results are emitted as events through the CDCLifecycleEventBus.
 * 
 * Created by Mirmelshtein on 18/09/2024
 * Copyright: SAP LTD.
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
        CDCDebuggable.log(LOG_TAG, "SessionValidationWorker starting work: ${Date()}")

        return try {
            // Get configuration from input data
            val apiKey = inputData.getString(INPUT_API_KEY)
            val domain = inputData.getString(INPUT_DOMAIN)
            val cname = inputData.getString(INPUT_CNAME)

            if (apiKey == null || domain == null) {
                CDCDebuggable.log(LOG_TAG, "Missing required configuration parameters")
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
            val authenticationApi = AuthenticationApi(coreClient, sessionService)
            val eventBus = CDCEventBusProvider.getEventBus()

            // Create validator and perform validation
            val validator = SessionValidator(authenticationApi, sessionService, eventBus)
            validator.validateSession()

            CDCDebuggable.log(LOG_TAG, "SessionValidationWorker completed successfully")
            Result.success()

        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "SessionValidationWorker failed: ${e.message}")
            
            // For network errors or other recoverable failures, we can retry
            when (e) {
                is UnknownHostException,
                is SocketTimeoutException,
                is IOException -> {
                    CDCDebuggable.log(LOG_TAG, "Recoverable error, will retry")
                    Result.retry()
                }
                else -> {
                    CDCDebuggable.log(LOG_TAG, "Non-recoverable error")
                    Result.failure()
                }
            }
        }
    }
}
