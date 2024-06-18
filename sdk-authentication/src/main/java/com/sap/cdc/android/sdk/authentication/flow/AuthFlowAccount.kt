package com.sap.cdc.android.sdk.authentication.flow

import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.authentication.AuthEndpoints.Companion.EP_ACCOUNTS_SET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.authentication.AuthenticationApi
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class AccountAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    /**
     * Request updated account information.
     *
     * @see [accounts.getAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/cab69a86edae49e2be93fd51b78fc35b.html?q=accounts.getAccountInfo)
     */
    suspend fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        withParameters(parameters!!)
        val accountResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_GET_ACCOUNT_INFO, this.parameters)
        if (accountResponse.isError()) {
            response.failedAuthenticationWith(accountResponse.toCDCError())
        }
        return response.withAuthenticationData(accountResponse.asJson()!!)
    }

    /**
     * Update account information.
     * NOTE: some account parameters needs to be JSON serialized.
     *
     * @see [accounts.setAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41398a8670b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    suspend fun setAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        withParameters(parameters!!)
        val setAccountResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_ACCOUNTS_SET_ACCOUNT_INFO, this.parameters)
        if (setAccountResponse.isError()) {
            response.failedAuthenticationWith(setAccountResponse.toCDCError())
        }
        return getAccountInfo()
    }
}