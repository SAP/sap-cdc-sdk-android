package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable
import com.sap.cdc.bitsnbytes.ui.view.model.Country
import com.sap.cdc.bitsnbytes.ui.view.model.CountryData

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for OtpSignIn screen.
 * This state is managed by OtpSignInViewModel and observed by OtpSignInView.
 */

@Immutable
data class OtpSignInState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputField: String = "",
    val selectedCountry: Country = CountryData.getDefaultCountry()
)

/**
 * Navigation events for OtpSignIn screen.
 */
@Immutable
sealed class OtpSignInNavigationEvent {
    data class NavigateToOtpVerify(
        val otpContext: String,
        val otpType: Int,
        val inputField: String
    ) : OtpSignInNavigationEvent()
}
