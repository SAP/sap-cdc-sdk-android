package com.sap.cdc.android.sdk.feature.otp

import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OTP_LOGIN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OTP_SEND_CODE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OTP_UPDATE
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService

class AuthOtpFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthOtpFlow"
    }

    /**
     * Send OTP code.
     * This call will initiate the OTP flow given provider parameters.
     * Either login or update is required to complete the flow using the "AuthResolvable" data
     * provided from this request.
     * @see [accounts.otp.sendCode](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4137e1be70b21014bbc5a10ce4041860.html?q=accounts.otp.sendCode)
     */
    suspend fun otpSendCode(parameters: MutableMap<String, String>, callbacks: AuthCallbacks) {
        CIAMDebuggable.log(LOG_TAG, "otpSendCode: with parameters:$parameters")

        if (!parameters.containsKey("lang")) {
            parameters["lang"] = "en"
        }

        val sendCode =
            AuthenticationApi(coreClient, sessionService).send(
                EP_OTP_SEND_CODE,
                parameters
            )

        // Interruption case
        if (isResolvableContext(sendCode)) {
            handleResolvableInterruption(sendCode, callbacks)
            return
        }

        // Error case
        if (sendCode.isError()) {
            val authError = createAuthError(sendCode)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(sendCode)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * OTP login to complete OTP authentication flow with given OTP code.
     * @see [accounts.otp.login](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4137bbe870b21014bbc5a10ce4041860.html?q=accounts.otp.login)
     */
    suspend fun otpLogin(parameters: MutableMap<String, String>, callbacks: AuthCallbacks) {
        CIAMDebuggable.log(LOG_TAG, "otpLogin: with parameters:$parameters")
        val otpLogin =
            AuthenticationApi(coreClient, sessionService).send(
                EP_OTP_LOGIN,
                parameters
            )

        // Interruption case
        if (isResolvableContext(otpLogin)) {
            handleResolvableInterruption(otpLogin, callbacks)
            return
        }

        // Error case
        if (otpLogin.isError()) {
            val authError = createAuthError(otpLogin)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        secureNewSession(otpLogin)
        val authSuccess = createAuthSuccess(otpLogin)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * OTP update to complete OTP authentication flow with given OTP code.
     * @see [accounts.otp.update](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/413807a270b21014bbc5a10ce4041860.html?q=accounts.otp.update)]
     */
    suspend fun otpUpdate(parameters: MutableMap<String, String>, callbacks: AuthCallbacks) {
        CIAMDebuggable.log(LOG_TAG, "otpUpdate: with parameters:$parameters")
        val otpUpdate =
            AuthenticationApi(coreClient, sessionService).send(
                EP_OTP_UPDATE,
                parameters
            )

        // Interruption case
        if (isResolvableContext(otpUpdate)) {
            handleResolvableInterruption(otpUpdate, callbacks)
            return
        }

        // Error case
        if (otpUpdate.isError()) {
            val authError = createAuthError(otpUpdate)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        secureNewSession(otpUpdate)
        val authSuccess = createAuthSuccess(otpUpdate)
        callbacks.onSuccess?.invoke(authSuccess)
    }
}