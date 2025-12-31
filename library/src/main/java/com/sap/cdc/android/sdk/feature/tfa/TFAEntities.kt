package com.sap.cdc.android.sdk.feature.tfa

import kotlinx.serialization.Serializable

/**
 * Two-Factor Authentication (TFA) data entities for SAP CIAM.
 * 
 * Contains data classes, enums, and entities for managing two-factor authentication
 * including email, phone, push, and TOTP providers.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */

/**
 * TFA provider information.
 * @property name Provider name
 * @property authLevel Authentication level
 * @property capabilities List of provider capabilities
 */
@Serializable
data class TFAProviderEntity(
    val name: String,
    val authLevel: String? = null,
    val capabilities: List<String> = emptyList(),
)

/**
 * Collection of active and inactive TFA providers.
 * @property activeProviders List of currently active providers
 * @property inactiveProviders List of inactive providers
 */
@Serializable
data class TFAProvidersEntity(
    val activeProviders: List<TFAProviderEntity>? = emptyList(),
    val inactiveProviders: List<TFAProviderEntity>? = emptyList()
)

/**
 * TFA email information.
 * @property id Email identifier
 * @property obfuscated Obfuscated email address
 * @property lastVerification Last verification timestamp
 */
@Serializable
data class TFAEmailEntity(
    val id: String,
    val obfuscated: String,
    val lastVerification: String,
)

/**
 * Supported TFA provider types.
 */
enum class TFAProvider(val value: String) {
    EMAIL("gigyaEmail"),
    PHONE("gigyaPhone"),
    PUSH("gigyaPush"),
    TOTP("gigyaTotp")
}

/**
 * Phone verification methods for TFA.
 */
enum class TFAPhoneMethod(val value: String) {
    SMS("sms"),
    VOICE("voice")
}

/**
 * Collection of registered phone numbers for TFA.
 * @property phones List of registered phone entities
 */
@Serializable
data class TFARegisteredPhoneEntities(
    val phones: List<TFAPhoneEntity>? = emptyList()
)

/**
 * TFA phone number information.
 * @property id Phone identifier
 * @property obfuscated Obfuscated phone number
 * @property lastMethod Last verification method used
 * @property lastVerification Last verification timestamp
 */
@Serializable
data class TFAPhoneEntity(
    val id: String?,
    val obfuscated: String?,
    val lastMethod: String? = null,
    val lastVerification: String? = null,
)
