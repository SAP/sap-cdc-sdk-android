package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_LOGIN
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
    override suspend fun authenticate(): IAuthResponse {
        val loginResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_LOGIN)
        // Secure new session if the response does not contain any errors.
        if (!loginResponse.isError()) {
            secureNewSession(loginResponse)
        }
        return AuthResponse(loginResponse)
    }
}