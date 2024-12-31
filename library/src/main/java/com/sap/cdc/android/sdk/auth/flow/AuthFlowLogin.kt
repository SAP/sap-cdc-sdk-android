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
    suspend fun login(parameters: MutableMap<String, String>): IAuthResponse {
        val login =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_LOGIN, parameters)

        // Prepare flow response
        val authResponse = AuthResponse(login)

        // Check resolvable flow state.
        val resolvableContext = initResolvableState(login)
        if (resolvableContext == null) {
            // No interruption in flow - secure the session - flow is successful.
            secureNewSession(login)
            return authResponse
        }

        // Flow ends with resolvable interruption.
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    /**
     * Initiate social login related authentication flow.
     * "NotifySocialLogin" call is used with social sign in flows (simple & link).
     */
    suspend fun notifySocialLogin(parameters: MutableMap<String, String>): IAuthResponse {
        val notifySocialLogin =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                parameters
            )

        // Prepare flow response
        val authResponse = AuthResponse(notifySocialLogin)
        val resolvableContext = initResolvableState(notifySocialLogin)
        if (resolvableContext == null) {
            // No interruption in flow - secure the session - flow is successful.
            secureNewSession(notifySocialLogin)
            return authResponse
        }

        // Flow ends with resolvable interruption.
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    /**
     * Send OTP code.
     * This call will initiate the OTP flow given provider parameters.
     * Either login or update is required to complete the flow using the "AuthResolvable" data
     * provided from this request.
     */
    suspend fun otpSendCode(parameters: MutableMap<String, String>): IAuthResponse {
        if (!parameters.containsKey("lang")) {
            parameters["lang"] = "en"
        }
        val otpSendCode =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_SEND_CODE,
                parameters
            )

        // Prepare flow response
        val authResponse = AuthResponse(otpSendCode)
        val resolvableContext = initResolvableState(otpSendCode)
            ?: // No interruption in flow. Flow is successful.
            return authResponse

        // Flow ends with resolvable interruption.
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    /**
     * OTP login to complete OTP authentication flow with given OTP code.
     */
    suspend fun otpLogin(parameters: MutableMap<String, String>): IAuthResponse {
        val otpLogin =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_LOGIN,
                parameters
            )

        // Prepare flow response
        val authResponse = AuthResponse(otpLogin)
        val resolvableContext = initResolvableState(otpLogin)
            ?: // No interruption in flow. Flow is successful.
            return authResponse

        // Flow ends with resolvable interruption.
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    /**
     * OTP update to complete OTP authentication flow with given OTP code.
     */
    suspend fun otpUpdate(parameters: MutableMap<String, String>): IAuthResponse {
        val otpUpdate =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_OTP_UPDATE,
                parameters
            )
        // Prepare flow response
        val authResponse = AuthResponse(otpUpdate)
        val resolvableContext = initResolvableState(otpUpdate)
            ?: // No interruption in flow. Flow is successful.
            return authResponse
        
        // Flow ends with resolvable interruption.
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

}