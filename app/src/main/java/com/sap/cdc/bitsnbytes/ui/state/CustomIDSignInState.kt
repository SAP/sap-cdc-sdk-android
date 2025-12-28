package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for CustomIDSignIn screen.
 * This state is managed by CustomIDSignInViewModel and observed by CustomIDSignInView.
 */

@Immutable
data class CustomIDSignInState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val identifier: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false
)

/**
 * Navigation events for CustomIDSignIn screen.
 */
@Immutable
sealed class CustomIDSignInNavigationEvent {
    data object NavigateToMyProfile : CustomIDSignInNavigationEvent()
}
