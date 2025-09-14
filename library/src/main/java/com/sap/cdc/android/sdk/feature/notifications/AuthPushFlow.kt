package com.sap.cdc.android.sdk.feature.notifications

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNT_AUTH_DEVICE_REGISTER
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNT_AUTH_PUSH_VERIFY
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_DEVICE_INFO
import com.sap.cdc.android.sdk.feature.session.SessionService

class AuthPushFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthPushFlow"
    }

    suspend fun registerAuthDevice(authCallbacks: AuthCallbacks) {
        // Obtain device info from secure storage.
        val esp = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val deviceInfo = esp.getString(CDC_DEVICE_INFO, "") ?: ""

        CDCDebuggable.log(LOG_TAG, "registerDevice: with deviceInfo:$deviceInfo")
        val registerDevice = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNT_AUTH_DEVICE_REGISTER,
            mutableMapOf("deviceInfo" to deviceInfo)
        )

        // Error case
        if (registerDevice.isError()) {
            val authError = createAuthError(registerDevice)
            authCallbacks.onError?.invoke(authError)
            return
        }
        // Success case
        val authSuccess = createAuthSuccess(registerDevice)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }

    suspend fun verifyAuthPush(vToken: String, authCallbacks: AuthCallbacks) {
        CDCDebuggable.log(LOG_TAG, "verifyAuthPush: with vToken:$vToken")
        val verify = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNT_AUTH_PUSH_VERIFY,
            mutableMapOf("vToken" to vToken)
        )
        // Error case
        if (verify.isError()) {
            val authError = createAuthError(verify)
            authCallbacks.onError?.invoke(authError)
            return
        }
        // Success case
        val authSuccess = createAuthSuccess(verify)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }
}