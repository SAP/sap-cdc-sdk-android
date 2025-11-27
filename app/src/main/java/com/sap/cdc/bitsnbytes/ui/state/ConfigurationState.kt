package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for Configuration screen.
 * This state is managed by ConfigurationViewModel and observed by ConfigurationView.
 */

@Immutable
data class ConfigurationState(
    val apiKey: String = "",
    val domain: String = "",
    val cname: String = "",
    val useWebView: Boolean = false,
    val debugNavigationLogging: Boolean = false
)
