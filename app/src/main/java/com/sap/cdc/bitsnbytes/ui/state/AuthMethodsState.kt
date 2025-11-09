package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for AuthMethods screen.
 * This state is managed by AuthMethodsViewModel and observed by AuthMethodsView.
 */

@Immutable
data class AuthMethodsState(
    val twoFactorContext: String = ""
)

/**
 * Navigation events for AuthMethods screen.
 */
@Immutable
sealed class AuthMethodsNavigationEvent {
    data object NavigateToPhoneSelection : AuthMethodsNavigationEvent()
    data class NavigateToTOTPVerification(val context: String) : AuthMethodsNavigationEvent()
    data object NavigateToLogin : AuthMethodsNavigationEvent()
}
