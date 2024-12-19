package com.sap.cdc.android.sdk.auth.biometric

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionEntity
import com.sap.cdc.android.sdk.auth.session.SessionSecure
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

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
     * Securing the session via biometric secure will encrypt the session Json with the biometric key
     * in addition to default AES encryption which will occur always when saving the session.
     */
    private fun biometricSecure(cipher: Cipher) {
        val session: Session? = sessionService.getSession()
        if (session == null) {
            Log.e(
                LOG_TAG,
                "Error securing biometric session: Unable to retrieve session from SDK secure storage"
            )
            return
        }

        // Encode session to JSON
        val sessionJson = Json.encodeToString(session)

        // Encrypt session via cipher
        val encryptedBytes =
            cipher.doFinal(sessionJson.toByteArray(Charset.defaultCharset()))
        val encryptedSession = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

        sessionService.secureBiometricSession(
            encryptedSession = encryptedSession,
            iv = String(Base64.encode(cipher.iv, Base64.DEFAULT))
        )

        CDCDebuggable.log(
            LOG_TAG,
            "Biometric session encrypted in addition to the default encryption"
        )
    }

    /**
     * Decrypting the session via biometric secure will decrypt the session Json with the biometric key
     * and only the default AES session encryption will remain.
     */
    private fun biometricUnsecure(sessionEntity: SessionEntity, cipher: Cipher, optOut: Boolean) {
        CDCDebuggable.log(LOG_TAG, "biometricUnsecure: save:$optOut")
        val encryptedSession = sessionEntity.session
        val encryptedBytes = Base64.decode(encryptedSession, Base64.DEFAULT)
        val decryptedSession = String(cipher.doFinal(encryptedBytes))

        if (optOut) {
            // Set the session with the decrypted session. No biometric encryption will be applied.
            val session: Session = Json.decodeFromString(decryptedSession)
            sessionService.setSession(session)
            keyGen.removeSecretKey()
        } else {
            sessionService.unlockBiometricSession(decryptedSession)
        }

        CDCDebuggable.log(
            LOG_TAG,
            "Biometric session decrypted and resaved with default encryption"
        )
    }

    /**
     * Unlock the session with biometric authentication.
     */
    private fun unlockBiometrics(
        optOut: Boolean,
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        onAuthenticationError: (errorCode: Int, errString: CharSequence) -> Unit,
        onAuthenticationFailed: () -> Unit,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        val cipher = keyGen.getCipher()
        val secretKey = keyGen.getSecretKey()

        val sessionEntity = getSessionEntity()
        if (sessionEntity == null) {
            CDCDebuggable.log(LOG_TAG, "Biometric OptOut: No session to unlock")
            onAuthenticationFailed()
            return
        }

        // Get IV from session entity required for decryption.
        val ivSpecBytes = sessionEntity.secureLevel.iv
        val ivSpec = GCMParameterSpec(128, Base64.decode(ivSpecBytes, Base64.DEFAULT))

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onAuthenticationError(errorCode, errString)
                    CDCDebuggable.log(
                        LOG_TAG,
                        "Biometric OptOut: onAuthenticationError: code: $errorCode, message: $errString"
                    )
                }

                override fun onAuthenticationFailed() {
                    onAuthenticationFailed()
                    CDCDebuggable.log(LOG_TAG, "Biometric OptOut: onAuthenticationFailed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {

                    CDCDebuggable.log(LOG_TAG, "Biometric OptOut: onAuthenticationSucceeded")
                    val cryptoObjectCipher = result.cryptoObject?.cipher
                    if (cryptoObjectCipher != null) {
                        // Decrypt session with biometric key and re-encrypt session with default setting.
                        biometricUnsecure(
                            sessionEntity = sessionEntity,
                            cipher = cryptoObjectCipher,
                            optOut = optOut
                        )
                    }
                    onAuthenticationSucceeded(result)
                }
            }
        )
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    //region OPT - IN

    /**
     * Opt in for biometric session authentication.
     */
    fun optInForBiometricSessionAuthentication(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        onAuthenticationError: (errorCode: Int, errString: CharSequence) -> Unit,
        onAuthenticationFailed: () -> Unit,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        val cipher = keyGen.getCipher()
        val secretKey = keyGen.getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onAuthenticationError(errorCode, errString)
                    CDCDebuggable.log(
                        LOG_TAG,
                        "Biometric OptIn: onAuthenticationError: code: $errorCode, message: $errString"
                    )
                }

                override fun onAuthenticationFailed() {
                    onAuthenticationFailed()
                    CDCDebuggable.log(LOG_TAG, "Biometric OptIn: onAuthenticationFailed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    CDCDebuggable.log(LOG_TAG, "Biometric OptIn: onAuthenticationSucceeded")
                    val cryptoObjectCipher = result.cryptoObject?.cipher
                    if (cryptoObjectCipher == null) {
                        CDCDebuggable.log(
                            LOG_TAG,
                            "Biometric OptIn: onAuthenticationSucceeded - Error no Cipher"
                        )
                    }
                    // Encrypt session with biometric key.
                    biometricSecure(cryptoObjectCipher!!)
                    onAuthenticationSucceeded(result)
                }
            }
        )
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    //endregion

    //region OPT - OUT

    /**
     * Opt out from biometric session authentication.
     */
    fun optOutFromBiometricSessionAuthentication(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        onAuthenticationError: (errorCode: Int, errString: CharSequence) -> Unit,
        onAuthenticationFailed: () -> Unit,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        unlockBiometrics(
            optOut = true,
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            onAuthenticationError = onAuthenticationError,
            onAuthenticationFailed = onAuthenticationFailed,
            onAuthenticationSucceeded = onAuthenticationSucceeded
        )
    }

    //endregion

    //region LOCK/UNLOCK

    /**
     * Method will just clear the current session in the memory of the current session service instance.
     * Unlocking the session via biometric unlock will be required.
     */
    fun lockBiometricSession() {
        sessionService.clearSession()
    }

    /**
     * Unlock the session with biometric authentication.
     */
    fun unlockSessionWithBiometricAuthentication(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        onAuthenticationError: (errorCode: Int, errString: CharSequence) -> Unit,
        onAuthenticationFailed: () -> Unit,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        unlockBiometrics(
            optOut = false,
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            onAuthenticationError = onAuthenticationError,
            onAuthenticationFailed = onAuthenticationFailed,
            onAuthenticationSucceeded = onAuthenticationSucceeded
        )
    }

    //endregion

    //region GET SESSION ENTITY

    /**
     * Try to get the session entity from the secure storage.
     */
    private fun getSessionEntity(): SessionEntity? {
        val esp =
            sessionService.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val json =
            esp.getString(SessionSecure.CDC_SESSIONS, null)
        var sessionMap: MutableMap<String, String> = mutableMapOf()
        if (json != null) {
            sessionMap = Json.decodeFromString<MutableMap<String, String>>(json)
        }
        val sessionEntityJson = sessionMap[sessionService.siteConfig.apiKey] ?: return null
        val sessionEntity = Json.decodeFromString<SessionEntity>(sessionEntityJson)
        return sessionEntity
    }

    //endregion

}

