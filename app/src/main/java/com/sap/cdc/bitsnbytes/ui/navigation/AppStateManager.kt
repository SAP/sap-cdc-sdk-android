package com.sap.cdc.bitsnbytes.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AppStateManager handles app-wide navigation and UI state.
 * This is separate from authentication state and focuses on navigation concerns.
 * 
 * Responsibilities:
 * - Tab selection state
 * - Navigation back button state
 * - Navigation controller management
 * - General loading states
 * - General error messages (non-authentication)
 */
class AppStateManager : ViewModel() {
    
    // NAVIGATION STATE
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    
    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()
    
    // NAVIGATION CONTROLLER MANAGEMENT
    private var currentNavController: NavController? = null
    
    // GENERAL APP STATE
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // NAVIGATION METHODS
    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
    
    fun setCanNavigateBack(canGoBack: Boolean) {
        _canNavigateBack.value = canGoBack
    }
    
    // NAVIGATION CONTROLLER METHODS
    fun setNavController(navController: NavController) {
        currentNavController = navController
        // Update back navigation state when controller changes
        updateBackNavigationState()
    }
    
    fun navigate(route: String) {
        currentNavController?.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
        updateBackNavigationState()
    }
    
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        currentNavController?.navigate(route, navOptions(builder))
        updateBackNavigationState()
    }
    
    fun popToRootAndNavigate(toRoute: String, rootRoute: String) {
        currentNavController?.popBackStack(
            route = rootRoute,
            inclusive = true
        )
        currentNavController?.navigate(toRoute)
        updateBackNavigationState()
    }
    
    fun navigateUp() {
        currentNavController?.popBackStack()
        updateBackNavigationState()
    }
    
    private fun updateBackNavigationState() {
        _canNavigateBack.value = currentNavController?.previousBackStackEntry != null
    }
    
    // GENERAL STATE METHODS
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun clearAllState() {
        _selectedTab.value = 0
        _canNavigateBack.value = false
        _isLoading.value = false
        _errorMessage.value = null
    }
}
