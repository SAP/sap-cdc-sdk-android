package com.sap.cdc.android.sdk.auth.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class BiometricKey {

    companion object {
        const val BIOMETRIC_KEY_NAME = "cdc_biometric_key"
    }

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

    fun removeSecretKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(BIOMETRIC_KEY_NAME)
    }

    fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        val secretKey = keyStore.getKey(BIOMETRIC_KEY_NAME, null) as SecretKey?
            ?: generateSecretKey()
        return secretKey
    }

    fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_GCM + "/"
                    + KeyProperties.ENCRYPTION_PADDING_NONE
        )

    }

}