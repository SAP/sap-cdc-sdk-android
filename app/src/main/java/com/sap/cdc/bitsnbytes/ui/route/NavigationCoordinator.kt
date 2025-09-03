package com.sap.cdc.bitsnbytes.ui.route

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.sap.cdc.bitsnbytes.ui.navigation.AppStateManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Navigation coordinator singleton instance. Used to control and coordinate the navigation flow
 * of multiple navigation controllers.
 * 
 * This coordinator acts as a mediator that delegates state management to AppStateManager
 * while providing a clean navigation API for the rest of the application.
 * 
 * Responsibilities:
 * - Coordinate navigation operations across the app
 * - Delegate state management to AppStateManager
 * - Provide a consistent navigation API
 * - Handle navigation controller lifecycle
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
        get() = appStateManager?.canNavigateBack ?: kotlinx.coroutines.flow.MutableStateFlow(false).asStateFlow()

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
}
