package com.sap.cdc.bitsnbytes.ui.view.viewmodel.factory

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate

// CompositionLocal for providing AuthenticationFlowDelegate across the composition tree
val LocalAuthenticationDelegate = compositionLocalOf<AuthenticationFlowDelegate?> { null }

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
        // Get the activity's ViewModelStoreOwner to ensure true activity scoping
        val activityViewModelStoreOwner = LocalViewModelStoreOwner.current
        return if (factory != null && activityViewModelStoreOwner != null) {
            viewModel<T>(viewModelStoreOwner = activityViewModelStoreOwner, factory = factory)
        } else if (factory != null) {
            viewModel<T>(factory = factory)
        } else if (activityViewModelStoreOwner != null) {
            viewModel<T>(viewModelStoreOwner = activityViewModelStoreOwner)
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
        // Get the backstack entry outside of remember to satisfy lint requirements
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
     * This provides a shared authentication state and direct CIAM SDK access
     * across all ViewModels in the application.
     * 
     * IMPORTANT: This ensures only ONE instance of AuthenticationDelegate exists
     * per activity, shared across all ViewModels.
     */
    @Composable
    fun activityScopedAuthenticationDelegate(
        context: Context
    ): AuthenticationFlowDelegate {
        // Get from CompositionLocal (provided by MainActivity)
        val providedDelegate = LocalAuthenticationDelegate.current
        return providedDelegate ?: throw IllegalStateException(
            "AuthenticationFlowDelegate not provided. Make sure MainActivity provides it via CompositionLocalProvider."
        )
    }
}

/**
 * ViewModel wrapper for AuthenticationDelegate to ensure proper lifecycle management.
 * This ensures only ONE AuthenticationDelegate instance exists per activity.
 */
class AuthenticationDelegateViewModel(context: Context) : ViewModel() {
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
