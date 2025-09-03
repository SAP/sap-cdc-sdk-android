package com.sap.cdc.bitsnbytes.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.sap.cdc.bitsnbytes.ui.navigation.AuthenticationFlowDelegate

/**
 * Example ViewModel demonstrating how to inject AuthenticationDelegate.
 * 
 * This shows the injection pattern:
 * 1. Receives AuthenticationDelegate as constructor parameter
 * 2. Can access authentication state and CDC SDK directly
 * 
 * Usage in Compose:
 * ```
 * @Composable
 * fun MyScreen() {
 *     val context = LocalContext.current
 *     val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)
 *     val viewModel: ExampleAuthenticationViewModel = viewModel { 
 *         ExampleAuthenticationViewModel(authDelegate) 
 *     }
 *     
 *     val isAuthenticated by viewModel.isAuthenticated.collectAsState()
 *     // ... use state and call methods
 * }
 * ```
 */
class ExampleAuthenticationViewModel(
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : ViewModel() {

    // AUTHENTICATION STATE - Direct access to delegate's StateFlow properties
    val isAuthenticated = authenticationFlowDelegate.isAuthenticated
    val userSession = authenticationFlowDelegate.userSession
    val authenticationError = authenticationFlowDelegate.authenticationError
    val isAuthenticating = authenticationFlowDelegate.isAuthenticating

    // DIRECT CDC SDK ACCESS - No repository needed!
    val siteConfig = authenticationFlowDelegate.siteConfig
    val authenticationService = authenticationFlowDelegate.authenticationService

    // Example methods would go here...
    // fun login(email: String, password: String) { ... }
    // fun logout() { ... }
    // etc.
}
