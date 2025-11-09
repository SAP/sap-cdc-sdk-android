package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for EmailRegistration screen.
 * This state is managed by EmailRegistrationViewModel and observed by EmailRegisterView.
 */

@Immutable
data class EmailRegistrationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false
)

/**
 * Navigation events for EmailRegistration screen.
 */
@Immutable
sealed class EmailRegistrationNavigationEvent {
    data object NavigateToMyProfile : EmailRegistrationNavigationEvent()
    data class NavigateToAuthMethods(val twoFactorContextJson: String) : EmailRegistrationNavigationEvent()
    data class NavigateToPendingRegistration(val registrationContextJson: String) : EmailRegistrationNavigationEvent()
}
