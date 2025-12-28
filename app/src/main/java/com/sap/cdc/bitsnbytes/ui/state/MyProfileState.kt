package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for MyProfile screen.
 * This state is managed by MyProfileViewModel and observed by MyProfileView.
 */

@Immutable
data class MyProfileState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

/**
 * Navigation events for MyProfile screen.
 */
@Immutable
sealed class MyProfileNavigationEvent {
    data object NavigateToWelcome : MyProfileNavigationEvent()
}
