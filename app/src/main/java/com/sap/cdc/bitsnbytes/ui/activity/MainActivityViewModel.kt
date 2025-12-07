package com.sap.cdc.bitsnbytes.ui.activity

import androidx.lifecycle.ViewModel
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.events.CDCEventBusProvider
import com.sap.cdc.android.sdk.events.EventSubscription
import com.sap.cdc.android.sdk.events.SessionEvent
import com.sap.cdc.android.sdk.events.subscribeToSessionEventsManual
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute

/**
 * ViewModel for MainActivity with proper session event handling.
 *
 * Responsibilities:
 * - Session event subscription and handling (proper MVVM architecture)
 * - Delegates authentication operations to AuthenticationFlowDelegate
 * - Coordinates navigation on session events
 * - Proper cleanup in onCleared()
 * - Provides single activity-scoped AuthenticationFlowDelegate instance
 */
class MainActivityViewModel(
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : ViewModel() {

    // This delegate is the SHARED instance that all ViewModels will use
    // It provides both state management and direct CDC SDK access
    // IMPORTANT: Made public so MainActivity can provide it via CompositionLocal

    // Expose authentication state to UI
    val isAuthenticated = authenticationFlowDelegate.isAuthenticated
    val userAccount = authenticationFlowDelegate.userAccount

    // Direct access to CDC SDK components
    val authenticationService = authenticationFlowDelegate.authenticationService

    // Event subscription handle for manual cleanup
    private var sessionEventSubscription: EventSubscription? = null

    init {
        setupSessionEventHandling()
    }

    /**
     * Setup session event handling with manual subscription.
     * ViewModels use manual subscription since they're not LifecycleOwners.
     */
    private fun setupSessionEventHandling() {
        // Initialize event bus if not already initialized
        if (!CDCEventBusProvider.isInitialized()) {
            CDCEventBusProvider.initialize()
        }

        // Subscribe to session events with manual lifecycle management
        sessionEventSubscription = subscribeToSessionEventsManual { event ->
            when (event) {
                is SessionEvent.SessionExpired -> handleSessionExpired()
                is SessionEvent.VerifySession -> handleSessionVerification()
                is SessionEvent.SessionRefreshed -> handleSessionRefreshed()
                is SessionEvent.ValidationStarted -> handleValidationStarted()
                is SessionEvent.ValidationSucceeded -> handleValidationSucceeded()
                is SessionEvent.ValidationFailed -> handleValidationFailed(event.reason)
            }
        }
    }

    /**
     * Handle session expiration.
     * Clears authentication state and navigates to welcome screen.
     */
    private fun handleSessionExpired() {
        CDCDebuggable.log("MainActivityViewModel", "Session expired - clearing state")
        
        // Clear authentication state via delegate
        authenticationFlowDelegate.handleSessionExpired()

        // Navigate to welcome screen
        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
            toRoute = ProfileScreenRoute.Welcome.route,
            rootRoute = ProfileScreenRoute.Welcome.route
        )
    }

    /**
     * Handle session verification request.
     */
    private fun handleSessionVerification() {
        CDCDebuggable.log("MainActivityViewModel", "Session verification requested")
        // Session verification logic can be added here if needed
    }

    /**
     * Handle session refresh.
     */
    private fun handleSessionRefreshed() {
        CDCDebuggable.log("MainActivityViewModel", "Session refreshed")
        // Session refresh handling can be added here if needed
    }

    /**
     * Handle session validation started.
     * Only fires if session validation is enabled via registerForSessionValidation().
     */
    private fun handleValidationStarted() {
        CDCDebuggable.log("MainActivityViewModel", "Session validation started")
    }

    /**
     * Handle successful session validation.
     * Only fires if session validation is enabled via registerForSessionValidation().
     */
    private fun handleValidationSucceeded() {
        CDCDebuggable.log("MainActivityViewModel", "Session validation succeeded")
    }

    /**
     * Handle failed session validation.
     * Only fires if session validation is enabled via registerForSessionValidation().
     * 
     * @param reason The reason for validation failure
     */
    private fun handleValidationFailed(reason: String) {
        CDCDebuggable.log("MainActivityViewModel", "Session validation failed: $reason")
        // Could trigger re-authentication flow if needed
    }

    /**
     * Clean up resources when ViewModel is cleared.
     * Unsubscribes from event bus to prevent memory leaks.
     */
    override fun onCleared() {
        sessionEventSubscription?.unsubscribe()
        sessionEventSubscription = null
        super.onCleared()
    }
}
