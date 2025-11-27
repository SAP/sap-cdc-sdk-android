package com.sap.cdc.bitsnbytes.navigation

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
    
    private val _hasProfileBackStack = MutableStateFlow(false)
    val hasProfileBackStack: StateFlow<Boolean> = _hasProfileBackStack.asStateFlow()
    
    // NAVIGATION CONTROLLER MANAGEMENT
    private var currentNavController: NavController? = null
    
    // CURRENT ROUTE TRACKING
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute.asStateFlow()
    
    // Deduplication tracking
    private var lastLoggedRoute: String? = null
    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null
    
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
    
    fun setHasProfileBackStack(hasBackStack: Boolean) {
        _hasProfileBackStack.value = hasBackStack
    }
    
    // NAVIGATION CONTROLLER METHODS
    fun setNavController(navController: NavController) {
        // Remove old listener if exists (prevents duplicate listeners)
        destinationChangedListener?.let { oldListener ->
            currentNavController?.removeOnDestinationChangedListener(oldListener)
        }
        
        currentNavController = navController
        
        // Create and store new listener with deduplication
        destinationChangedListener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            val newRoute = destination.route
            _currentRoute.value = newRoute
            updateBackNavigationState()
            
            // Only log if route actually changed (deduplication)
            if (newRoute != lastLoggedRoute) {
                lastLoggedRoute = newRoute
                NavigationDebugLogger.logNavigation(
                    source = "AppStateManager",
                    action = "Destination Changed",
                    navController = controller,
                    details = "New destination: $newRoute"
                )
            }
        }
        
        // Add the new listener
        navController.addOnDestinationChangedListener(destinationChangedListener!!)
        
        // Initial state update
        updateBackNavigationState()
    }
    
    fun navigate(route: String) {
        NavigationDebugLogger.logNavigation(
            source = "AppStateManager",
            action = "navigate()",
            navController = currentNavController,
            targetRoute = route
        )
        
        currentNavController?.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
        updateBackNavigationState()
    }
    
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        NavigationDebugLogger.logNavigation(
            source = "AppStateManager",
            action = "navigate(with options)",
            navController = currentNavController,
            targetRoute = route
        )
        
        currentNavController?.navigate(route, navOptions(builder))
        updateBackNavigationState()
    }
    
    fun popToRootAndNavigate(toRoute: String, rootRoute: String) {
        NavigationDebugLogger.logNavigation(
            source = "AppStateManager",
            action = "popToRootAndNavigate()",
            navController = currentNavController,
            targetRoute = toRoute,
            details = "Popping to root: $rootRoute"
        )
        
        currentNavController?.popBackStack(
            route = rootRoute,
            inclusive = true
        )
        currentNavController?.navigate(toRoute)
        updateBackNavigationState()
    }
    
    fun navigateUp() {
        NavigationDebugLogger.logNavigation(
            source = "AppStateManager",
            action = "navigateUp()",
            navController = currentNavController
        )
        
        currentNavController?.popBackStack()
        updateBackNavigationState()
    }
    
    private fun updateBackNavigationState() {
        _canNavigateBack.value = currentNavController?.previousBackStackEntry != null
    }
    
    /**
     * Check if the current NavController is valid and has a navigation graph set.
     * This prevents crashes when trying to navigate before the graph is initialized.
     */
    fun hasValidNavController(): Boolean {
        return try {
            currentNavController?.graph != null
        } catch (e: Exception) {
            false
        }
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
    
    override fun onCleared() {
        // Clean up listener when ViewModel is cleared to prevent memory leaks
        destinationChangedListener?.let { listener ->
            currentNavController?.removeOnDestinationChangedListener(listener)
        }
        destinationChangedListener = null
        currentNavController = null
        super.onCleared()
    }
}
