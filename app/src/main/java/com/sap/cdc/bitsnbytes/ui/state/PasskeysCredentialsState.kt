package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredentials

/**
 * Created by Tal Mirmelshtein on 09/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for PasskeysCredentials screen.
 * This state is managed by PasskeysCredentialsViewModel and observed by PasskeysCredentialsView.
 */

@Immutable
data class PasskeysCredentialsState(
    val isLoading: Boolean = false,
    val isLoadingPasskeys: Boolean = false,
    val error: String? = null,
    val passkeyCredentials: PasskeyCredentials? = null
)
