package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for AboutMe screen.
 * This state is managed by AboutMeViewModel and observed by AboutMeView.
 */

@Immutable
data class AboutMeState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val nickname: String = "",
    val alias: String = "",
    val email: String = "",
    val showSuccessBanner: Boolean = false
)
