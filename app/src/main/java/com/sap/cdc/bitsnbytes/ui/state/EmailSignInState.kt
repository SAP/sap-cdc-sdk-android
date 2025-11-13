package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for EmailSignIn screen.
 * This state is managed by EmailSignInViewModel and observed by EmailSignInView.
 */

@Immutable
data class EmailSignInState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val captchaRequired: Boolean = false
)

/**
 * Navigation events for EmailSignIn screen.
 */
@Immutable
sealed class EmailSignInNavigationEvent {
    data object NavigateToMyProfile : EmailSignInNavigationEvent()
    data class NavigateToAuthMethods(val twoFactorContextJson: String) : EmailSignInNavigationEvent()
    data class NavigateToPendingRegistration(val registrationContextJson: String) : EmailSignInNavigationEvent()
}
