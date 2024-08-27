package com.sap.cdc.android.sdk.auth.flow

import android.webkit.CookieManager
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient


/**
 * Created by Tal Mirmelshtein on 15/06/2024
 * Copyright: SAP LTD.
 */

class LogoutAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    com.sap.cdc.android.sdk.auth.flow.AuthFlow(coreClient, sessionService) {

    /**
     * Initiate logout flow.
     *
     * @see [accounts.logout](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41376ba570b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    override suspend fun authenticate(): IAuthResponse {
        val logoutResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_LOGOUT)
        // Check errors.
        if (logoutResponse.isError()) {
            response.failedAuthenticationWith(logoutResponse.toCDCError())
        }
        // Success.
        //TODO: Determine if additional data purge is required.
        clearCookies()
        return response.withAuthenticationData(logoutResponse.asJson()!!)
    }

    private fun clearCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()
    }
}
