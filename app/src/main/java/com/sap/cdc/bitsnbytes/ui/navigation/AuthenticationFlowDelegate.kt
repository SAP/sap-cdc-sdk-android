package com.sap.cdc.bitsnbytes.ui.navigation

import android.content.Context
import com.sap.cdc.android.sdk.auth.AuthenticationService
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.bitsnbytes.cdc.IdentityServiceRepository
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
    
    // Get CDC SDK instances from repository (but expose them directly)
    private val identityRepository = IdentityServiceRepository.getInstance(context)
    
    // Direct CDC SDK access - eliminates repository passthrough boilerplate
    val siteConfig: SiteConfig = identityRepository.getConfig()
    val authenticationService: AuthenticationService = identityRepository.authenticationService
    
    // Authentication state flows
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userSession = MutableStateFlow<UserSessionData?>(null)
    val userSession: StateFlow<UserSessionData?> = _userSession.asStateFlow()
    
    private val _authenticationError = MutableStateFlow<String?>(null)
    val authenticationError: StateFlow<String?> = _authenticationError.asStateFlow()
    
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()
    
    
    init {
        // Initialize state based on existing CDC session
        syncWithCDCSession()
    }
    
    // State management methods
    fun setAuthenticated(isAuth: Boolean, session: UserSessionData? = null) {
        _isAuthenticated.value = isAuth
        _userSession.value = session
    }
    
    fun setAuthenticationError(error: String?) {
        _authenticationError.value = error
    }
    
    fun setAuthenticating(isAuthenticating: Boolean) {
        _isAuthenticating.value = isAuthenticating
    }
    
    fun clearAuthenticationState() {
        _isAuthenticated.value = false
        _userSession.value = null
        _authenticationError.value = null
        _isAuthenticating.value = false
    }
    
    fun clearError() {
        _authenticationError.value = null
    }
    
    // CDC Session integration helpers
    fun updateFromCDCSession(session: Session?) {
        if (session != null) {
            // Convert CDC session to UserSessionData
            // Note: Session only contains token/secret, user profile info would need to be fetched separately
            val userSessionData = UserSessionData(
                userId = "", // Would need to fetch from account info
                email = null, // Would need to fetch from account info
                displayName = null, // Would need to fetch from account info
                profileImageUrl = null, // Would need to fetch from account info
                sessionToken = session.token
            )
            setAuthenticated(true, userSessionData)
        } else {
            setAuthenticated(false, null)
        }
    }
    
    fun getCDCSession(): Session? {
        return getCurrentCDCSession()
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
     * Handle successful authentication - updates both CDC session and local state
     */
    fun handleAuthenticationSuccess(cdcSession: Session) {
        // Set the session in CDC SDK
        authenticationService.session().setSession(cdcSession)
        // Update local state
        updateFromCDCSession(cdcSession)
        setAuthenticationError(null)
        setAuthenticating(false)
    }
    
    /**
     * Refresh authentication state from CDC session
     * Useful when session might have been updated externally
     */
    fun refreshAuthenticationState() {
        syncWithCDCSession()
    }
}

/**
 * Data class for user session information
 */
data class UserSessionData(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val profileImageUrl: String?,
    val sessionToken: String
)

/**
 * Extension functions for AuthenticationDelegate
 */
fun AuthenticationFlowDelegate.isUserLoggedIn(): Boolean = isAuthenticated.value

fun AuthenticationFlowDelegate.getCurrentUser(): UserSessionData? = userSession.value

fun AuthenticationFlowDelegate.hasAuthenticationError(): Boolean = authenticationError.value != null

/**
 * Convenience function to handle authentication success
 */
fun AuthenticationFlowDelegate.handleAuthenticationSuccess(session: UserSessionData) {
    setAuthenticationError(null)
    setAuthenticating(false)
    setAuthenticated(true, session)
}

/**
 * Convenience function to handle authentication failure
 */
fun AuthenticationFlowDelegate.handleAuthenticationFailure(error: String) {
    setAuthenticating(false)
    setAuthenticated(false)
    setAuthenticationError(error)
}

/**
 * Convenience function to start authentication process
 */
fun AuthenticationFlowDelegate.startAuthentication() {
    setAuthenticationError(null)
    setAuthenticating(true)
}
