package com.sap.cdc.android.sdk.authentication.biometric

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import com.sap.cdc.android.sdk.authentication.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.authentication.session.SessionEncryption
import com.sap.cdc.android.sdk.authentication.session.SessionSecure
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import javax.crypto.Cipher

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class BiometricAuth(private val sessionService: SessionService) {

    private var keyGen: BiometricKey = BiometricKey()

    companion object {
        const val LOG_TAG = "BiometricAuth"
        const val BIOMETRIC_ENROLL_REQUEST_CODE = 10080
        const val BIOMETRIC_SETTINGS_REQUEST_CODE = 10081
    }

    init {
        keyGen.generateSecretKey()
    }

    /**
     * Check if the device can authenticate with biometrics.
     * Return status is defined via "androidx.biometric.BiometricManager" class error definitions.
     * @see BiometricManager for more info.
     */
    @SuppressLint("SwitchIntDef")
    fun canAuthenticate(
        activityContext: Activity, authenticators: Int? = BIOMETRIC_STRONG
    ): Int {
        val biometricManager = BiometricManager.from(activityContext)
        return biometricManager.canAuthenticate(authenticators!!)
    }

    /**
     * Start intent flow for biometric enrollment.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun startBiometricEnrollment(
        activityContext: Activity, authenticators: Int
    ) {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators
            )
        }
        activityContext.startActivityForResult(enrollIntent, BIOMETRIC_ENROLL_REQUEST_CODE)
    }

    /**
     * Open settings screen to allow biometric enrollment.
     */
    private fun openSecuritySettingsScreenForBiometricEnrollment(activityContext: Activity) {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        activityContext.startActivityForResult(intent, BIOMETRIC_SETTINGS_REQUEST_CODE)
    }

    /**
     * Directs user to enroll biometric feature if device is equal or higher than API level 30 or
     * opens settings screen to allow the feature manually.
     * This method should be used when error BIOMETRIC_ERROR_NONE_ENROLLED is received from
     * the "canAuthenticated" method.
     */
    fun enrollOrAllowBiometricAuthentication(
        activityContext: Activity, authenticators: Int? = BIOMETRIC_STRONG
    ) {
        // Prompts the user to create credentials that your app accepts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startBiometricEnrollment(activityContext, authenticators!!)
        } else {
            openSecuritySettingsScreenForBiometricEnrollment(activityContext)
        }
    }

    /**
     * Write session object (encrypted json using biometric cipher) in encrypted shared preferences.
     * Double encryption.
     */
    private fun secure(cipher: Cipher) {
        val session = sessionService.sessionSecure.getSession()
        if (session == null) {
            Log.e(
                LOG_TAG,
                "Error securing biometric session: Unable to retrieve session from SDK secure storage"
            )
            return
        }
        val esp =
            sessionService.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val json = esp.getString(SessionSecure.CDC_SESSIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }

        // Encrypt session via cipher
        session.encryptionType = SessionEncryption.BIOMETRIC
        val encryptedSession =
            String(cipher.doFinal(session.toJson().toByteArray(Charset.defaultCharset())))

        // Put encrypted session in session map.
        sessionMap[sessionService.siteConfig.apiKey] = encryptedSession

        esp.edit().putString(SessionSecure.CDC_SESSIONS, Json.encodeToString(sessionMap)).apply()
    }

    //region OPT - IN

    fun optInForBiometricSessionAuthentication(
        prompt: BiometricPrompt, promptInfo: BiometricPrompt.PromptInfo
    ) {
        val cipher = keyGen.getCipher()
        val secretKey = keyGen.getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun getOptInAuthenticationCallback() = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Log.d(
                LOG_TAG,
                "Biometric OptIn: onAuthenticationError: code: $errorCode, message: $errString"
            )
        }

        override fun onAuthenticationFailed() {
            Log.d(LOG_TAG, "Biometric OptIn: onAuthenticationFailed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            Log.d(LOG_TAG, "Biometric OptIn: onAuthenticationSucceeded")
            val cipher = result.cryptoObject?.cipher
            if (cipher == null) {
                Log.e(LOG_TAG, "Biometric OptIn: onAuthenticationSucceeded - Error no Cipher")
            }
            // Encrypt session with biometric key.
            secure(cipher!!)
        }
    }

    //endregion

    //region OPT - OUT

    fun optOutFromBiometricSessionAuthentication(
        promptInfo: BiometricPrompt.PromptInfo
    ) {
        val cipher = keyGen.getCipher()
        val secretKey = keyGen.getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    }

    fun getOptOutAuthenticationCallback() = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Log.d(
                LOG_TAG,
                "Biometric OptOut: onAuthenticationError: code: $errorCode, message: $errString"
            )
        }

        override fun onAuthenticationFailed() {
            Log.d(LOG_TAG, "Biometric OptOut: onAuthenticationFailed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            Log.d(LOG_TAG, "Biometric OptOut: onAuthenticationSucceeded")
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                // Decrypt session with biometric key and re-encrypt session with default setting.
            }
        }
    }

    //endregion

    //region LOCK/UNLOCK

    fun lockSessionWithBiometricAuthentication() {
        //TODO: Probably not required anymore cause the SDK does not track session on heap.
    }

    fun unlockSessionWithBiometricAuthentication(
        promptInfo: BiometricPrompt.PromptInfo
    ) {
        val cipher = keyGen.getCipher()
        val secretKey = keyGen.getSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
    }

    fun getUnlockAuthenticationCallback() = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Log.d(
                LOG_TAG,
                "Biometric OptOut: onAuthenticationError: code: $errorCode, message: $errString"
            )
        }

        override fun onAuthenticationFailed() {
            Log.d(LOG_TAG, "Biometric OptOut: onAuthenticationFailed")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            Log.d(LOG_TAG, "Biometric OptOut: onAuthenticationSucceeded")
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                // Decrypt session with biometric key make it available in heap..

            }
        }
    }

    //endregion

}

