package com.sap.cdc.android.sdk.example.ui.route

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
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

    fun setNavController(navController: NavController) {
        currentNavController = navController
    }

    fun navigate(route: String) {
        currentNavController?.navigate(route)
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

    fun popAndNavigate(route: String) {
        currentNavController?.popBackStack()
        currentNavController?.navigate(route)
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        currentNavController?.navigate(route, navOptions(builder))
    }

    fun navigateUp() {
        currentNavController?.popBackStack()
        _backNav.value = currentNavController?.previousBackStackEntry != null
    }

}