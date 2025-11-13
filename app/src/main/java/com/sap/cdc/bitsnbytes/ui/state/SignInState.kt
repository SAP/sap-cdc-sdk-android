package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for SignIn screen.
 * This state is managed by SignInViewModel and observed by SignInView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class SignInState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation events for SignIn screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class SignInNavigationEvent {
    data class NavigateToProfile(val route: String) : SignInNavigationEvent()
    data class NavigateToPendingRegistration(val context: String) : SignInNavigationEvent()
    data class NavigateToLinkAccount(val context: String) : SignInNavigationEvent()
    data class NavigateToAuthTab(val tabIndex: Int) : SignInNavigationEvent()
    data class NavigateToOTPSignIn(val otpType: String) : SignInNavigationEvent()
    data object NavigateToCustomIdSignIn : SignInNavigationEvent()
}
