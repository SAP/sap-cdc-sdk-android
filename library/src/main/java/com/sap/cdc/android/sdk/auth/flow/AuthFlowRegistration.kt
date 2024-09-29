package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_INIT_REGISTRATION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_REGISTER
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class RegistrationAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

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
    override suspend fun authenticate(): IAuthResponse {
        // Add default finalize registration parameter.
        if (!parameters.containsKey("finalizeRegistration")) {
            parameters["finalizeRegistration"] = true.toString()
        }
        // Init registration.
        val initResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_INIT_REGISTRATION)
        // Check errors.
        if (!initResponse.isError()) {
            // Fetch regToken.
            // Required parameter for flow.
            val regToken = initResponse.stringField("regToken")
            // Actual registration call.
            parameters["regToken"] = regToken!!
            val registrationResponse =
                AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_ACCOUNTS_REGISTER,
                    parameters
                )
            if (!registrationResponse.isError()) {
                secureNewSession(registrationResponse)
            }
            return AuthResponse(registrationResponse)
        }
        return AuthResponse(initResponse)
    }

    /**
     *
     * Finalize registration flow.
     * If not requested at flow initiation or interrupted.
     *
     * @see [accounts.finalizeRegistration](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/228cd8bc68dc477094b3e0e9fe108e23.html?q=accounts.getAccountInfo)
     */
    suspend fun finalize(): IAuthResponse {
        val finalizeRegistrationResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_FINALIZE_REGISTRATION,
                parameters
            )
        if (!finalizeRegistrationResponse.isError()) {
            secureNewSession(finalizeRegistrationResponse)
        }
        return AuthResponse(finalizeRegistrationResponse)
    }

}
