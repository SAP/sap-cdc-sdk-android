package com.sap.cdc.android.sdk.auth.model

data class OptionsResponseModel(
    var options: String? = null,
    var token: String? = null
)

data class GetCredentialResultEntity(
    val credential: String,
    val options: String,
)