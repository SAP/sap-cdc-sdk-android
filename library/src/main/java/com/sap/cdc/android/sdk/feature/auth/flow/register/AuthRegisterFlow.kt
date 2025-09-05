package com.sap.cdc.android.sdk.feature.auth.flow.register

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_INIT_REGISTRATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_REGISTER
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.flow.AuthFlow
import com.sap.cdc.android.sdk.feature.auth.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

class AuthRegisterFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthRegisterFlow"
    }

    suspend fun register(
        credentials: Credentials,
        callbacks: AuthCallbacks
    ) {
        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        credentials.email?.let { parameters["email"] = it }
        parameters["password"] = credentials.password
        register(parameters, callbacks)
    }

    /**
     * Initiate registration authentication flow.
     * Flow consists of the following api calls:
     *
     * 1. init registration.
     * @see [accounts.initRegistration](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4136e1f370b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     *
     * 2. registration.
     * @see [accounts.register](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/1fe26c820cd145cd8c927a497c33d935.html?q=accounts.getAccountInfo)
     *
     * 3. Finalize registration (True by default unless requested otherwise).
     * @see [accounts.finalizeRegistration](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/228cd8bc68dc477094b3e0e9fe108e23.html?q=accounts.getAccountInfo)
     */
    suspend fun register(parameters: MutableMap<String, String>, callbacks: AuthCallbacks) {
        CDCDebuggable.log(
            LOG_TAG,
            "register: with parameters:$parameters"
        )

        // If not specified, registration will be finalized by default.
        if (!parameters.containsKey("finalizeRegistration")) {
            parameters["finalizeRegistration"] = true.toString()
        }

        // Init registration.
        val initRegistration =
            AuthenticationApi(coreClient, sessionService).send(EP_ACCOUNTS_INIT_REGISTRATION)

        if (initRegistration.isError()) {
            val authError = createAuthError(initRegistration)
            callbacks.onError?.invoke(authError)
            return
        }

        // Fetch regToken. Required parameter for flow.
        val regToken = initRegistration.stringField("regToken")

        // Actual registration call using original provided parameters.
        parameters["regToken"] = regToken!!
        val registration =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_REGISTER,
                parameters
            )

        if (isResolvableContext(registration)) {
            // Resolvable interruption case
            handleResolvableInterruption(registration, callbacks)
            return
        }
        if (registration.isError()) {
            // Error case
            val authError = createAuthError(registration)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        CDCDebuggable.log(LOG_TAG, "register: success")
        // No interruption in flow - secure the session - flow is successful.
        secureNewSession(registration)

        callbacks.onSuccess?.invoke(createAuthSuccess(registration))
    }

    /**
     *
     * Finalize registration flow.
     * If not requested at flow initiation or interrupted.
     *
     * @see [accounts.finalizeRegistration](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/228cd8bc68dc477094b3e0e9fe108e23.html?q=accounts.getAccountInfo)
     */
    suspend fun finalize(parameters: MutableMap<String, String>, callbacks: AuthCallbacks) {
        CDCDebuggable.log(
            RegistrationAuthFlow.Companion.LOG_TAG,
            "finalize: with parameters:$parameters"
        )
        val response =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_FINALIZE_REGISTRATION,
                parameters
            )

        if (response.isError()) {
            val authError = createAuthError(response)
            callbacks.onError?.invoke(authError)
            return
        }
        // Success case
        secureNewSession(response)
        callbacks.onSuccess?.invoke(createAuthSuccess(response))
    }
}