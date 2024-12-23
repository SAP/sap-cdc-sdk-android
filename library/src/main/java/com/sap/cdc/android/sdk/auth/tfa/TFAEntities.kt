package com.sap.cdc.android.sdk.auth.tfa

import kotlinx.serialization.Serializable


@Serializable
data class TFAProviderEntity(
    val name: String,
    val authLevel: String
)

@Serializable
data class TFAProvidersEntity(
    val activeProviders: List<TFAProviderEntity> = emptyList(),
    val inactiveProviders: List<TFAProviderEntity> = emptyList()
)
