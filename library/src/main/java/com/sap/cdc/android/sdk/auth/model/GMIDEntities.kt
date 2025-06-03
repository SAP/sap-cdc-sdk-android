package com.sap.cdc.android.sdk.auth.model

import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
@Serializable
data class GMIDEntity(
    val gmid: String?,
    val refreshTime: Long? = 0L
)
