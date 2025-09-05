package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.emitTokenReceived
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.session.Session
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.bitsnbytes.feature.auth.model.AccountEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Complete authentication state solution for ViewModels.
 * Provides direct access to CDC SDK and manages authentication state.
 *
 * Key benefits:
 * - Direct CDC SDK access (eliminates repository passthrough)
 * - Centralized authentication state management
 * - Activity-scoped lifecycle when used with ViewModelScopeProvider
 * - Single instance shared across all ViewModels
 */
class AuthenticationFlowDelegate(context: Context) {

    // Direct CDC SDK access - eliminates repository passthrough boilerplate
    /**
     * Initialize the site configuration class
     */
    private var siteConfig = SiteConfig(context)

    /**
     * Initialize authentication service.
     */
    var authenticationService = AuthenticationService(siteConfig).registerForPushAuthentication(
        object : IFCMTokenRequest {
            override fun requestFCMToken() {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    emitTokenReceived(token)
                })
            }
        }
    )

    val cdc = AuthenticationFlowCDCDelegate(authenticationService)

    // Authentication state flows
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userAccount = MutableStateFlow<AccountEntity?>(null)
    val userAccount: StateFlow<AccountEntity?> = _userAccount.asStateFlow()

    init {
        // Initialize state based on existing CDC session
        syncWithCDCSession()
    }

    // State management methods
    fun setAuthenticated(isAuth: Boolean) {
        _isAuthenticated.value = isAuth
    }

    fun clearAuthenticationState() {
        _isAuthenticated.value = false
        _userAccount.value = null
    }

    // CDC Session integration helpers
    fun updateFromCDCSession(session: Session?) {
        if (session != null) {
            setAuthenticated(true)
        } else {
            setAuthenticated(false)
        }
    }

    // Convenience methods for common CDC operations
    fun hasValidSession(): Boolean = authenticationService.session().availableSession()
    fun getCurrentCDCSession(): Session? = authenticationService.session().getSession()
    fun clearCDCSession() = authenticationService.session().clearSession()

    // Helper method to sync with CDC session
    private fun syncWithCDCSession() {
        val cdcSession = getCurrentCDCSession()
        updateFromCDCSession(cdcSession)
    }

    /**
     * Handle session expiration - clears both CDC session and local state
     */
    fun handleSessionExpired() {
        clearCDCSession()
        clearAuthenticationState()
    }

    /**
     * Refresh authentication state from CDC session
     * Useful when session might have been updated externally
     */
    fun refreshAuthenticationState() {
        syncWithCDCSession()
    }
}

class AuthenticationFlowCDCDelegate(val service: AuthenticationService) {

    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        service.authenticate().login()
            .credentials(credentials = credentials, configure = authCallbacks)
    }

    suspend fun register(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit,
        parameters: MutableMap<String, String> = mutableMapOf()
    ) {
        service.authenticate().register().credentials(
            credentials = credentials,
            configure = authCallbacks,
            parameters = parameters
        )
    }
}

/**
 * Extension functions for AuthenticationDelegate
 */
fun AuthenticationFlowDelegate.isUserLoggedIn(): Boolean = isAuthenticated.value

fun AuthenticationFlowDelegate.getCurrentUser(): AccountEntity? = userAccount.value

