package com.sap.cdc.bitsnbytes.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.sap.cdc.bitsnbytes.ui.navigation.AuthenticationFlowDelegate

/**
 * Example ViewModel demonstrating how to inject AuthenticationDelegate.
 * This shows the pattern for ViewModels that need authentication functionality.
 * 
 * Key benefits:
 * - Direct access to CDC SDK through delegate (no repository passthrough)
 * - Shared authentication state across the app (single instance)
 * - Clean separation of concerns
 * 
 * IMPORTANT: This ViewModel receives the shared AuthenticationDelegate instance
 * from the AuthenticationViewModelFactory. The Composable doesn't need to know
 * about the delegate - only the ViewModel.
 */
class ExampleLoginViewModel(
    private val authDelegate: AuthenticationFlowDelegate
) : ViewModel() {
    
    // This delegate is the SHARED instance provided by AuthenticationViewModelFactory
    // All ViewModels created through the factory will use the same delegate instance
    
    // Expose authentication state to UI
    val isAuthenticated = authDelegate.isAuthenticated
    val userSession = authDelegate.userSession
    val authenticationError = authDelegate.authenticationError
    val isAuthenticating = authDelegate.isAuthenticating
    
    // Direct access to CDC SDK components
    val siteConfig = authDelegate.siteConfig
    val authenticationService = authDelegate.authenticationService
    
    // Example methods would go here - implementation details removed to avoid compilation errors
    // fun login(email: String, password: String) { /* implementation */ }
    // fun socialLogin(provider: String) { /* implementation */ }
    // fun logout() { /* implementation */ }
}
