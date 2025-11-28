package com.sap.cdc.bitsnbytes.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized navigation coordinator for the application.
 * 
 * Provides a singleton interface for managing navigation operations across the app.
 * Acts as a facade that delegates state management to [AppStateManager] while providing
 * a clean, consistent navigation API.
 * 
 * ## Usage
 * ```kotlin
 * // In your ViewModel or Composable
 * val navCoordinator = NavigationCoordinator.INSTANCE
 * 
 * // Navigate to a route
 * navCoordinator.navigate(Routes.Profile.route)
 * 
 * // Navigate with options
 * navCoordinator.navigate(Routes.Home.route) {
 *     popUpTo(Routes.Auth.route) { inclusive = true }
 *     launchSingleTop = true
 * }
 * 
 * // Navigate up
 * navCoordinator.navigateUp()
 * 
 * // Check navigation availability (prevents crashes)
 * if (navCoordinator.isNavigationAvailable()) {
 *     navCoordinator.navigate(Routes.Settings.route)
 * }
 * ```
 * 
 * ## Setup
 * ```kotlin
 * // In your main activity's onCreate
 * val appStateManager = AppStateManager()
 * NavigationCoordinator.INSTANCE.setAppStateManager(appStateManager)
 * 
 * // After NavHost is composed
 * NavigationCoordinator.INSTANCE.setNavController(navController)
 * ```
 * 
 * @see AppStateManager
 * @see Routes
 */
class NavigationCoordinator private constructor() {

    companion object {
        val INSTANCE: NavigationCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
        { NavigationCoordinator() }
    }

    private var appStateManager: AppStateManager? = null

    /**
     * Expose back navigation state from AppStateManager.
     * This maintains backward compatibility with existing code that uses backNav.
     */
    val backNav: StateFlow<Boolean>
        get() = appStateManager?.canNavigateBack ?: MutableStateFlow(false).asStateFlow()

    /**
     * Set the AppStateManager to delegate navigation state management to.
     * This should be called when the AppStateManager is available.
     */
    fun setAppStateManager(manager: AppStateManager) {
        appStateManager = manager
    }

    /**
     * Set current used navigation controller.
     * Delegates to AppStateManager for actual state management.
     */
    fun setNavController(navController: NavController) {
        appStateManager?.setNavController(navController)
    }

    /**
     * Navigate to new route.
     * Delegates to AppStateManager for actual navigation.
     */
    fun navigate(route: String) {
        appStateManager?.navigate(route)
    }

    /**
     * Pop controller backstack until specified root route (including) it and navigate to specified route.
     * Delegates to AppStateManager for actual navigation.
     */
    fun popToRootAndNavigate(toRoute: String, rootRoute: String) {
        appStateManager?.popToRootAndNavigate(toRoute, rootRoute)
    }

    /**
     * Navigate to new route providing special options for this navigation operation.
     * Delegates to AppStateManager for actual navigation.
     */
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        appStateManager?.navigate(route, builder)
    }

    /**
     * Navigate back/up the stack.
     * Delegates to AppStateManager for actual navigation.
     */
    fun navigateUp() {
        appStateManager?.navigateUp()
    }

    /**
     * Check if navigation is available and safe to use.
     * This prevents crashes when the app starts from a killed state and navigation hasn't been initialized yet.
     * 
     * @return true if both AppStateManager exists AND NavController has a navigation graph set
     */
    fun isNavigationAvailable(): Boolean {
        return try {
            appStateManager?.hasValidNavController() ?: false
        } catch (e: Exception) {
            false
        }
    }
}
