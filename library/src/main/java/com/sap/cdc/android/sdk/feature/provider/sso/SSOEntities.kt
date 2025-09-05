package com.sap.cdc.android.sdk.feature.provider.sso

import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 15/12/2024
 * Copyright: SAP LTD.
 */

@Serializable
data class SSOResponseEntity(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Long = 0L,
    val id_token: String?,
    val refresh_token: String?,
    val device_secret: String?
)
