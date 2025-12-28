package com.sap.cdc.android.sdk.feature.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal session storage entity with encryption metadata.
 * 
 * @property session Serialized session data (may be encrypted)
 * @property secureLevel Encryption level and metadata
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
 * CDC mobile session data.
 * 
 * Represents an authenticated user session containing credentials required
 * for making authenticated API requests to CDC services.
 * 
 * ## Usage
 * ```kotlin
 * val session = authService.session().getSession()
 * if (session != null) {
 *     println("Token: ${session.token}")
 *     println("Expires in: ${session.expiration} seconds")
 * }
 * ```
 * 
 * @property token Session token for authentication
 * @property secret Session secret for request signing
 * @property expiration Time until session expires in seconds (0 = no expiration)
 */
@Serializable
data class Session(
    @SerialName("sessionToken") var token: String,
    @SerialName("sessionSecret") var secret: String,
    @SerialName("expires_in") var expiration: Long? = 0,
)

/**
 * Session encryption level configuration.
 * 
 * @property encryptionType The type of encryption applied to the session
 * @property iv Initialization vector for biometric encryption (null for standard encryption)
 */
@Serializable
data class SecureLevel(
    @SerialName("encryptionType") val encryptionType: SessionSecureLevel,
    @SerialName("iv") val iv: String?
)

/**
 * Session encryption types.
 * 
 * Defines the available security levels for session storage:
 * - **STANDARD**: Session encrypted with AES256 GCM
 * - **BIOMETRIC**: Session encrypted with biometric authentication (requires device unlock)
 * 
 * ## Usage
 * ```kotlin
 * val currentLevel = authService.session().sessionSecurityLevel()
 * when (currentLevel) {
 *     SessionSecureLevel.STANDARD -> // Standard encryption
 *     SessionSecureLevel.BIOMETRIC -> // Biometric protected
 * }
 * ```
 */
enum class SessionSecureLevel(val value: Int) {
    STANDARD(0), 
    BIOMETRIC(1);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value }
    }
}
