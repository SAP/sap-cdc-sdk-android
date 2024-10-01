package com.sap.cdc.android.sdk.auth.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
@Serializable
data class Session(
    @SerialName("sessionToken") var token: String,
    @SerialName("sessionSecret") var secret: String,
    @SerialName("expires_in") var expiration: Long? = 0,
    @SerialName("encryptionType") var encryptionType: SessionEncryption = SessionEncryption.DEFAULT
) {

    companion object {
        fun fromJson(json: String): Session = Json.decodeFromString(json)
    }

    fun toJson(): String = Json.encodeToString(this)

}

@Serializable
data class SessionInfo(
    @SerialName("sessionInfo") var session: Session
)

enum class SessionEncryption {
    DEFAULT, BIOMETRIC
}