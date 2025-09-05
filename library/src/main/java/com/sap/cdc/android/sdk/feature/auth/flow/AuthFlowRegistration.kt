package com.sap.cdc.android.sdk.feature.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_INIT_REGISTRATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_REGISTER
import com.sap.cdc.android.sdk.feature.auth.AuthResponse
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class RegistrationAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "RegistrationAuthFlow"
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
    suspend fun register(parameters: MutableMap<String, String>): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "register: with parameters:$parameters")
        // Add default finalize registration parameter.
        if (!parameters.containsKey("finalizeRegistration")) {
            parameters["finalizeRegistration"] = true.toString()
        }
        // Init registration.
        val initRegistration =
            AuthenticationApi(coreClient, sessionService).send(EP_ACCOUNTS_INIT_REGISTRATION)

        if (initRegistration.isError()) {
            return AuthResponse(initRegistration)
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

        // Prepare flow response
        val authResponse = AuthResponse(registration)

        // Check resolvable flow state.
        if (authResponse.isResolvable()) {
            val resolvableContext = initResolvableState(registration)
            if (resolvableContext != null) {
                CDCDebuggable.log(
                    LOG_TAG,
                    "register interrupted: resolvableContext:$resolvableContext"
                )
                // Flow ends with resolvable interruption.
                authResponse.resolvableContext = resolvableContext
            }
        } else if (authResponse.isError()) {
            CDCDebuggable.log(LOG_TAG, "register: error")
            return authResponse
        }

        CDCDebuggable.log(LOG_TAG, "register: success")
        // No interruption in flow - secure the session - flow is successful.
        secureNewSession(registration)
        return authResponse
    }

    /**
     *
     * Finalize registration flow.
     * If not requested at flow initiation or interrupted.
     *
     * @see [accounts.finalizeRegistration](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/228cd8bc68dc477094b3e0e9fe108e23.html?q=accounts.getAccountInfo)
     */
    suspend fun finalize(parameters: MutableMap<String, String>): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "finalize: with parameters:$parameters")
        val finalizeRegistrationResponse =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_FINALIZE_REGISTRATION,
                parameters
            )

        // Prepare flow response
        val authResponse = AuthResponse(finalizeRegistrationResponse)
        if (finalizeRegistrationResponse.isError()) {
            CDCDebuggable.log(LOG_TAG, "finalize: error")
            return authResponse
        }

        CDCDebuggable.log(LOG_TAG, "finalize: success")
        // Flow is successful - secure the session.
        secureNewSession(finalizeRegistrationResponse)
        return authResponse
    }

}
