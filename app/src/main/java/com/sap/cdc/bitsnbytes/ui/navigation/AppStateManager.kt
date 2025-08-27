package com.sap.cdc.bitsnbytes.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized state manager for the entire application.
 * Manages global state that needs to persist across navigation and configuration changes.
 */
class AppStateManager : ViewModel() {

    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Current selected tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Navigation state for back button handling
    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // User session data
    private val _userSession = MutableStateFlow<UserSessionData?>(null)
    val userSession: StateFlow<UserSessionData?> = _userSession.asStateFlow()

    /**
     * Update authentication state
     */
    fun setAuthenticated(isAuth: Boolean) {
        _isAuthenticated.value = isAuth
    }

    /**
     * Update selected tab
     */
    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    /**
     * Update back navigation state
     */
    fun setCanNavigateBack(canGoBack: Boolean) {
        _canNavigateBack.value = canGoBack
    }

    /**
     * Set loading state
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Set error message
     */
    fun setError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Update user session data
     */
    fun setUserSession(session: UserSessionData?) {
        _userSession.value = session
        _isAuthenticated.value = session != null
    }

    /**
     * Clear all state (for logout)
     */
    fun clearState() {
        _isAuthenticated.value = false
        _userSession.value = null
        _selectedTab.value = 0
        _canNavigateBack.value = false
        _isLoading.value = false
        _errorMessage.value = null
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
