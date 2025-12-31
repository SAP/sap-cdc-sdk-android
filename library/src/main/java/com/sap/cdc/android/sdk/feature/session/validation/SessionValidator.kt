package com.sap.cdc.android.sdk.feature.session.validation

import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.events.LifecycleAwareEventBus
import com.sap.cdc.android.sdk.events.SessionEvent
import com.sap.cdc.android.sdk.feature.AuthEndpoints
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService
import kotlinx.coroutines.Dispatchers
import java.util.Date

/**
 * Core session validation logic that performs validation against CIAM API.
 * 
 * Validates active sessions by checking their validity with the CIAM backend.
 * Emits events for validation lifecycle (started, succeeded, failed).
 * 
 * @property authenticationApi Authentication API client for network requests
 * @property sessionService Session service for retrieving and managing sessions
 * @property eventBus Event bus for emitting validation events
 * 
 * @author Tal Mirmelshtein
 * @since 18/09/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see SessionEvent
 * @see SessionService
 */
class SessionValidator(
    private val authenticationApi: AuthenticationApi,
    private val sessionService: SessionService,
    private val eventBus: LifecycleAwareEventBus
) {
    companion object {
        const val LOG_TAG = "SessionValidator"
    }

    /**
     * Validates the current session by making a request to the CIAM API.
     * Emits appropriate events based on the validation result.
     *
     * @throws Exception if validation fails due to network or other errors
     */
    suspend fun validateSession() {
        val session = sessionService.getSession()
        if (session == null) {
            CIAMDebuggable.log(LOG_TAG, "No active session found for validation")
            emitEvent(SessionEvent.ValidationFailed("", "No active session"))
            return
        }

        CIAMDebuggable.log(LOG_TAG, "Starting session validation: ${Date()} ")
        emitEvent(SessionEvent.ValidationStarted(session.token))

        try {
            val parameters = mutableMapOf(
                "sessionToken" to session.token
            )

            val response = authenticationApi.send(
                api = AuthEndpoints.EP_ACCOUNTS_VERIFY_LOGIN,
                parameters = parameters
            )

            if (response.isError()) {
                // Session on longer valid
                val errorMsg = response.errorMessage() ?: "Unknown validation error"
                CIAMDebuggable.log(LOG_TAG, "Session validation failed: $errorMsg")
                emitEvent(SessionEvent.ValidationFailed(session.token, errorMsg))
                return
            }

            CIAMDebuggable.log(LOG_TAG, "Session validation succeeded")
            emitEvent(SessionEvent.ValidationSucceeded(session.token))

        } catch (e: Exception) {
            CIAMDebuggable.log(LOG_TAG, "Session validation failed with exception: ${e.message}")
            val errorMsg = e.message ?: "Network error during validation"
            emitEvent(SessionEvent.ValidationFailed(session.token, errorMsg))
            throw e
        }
    }

    /**
     * Emits a session event using the event bus.
     *
     * @param event The session event to emit
     */
    private fun emitEvent(event: SessionEvent) {
        eventBus.emit(
            event = event,
            dispatcher = Dispatchers.Main
        )
    }
}
