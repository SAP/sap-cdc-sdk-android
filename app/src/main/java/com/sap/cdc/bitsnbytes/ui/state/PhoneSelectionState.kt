package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable
import com.sap.cdc.bitsnbytes.ui.view.model.Country
import com.sap.cdc.bitsnbytes.ui.view.model.CountryData

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for PhoneSelection screen.
 * This state is managed by PhoneSelectionViewModel and observed by PhoneSelectionView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class PhoneSelectionState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputField: String = "",
    val selectedCountry: Country = CountryData.getDefaultCountry()
)

/**
 * Navigation events for PhoneSelection screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class PhoneSelectionNavigationEvent {
    data class NavigateToPhoneVerification(val twoFactorContext: String) : PhoneSelectionNavigationEvent()
}
