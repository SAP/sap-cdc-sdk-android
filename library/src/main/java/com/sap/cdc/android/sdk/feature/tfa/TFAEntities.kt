package com.sap.cdc.android.sdk.feature.tfa

import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Serializable
data class TFAProviderEntity(
    val name: String,
    val authLevel: String? = null,
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class TFAProvidersEntity(
    val activeProviders: List<TFAProviderEntity>? = emptyList(),
    val inactiveProviders: List<TFAProviderEntity>? = emptyList()
)

@Serializable
data class TFAEmailEntity(
    val id: String,
    val obfuscated: String,
    val lastVerification: String,
)

enum class TFAProvider(val value: String) {
    EMAIL("gigyaEmail"),
    PHONE("gigyaPhone"),
    PUSH("gigyaPush"),
    TOTP("gigyaTotp")
}

enum class TFAPhoneMethod(val value: String) {
    SMS("sms"),
    VOICE("voice")
}

@Serializable
data class TFARegisteredPhoneEntities(
    val phones: List<TFAPhoneEntity>? = emptyList()
)

@Serializable
data class TFAPhoneEntity(
    val id: String?,
    val obfuscated: String?,
    val lastMethod: String? = null,
    val lastVerification: String? = null,
)


