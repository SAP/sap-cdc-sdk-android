package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for PendingRegistration screen.
 * This state is managed by PendingRegistrationViewModel and observed by PendingRegistrationView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class PendingRegistrationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val fieldValues: Map<String, String> = emptyMap(),
    val missingFields: List<String> = emptyList()
)

/**
 * Navigation events for PendingRegistration screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class PendingRegistrationNavigationEvent {
    data object NavigateToMyProfile : PendingRegistrationNavigationEvent()
    data class NavigateToLinkAccount(val linkingContext: String) : PendingRegistrationNavigationEvent()
}
