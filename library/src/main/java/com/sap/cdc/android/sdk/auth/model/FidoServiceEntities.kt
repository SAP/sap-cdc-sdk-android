package com.sap.cdc.android.sdk.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OptionsResponseModel(
    val token: String,
    val options: String
)

@Serializable
data class CreateCredentialResultEntity(
    @SerialName("rawId") var rawId: String? = null,
    @SerialName("authenticatorAttachment") var authenticatorAttachment: String? = null,
    @SerialName("type") var type: String? = null,
    @SerialName("id") var id: String? = null,
    @SerialName("response") var response: CreateCredentialResultResponseEntity? = CreateCredentialResultResponseEntity(),
)

@Serializable
data class CreateCredentialResultResponseEntity(
    @SerialName("clientDataJSON") var clientDataJSON: String? = null,
    @SerialName("attestationObject") var attestationObject: String? = null,
    @SerialName("transports") var transports: ArrayList<String> = arrayListOf(),
    @SerialName("authenticatorData") var authenticatorData: String? = null,
    @SerialName("publicKeyAlgorithm") var publicKeyAlgorithm: Int? = null,
    @SerialName("publicKey") var publicKey: String? = null
)
