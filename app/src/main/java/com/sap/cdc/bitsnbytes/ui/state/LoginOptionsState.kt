package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for LoginOptions screen.
 * This state is managed by LoginOptionsViewModel and observed by LoginOptionsView.
 */

@Immutable
data class LoginOptionsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showBanner: Boolean = false,
    val bannerText: String = ""
)
