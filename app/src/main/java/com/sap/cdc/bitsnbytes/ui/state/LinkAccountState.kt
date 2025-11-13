package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for LinkAccount screen.
 * This state is managed by LinkAccountViewModel and observed by LinkAccountView.
 */

@Immutable
data class LinkAccountState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val password: String = ""
)

/**
 * Navigation events for LinkAccount screen.
 */
@Immutable
sealed class LinkAccountNavigationEvent {
    data object NavigateToMyProfile : LinkAccountNavigationEvent()
}
