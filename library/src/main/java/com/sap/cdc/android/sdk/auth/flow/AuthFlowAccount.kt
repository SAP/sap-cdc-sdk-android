package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_SET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient

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
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                this.parameters
            )
        return AuthResponse(accountResponse)
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
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_SET_ACCOUNT_INFO,
                this.parameters
            )
        return AuthResponse(setAccountResponse)
    }

    /**
     * Request conflicting accounts information.
     * NOTE: Call requires regToken due to interruption source.
     *
     * @see [accounts.getConflictingAccounts](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4134d7df70b21014bbc5a10ce4041860.html?q=conflictingAccounts)
     */
    suspend fun getConflictingAccounts(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        withParameters(parameters!!)
        val conflictingAccountsResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS,
            this.parameters
        )
        return AuthResponse(conflictingAccountsResponse)
    }
}