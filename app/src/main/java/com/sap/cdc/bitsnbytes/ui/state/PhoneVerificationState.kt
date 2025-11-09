package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for PhoneVerification screen.
 * This state is managed by PhoneVerificationViewModel and observed by PhoneVerificationView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class PhoneVerificationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpValue: String = "",
    val codeSent: Boolean = false
)

/**
 * Navigation events for PhoneVerification screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class PhoneVerificationNavigationEvent {
    data object NavigateToMyProfile : PhoneVerificationNavigationEvent()
}
