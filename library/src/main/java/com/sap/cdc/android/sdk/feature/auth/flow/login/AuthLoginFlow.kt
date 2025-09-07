package com.sap.cdc.android.sdk.feature.auth.flow.login

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_ID_CREATE_TOKEN
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_LOGIN
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.flow.AuthError
import com.sap.cdc.android.sdk.feature.auth.flow.AuthFlow
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.model.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

class AuthLoginFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthLoginFlow"
    }

    suspend fun login(
        parameters: MutableMap<String, String>,
        callbacks: AuthCallbacks
    ) {
        CDCDebuggable.log(LOG_TAG, "login: with parameters:$parameters")

        try {
            val login = AuthenticationApi(coreClient, sessionService)
                .send(EP_ACCOUNTS_LOGIN, parameters)

            // Direct callback handling - no IAuthResponse creation
            handleLoginResponse(login, callbacks)

        } catch (exception: Exception) {
            val authError = AuthError(
                message = exception.message ?: "Unknown error occurred",
                code = null
            )
            callbacks.onError?.invoke(authError)
        }
    }

    suspend fun login(credentials: Credentials, callbacks: AuthCallbacks) {
        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        credentials.loginId?.let { parameters["loginID"] = it }
        credentials.aToken?.let { parameters["aToken"] = it }
        parameters["password"] = credentials.password
        login(parameters, callbacks)
    }

    suspend fun login(credentials: CustomIdCredentials, callbacks: AuthCallbacks) {
        CDCDebuggable.log(LOG_TAG, "login: with customID:$credentials")

        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        parameters["identifier"] = credentials.identifier
        parameters["identifierType"] = credentials.identifierType

        val createToken = AuthenticationApi(coreClient, sessionService)
            .send(
                EP_ACCOUNTS_ID_CREATE_TOKEN,
                parameters
            )
        // Error case
        if (createToken.isError()) {
            val authError = createAuthError(createToken)
            callbacks.onError?.invoke(authError)
            return
        }

        // Create token success case
        val aToken: String? = createToken.stringField("aToken")

        // Reuse parameters map
        parameters.clear()
        parameters["aToken"] = aToken ?: ""
        parameters["password"] = credentials.password
        login(parameters, callbacks)
    }

    private suspend fun handleLoginResponse(
        response: CDCResponse,
        callbacks: AuthCallbacks
    ) {
        if (isResolvableContext(response)) {
            // Resolvable interruption case
            handleResolvableInterruption(response, callbacks)
            return
        }
        if (response.isError()) {
            // Error case
            val authError = createAuthError(response)
            callbacks.onError?.invoke(authError)
            return
        }
        // Success case
        secureNewSession(response)
        val authSuccess = createAuthSuccess(response)
        callbacks.onSuccess?.invoke(authSuccess)
    }

}