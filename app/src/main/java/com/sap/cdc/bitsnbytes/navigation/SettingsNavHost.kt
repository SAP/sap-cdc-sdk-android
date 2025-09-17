package com.sap.cdc.bitsnbytes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.bitsnbytes.ui.view.screens.ConfigurationView
import com.sap.cdc.bitsnbytes.ui.view.screens.ConfigurationViewModel
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.factory.CustomViewModelFactory
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.factory.ViewModelScopeProvider

/**
 * Settings Navigation Host for managing configuration-related screens.
 * This NavHost follows the same pattern as ProfileNavHost to maintain consistency
 * in the application's navigation architecture.
 */
@Composable
fun SettingsNavHost(appStateManager: AppStateManager) {
    val settingsNavController = rememberNavController()

    // Update the app state manager to use our settings navigation controller
    appStateManager.setNavController(settingsNavController)

    // Listen to navigation changes and update back navigation state
    val navBackStackEntry by settingsNavController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // Always show back navigation in settings section since user can go back to main home
        val canGoBack = true
        appStateManager.setCanNavigateBack(canGoBack)
    }

    val context = LocalContext.current.applicationContext

    // Provide the shared AuthenticationFlowDelegate to the entire composition tree
    ViewModelScopeProvider.ProvideAuthenticationDelegate(context) {
        // Get the delegate to pass to ViewModels that need it
        val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)

        NavHost(
            settingsNavController, 
            startDestination = SettingsScreenRoute.Configuration.route
        ) {
            composable(SettingsScreenRoute.Configuration.route) {
                val viewModel: ConfigurationViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                ConfigurationView(viewModel)
            }
            
            // Additional settings screens can be added here in the future
            // For example:
            // composable(SettingsScreenRoute.Privacy.route) { ... }
            // composable(SettingsScreenRoute.Security.route) { ... }
            // composable(SettingsScreenRoute.Notifications.route) { ... }
        }
    }
}

/**
 * Settings screen routes for navigation within the settings flow.
 * Following the same pattern as ProfileScreenRoute.
 */
sealed class SettingsScreenRoute(
    val route: String,
) {
    data object Configuration : SettingsScreenRoute("Configuration")
    // Future settings screens can be added here:
    // data object Privacy : SettingsScreenRoute("Privacy")
    // data object Security : SettingsScreenRoute("Security")
    // data object Notifications : SettingsScreenRoute("Notifications")
}
