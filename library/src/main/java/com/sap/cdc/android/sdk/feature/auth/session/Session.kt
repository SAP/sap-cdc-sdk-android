package com.sap.cdc.android.sdk.feature.auth.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Serializable
data class SessionEntity(
    @SerialName("session") var session: String? = null,
    @SerialName("secureLevel") var secureLevel: SecureLevel = SecureLevel(
        encryptionType = SessionSecureLevel.STANDARD,
        iv = null
    ),
)

/**
 * CDC Mobile session structure data class.
 */
@Serializable
data class Session(
    @SerialName("sessionToken") var token: String,
    @SerialName("sessionSecret") var secret: String,
    @SerialName("expires_in") var expiration: Long? = 0,
)

/**
 * CDC mobile session secure level data class.
 */
@Serializable
data class SecureLevel(
    @SerialName("encryptionType") val encryptionType: SessionSecureLevel,
    @SerialName("iv") val iv: String?
)

/**
 * Available encryption types.
 * STANDARD - session will be encrypted in AES256 GCM mode.
 * BIOMETRIC - session will be encrypted using biometric authentication.
 */
enum class SessionSecureLevel(val value: Int) {
    STANDARD(0), BIOMETRIC(1);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value }
    }
}
