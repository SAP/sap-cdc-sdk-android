package com.sap.cdc.android.sdk.feature.auth.model

data class Credentials(
    val loginId: String? = null,
    val email: String? = null,
    val aToken: String? = null,
    val password: String
)

data class CustomIdCredentials(
    val identifier: String,
    val identifierType: String,
    val password: String
)