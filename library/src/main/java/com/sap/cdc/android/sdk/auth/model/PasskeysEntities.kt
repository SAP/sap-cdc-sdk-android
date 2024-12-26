package com.sap.cdc.android.sdk.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class OptionsResponseModel(
    var options: String? = null,
    var token: String? = null
)

@Serializable
data class GetCredentialResultEntity(
    val credential: String,
    val options: String,
)