package com.sap.cdc.bitsnbytes.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate

/**
 * ViewModel for MainActivity demonstrating AuthenticationDelegate injection.
 * 
 * UPDATED: Now uses the shared AuthenticationDelegate pattern.
 * This ViewModel receives the shared delegate instance to ensure single CDC SDK connection.
 */
class MainActivityViewModel(
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : ViewModel() {
    
    // This delegate is the SHARED instance that all ViewModels will use
    // It provides both state management and direct CDC SDK access
    
    // Expose authentication state to UI
    val isAuthenticated = authenticationFlowDelegate.isAuthenticated
    val userAccount = authenticationFlowDelegate.userAccount
    
    // Direct access to CDC SDK components
    val authenticationService = authenticationFlowDelegate.authenticationService
    
    // Example methods would go here - implementation details removed to avoid compilation errors
    // fun initializeApp() { /* implementation */ }
    // fun handleSessionExpired() { /* implementation */ }
    // fun handleSessionVerification() { /* implementation */ }
}
