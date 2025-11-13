package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for OtpVerify screen.
 * This state is managed by OtpVerifyViewModel and observed by OtpVerifyView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class OtpVerifyState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpValue: String = "",
    val codeSent: Boolean = false
)

/**
 * Navigation events for OtpVerify screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class OtpVerifyNavigationEvent {
    data object NavigateToMyProfile : OtpVerifyNavigationEvent()
    data class NavigateToPendingRegistration(val registrationContext: String) : OtpVerifyNavigationEvent()
}
