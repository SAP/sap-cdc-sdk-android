package com.sap.cdc.android.sdk.feature.account

import kotlinx.serialization.Serializable

@Serializable
data class LinkEntities(
    val loginProviders: List<String>,
    var loginID: String? = null,
    var phones: List<String>? = null
)