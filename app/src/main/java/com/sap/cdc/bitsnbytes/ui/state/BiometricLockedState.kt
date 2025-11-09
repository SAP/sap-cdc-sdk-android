package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for BiometricLocked screen.
 * This state is managed by BiometricLockedViewModel and observed by BiometricLockedView.
 */

@Immutable
data class BiometricLockedState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation events for BiometricLocked screen.
 */
@Immutable
sealed class BiometricLockedNavigationEvent {
    data object NavigateToMyProfile : BiometricLockedNavigationEvent()
}
