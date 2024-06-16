package com.sap.cdc.android.sdk.authentication.flow

import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_LOGIN
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.api.Api

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class LoginAuthFlow(sessionService: SessionService) : AuthFlow(sessionService) {

    /**
     * Initiate login authentication flow.
     *
     * @see [accounts.login](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/683844d3c4b54104b2201efffdf558e3.html?q=accounts.getAccountInfo)
     */
    override suspend fun authenticate(): IAuthResponse {
        val loginResponse =
            Api(sessionService).genericSend(EP_ACCOUNTS_LOGIN)
        // Check errors.
        if (loginResponse.isError()) {
            response.failedAuthenticationWith(loginResponse.toCDCError())
        }
        // Success.
        return response.withAuthenticationData(loginResponse.asJson()!!)
    }
}