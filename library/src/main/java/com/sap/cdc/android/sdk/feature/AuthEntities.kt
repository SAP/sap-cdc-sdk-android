package com.sap.cdc.android.sdk.feature

data class EmailCredentials(
    val email: String,
    val password: String
)

data class LoginIdCredentials(
    val loginId: String,
    val password: String
)

data class ATokenCredentials(
    val aToken: String,
    val password: String
)

data class CustomIdCredentials(
    val identifier: String,
    var identifierType: String,
    val password: String
)

data class Credentials(
    val loginId: String? = null,
    val email: String? = null,
    val aToken: String? = null,
    val password: String
)

