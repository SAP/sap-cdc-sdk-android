package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for Welcome screen.
 * This state is managed by WelcomeViewModel and observed by WelcomeView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class WelcomeState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation events for Welcome screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class WelcomeNavigationEvent {
    data object NavigateToMyProfile : WelcomeNavigationEvent()
}
