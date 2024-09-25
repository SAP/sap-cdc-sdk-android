package com.sap.cdc.android.sdk.auth.model

data class ConflictingAccountsEntity(
    val loginProviders: List<String>,
    var loginId: String? = null
)
