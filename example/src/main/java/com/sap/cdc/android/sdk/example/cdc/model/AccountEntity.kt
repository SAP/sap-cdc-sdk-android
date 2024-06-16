package com.sap.cdc.android.sdk.example.cdc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Serializable
data class AccountEntity(
    @SerialName("UID") val uid: String,
    val profile: ProfileEntity
)

@Serializable
data class ProfileEntity(
    val email: String,
    val firstName: String,
    val lastName: String
)