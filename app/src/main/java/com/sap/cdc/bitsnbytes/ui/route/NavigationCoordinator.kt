package com.sap.cdc.bitsnbytes.ui.route

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Navigation coordinator singleton instance. Used to control and sync with the navigation flow
 * of multiple navigation controllers.
 */
class NavigationCoordinator private constructor() {

    companion object {
        val INSTANCE: NavigationCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED)
        { NavigationCoordinator() }
    }

    private var currentNavController: NavController? = null

    // Tracking back state for back icon navigation in toolbar. Kind of a hack.
    private val _backNav = MutableStateFlow(false)
    val backNav = _backNav.asStateFlow()

    /**
     * Set current used navigation controller.
     */
    fun setNavController(navController: NavController) {
        currentNavController = navController
    }

    /**
     * Navigate to new route.
     */
    fun navigate(route: String) {
        currentNavController?.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
        // Update back stack custom state.
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

    /**
     * Pop current route and navigate to new route.
     */
    fun popAndNavigate(route: String) {
        currentNavController?.popBackStack()
        currentNavController?.navigate(route)
        // Update back stack custom state.
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

    /**
     * Pop controller backstack until specified root route (including) it and navigate to specified route.
     */
    fun popToRootAndNavigate(toRoute: String, rootRoute: String) {
        currentNavController?.popBackStack(
            route = rootRoute,
            inclusive = true
        )
        currentNavController?.navigate(toRoute)
    }

    /**
     * Navigate to new route providing special options for this navigation operation.
     */
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        currentNavController?.navigate(route, navOptions(builder))
    }

    /**
     * Navigate back/up the stack.
     */
    fun navigateUp() {
        currentNavController?.popBackStack()
        // Update back stack custom state.
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

}