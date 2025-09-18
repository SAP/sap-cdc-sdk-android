package com.sap.cdc.android.sdk.feature.provider.passkey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OptionsResponseModel(
    var options: String? = null,
    var token: String? = null
)

@Serializable
data class GetCredentialResultEntity(
    val credential: String,
    val options: String,
)

@Serializable
data class PasskeyCredential(
    @SerialName("Id") val id: String,
    @SerialName("DeviceName") val deviceName: String? = null,
    @SerialName("RegistrationDate") val registrationDate: String? = null,
    @SerialName("IpAddress") val ipAddress: String? = null,
    @SerialName("City") val city: String? = null,
    @SerialName("State") val state: String? = null,
    @SerialName("Country") val country: String? = null,
    @SerialName("Platform") val platform: String? = null,
    @SerialName("Browser") val browser: String? = null,
    @SerialName("IsMobile") val isMobile: Boolean? = null,
    @SerialName("LastLogin") val lastLogin: String? = null
)

@Serializable
data class PasskeyCredentials(
    @SerialName("credentials") val credentials: List<PasskeyCredential> = listOf()
)
