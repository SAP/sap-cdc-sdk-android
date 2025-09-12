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
    @SerialName("DeviceName") val deviceName: String,
    @SerialName("RegistrationDate") val registrationDate: String,
    @SerialName("IpAddress") val ipAddress: String,
    @SerialName("City") val city: String,
    @SerialName("State") val state: String,
    @SerialName("Country") val country: String,
    @SerialName("Platform") val platform: String,
    @SerialName("Browser") val browser: String,
    @SerialName("IsMobile") val isMobile: Boolean,
    @SerialName("LastLogin") val lastLogin: String
)

@Serializable
data class PasskeyCredentials(
    @SerialName("credentials") val credentials: List<PasskeyCredential>
)
