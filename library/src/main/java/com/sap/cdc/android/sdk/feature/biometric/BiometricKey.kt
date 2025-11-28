package com.sap.cdc.android.sdk.feature.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Biometric authentication key management for secure session storage.
 * 
 * Manages cryptographic keys stored in Android Keystore for biometric-protected
 * session encryption. Keys are invalidated when new biometric credentials are enrolled.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */
class BiometricKey {

    companion object {
        const val BIOMETRIC_KEY_NAME = "cdc_biometric_key"
    }

    /**
     * Generates a new AES secret key in Android Keystore.
     * Key requires biometric authentication and is invalidated when biometrics change.
     * @return Generated SecretKey
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            // Invalidate the keys if the user has registered a new biometric
            // credential, such as a new fingerprint. Can call this method only
            // on Android 7.0 (API level 24) or higher. The variable
            // "invalidatedByBiometricEnrollment" is true by default.
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Removes the biometric secret key from Android Keystore.
     * Called when biometric authentication is disabled or reset.
     */
    fun removeSecretKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(BIOMETRIC_KEY_NAME)
    }

    /**
     * Retrieves or generates the biometric secret key.
     * If key doesn't exist, generates a new one.
     * @return SecretKey for biometric encryption/decryption
     */
    fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        val secretKey = keyStore.getKey(BIOMETRIC_KEY_NAME, null) as SecretKey?
            ?: generateSecretKey()
        return secretKey
    }

    /**
     * Creates a Cipher instance for AES-GCM encryption.
     * Used for encrypting/decrypting biometric-protected session data.
     * @return Configured Cipher instance
     */
    fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_GCM + "/"
                    + KeyProperties.ENCRYPTION_PADDING_NONE
        )
    }

}
