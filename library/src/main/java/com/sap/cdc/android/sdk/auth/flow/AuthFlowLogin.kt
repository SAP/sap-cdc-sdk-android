package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_LOGIN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OTP_LOGIN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OTP_SEND_CODE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OTP_UPDATE
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class LoginAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    /**
     * Initiate login authentication flow.
     *
     * @see [accounts.login](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/683844d3c4b54104b2201efffdf558e3.html?q=accounts.getAccountInfo)
     */
    suspend fun login(): IAuthResponse {
        val loginResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_LOGIN, parameters)
        // Secure new session if the response does not contain any errors.
        if (!loginResponse.isError()) {
            secureNewSession(loginResponse)
        }
        val authResponse = AuthResponse(loginResponse)
        // Check resolvable flow state.
        initResolvableState(authResponse)
        return authResponse
    }

    /**
     * Initiate social login related authentication flow.
     * "NotifySocialLogin" call is used with social sign in flows (simple & link).
     */
    suspend fun notifySocialLogin(): IAuthResponse {
        val notifyResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                parameters
            )
        if (!notifyResponse.isError()) {
            secureNewSession(notifyResponse)
        }
        val authResponse = AuthResponse(notifyResponse)
        return authResponse
    }

    /**
     * Sign in using phone number.
     * This call will initiate the flow for phone number sign in.
     * Either login or update is required to complete the flow using the "AuthResolvable" data
     * provided from this request.
     */
    suspend fun otpSignIn(): IAuthResponse {
        val sendCodeResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_SEND_CODE,
                parameters
            )
        if (!parameters.containsKey("lang")) {
            parameters["lang"] = "en"
        }
        val authResponse = AuthResponse(sendCodeResponse)
        initResolvableState(authResponse)
        return authResponse
    }

    suspend fun otpLogin(): IAuthResponse {
        val phoneLoginResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_LOGIN,
                parameters
            )
        val authResponse = AuthResponse(phoneLoginResponse)
        initResolvableState(authResponse)
        return authResponse
    }

    suspend fun otpUpdate(): IAuthResponse {
        val phoneLoginUpdate =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_UPDATE,
                parameters
            )
        val authResponse = AuthResponse(phoneLoginUpdate)
        initResolvableState(authResponse)
        return authResponse
    }

}