package com.sap.cdc.bitsnbytes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Provides proper ViewModel scoping for different navigation levels.
 * Solves the issue of ViewModel state loss during tab navigation.
 */
object ViewModelScopeProvider {

    /**
     * Creates a ViewModel scoped to the activity level.
     * Use this for ViewModels that should persist across all navigation.
     */
    @Composable
    inline fun <reified T : ViewModel> activityScopedViewModel(
        factory: ViewModelProvider.Factory? = null
    ): T {
        return if (factory != null) {
            viewModel<T>(factory = factory)
        } else {
            viewModel<T>()
        }
    }

    /**
     * Creates a ViewModel scoped to a navigation graph.
     * Use this for ViewModels that should persist within a tab but reset when switching tabs.
     */
    @Composable
    inline fun <reified T : ViewModel> graphScopedViewModel(
        navController: NavController,
        graphRoute: String,
        factory: ViewModelProvider.Factory? = null
    ): T {
        val backStackEntry = navController.getBackStackEntry(graphRoute)
        return if (factory != null) {
            viewModel<T>(viewModelStoreOwner = backStackEntry, factory = factory)
        } else {
            viewModel<T>(viewModelStoreOwner = backStackEntry)
        }
    }

    /**
     * Creates a ViewModel scoped to the current screen.
     * Use this for ViewModels that should be recreated for each screen.
     */
    @Composable
    inline fun <reified T : ViewModel> screenScopedViewModel(
        factory: ViewModelProvider.Factory? = null,
        viewModelStoreOwner: ViewModelStoreOwner? = null
    ): T {
        val owner = viewModelStoreOwner ?: LocalViewModelStoreOwner.current
        return if (factory != null && owner != null) {
            viewModel<T>(viewModelStoreOwner = owner, factory = factory)
        } else if (factory != null) {
            viewModel<T>(factory = factory)
        } else {
            viewModel<T>()
        }
    }

    /**
     * Creates a ViewModel scoped to a parent navigation entry.
     * Useful for sharing ViewModels between child screens.
     */
    @Composable
    inline fun <reified T : ViewModel> parentScopedViewModel(
        navBackStackEntry: NavBackStackEntry,
        factory: ViewModelProvider.Factory? = null
    ): T {
        return if (factory != null) {
            viewModel<T>(viewModelStoreOwner = navBackStackEntry, factory = factory)
        } else {
            viewModel<T>(viewModelStoreOwner = navBackStackEntry)
        }
    }

    /**
     * Creates an activity-scoped authentication delegate.
     * This provides a shared authentication state and direct CDC SDK access
     * across all ViewModels in the application.
     * 
     * IMPORTANT: This ensures only ONE instance of AuthenticationDelegate exists
     * per activity, shared across all ViewModels.
     */
    @Composable
    fun activityScopedAuthenticationDelegate(
        context: android.content.Context
    ): AuthenticationFlowDelegate {
        // Use activity-scoped ViewModel to ensure the delegate survives ViewModel recreation
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthenticationDelegateViewModel(context) as T
            }
        }
        val viewModel = activityScopedViewModel<AuthenticationDelegateViewModel>(factory)
        return viewModel.authenticationFlowDelegate
    }
}

/**
 * ViewModel wrapper for AuthenticationDelegate to ensure proper lifecycle management.
 * This ensures only ONE AuthenticationDelegate instance exists per activity.
 */
class AuthenticationDelegateViewModel(context: android.content.Context) : ViewModel() {
    val authenticationFlowDelegate = AuthenticationFlowDelegate(context)
}

/**
 * Extension function to get a ViewModel scoped to a specific navigation graph
 */
@Composable
inline fun <reified T : ViewModel> NavController.graphScopedViewModel(
    graphRoute: String,
    factory: ViewModelProvider.Factory? = null
): T {
    return ViewModelScopeProvider.graphScopedViewModel<T>(this, graphRoute, factory)
}
