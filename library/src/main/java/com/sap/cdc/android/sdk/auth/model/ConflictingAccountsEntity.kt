package com.sap.cdc.android.sdk.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class ConflictingAccountsEntity(
    val loginProviders: List<String>,
    var loginID: String? = null
)
