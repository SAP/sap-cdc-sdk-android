package com.sap.cdc.android.sdk.feature.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkEntities(
    val loginProviders: List<String>,
    var loginID: String? = null
)
