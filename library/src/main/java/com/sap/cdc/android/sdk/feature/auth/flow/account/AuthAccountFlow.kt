package com.sap.cdc.android.sdk.feature.auth.flow.account

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_ID_TOKEN_EXCHANGE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_SET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.flow.AuthFlow
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

class AuthAccountFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AccountAuthFlow"
    }

    /**
     * Request updated account information.
     *
     * @see [accounts.getAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/cab69a86edae49e2be93fd51b78fc35b.html?q=accounts.getAccountInfo)
     */
    suspend fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        callbacks: AuthCallbacks
    ) {
        CDCDebuggable.log(
            AccountAuthFlow.Companion.LOG_TAG,
            "getAccountInfo: with parameters:$parameters"
        )

        val request =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                parameters!!
            )
        // Error case
        if (request.isError()) {
            val authError = createAuthError(request)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(request)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * Update account information with DSL callback support and interception capability.
     * NOTE: some account parameters needs to be JSON serialized.
     *
     * @see [accounts.setAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41398a8670b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    suspend fun setAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        refreshOnSuccess: Boolean = false,
        callbacks: AuthCallbacks,
    ) {
        CDCDebuggable.log(
            AccountAuthFlow.Companion.LOG_TAG,
            "setAccountInfo: with parameters:$parameters"
        )

        val request =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_SET_ACCOUNT_INFO,
                parameters ?: mutableMapOf()
            )

        // Error case
        if (request.isError()) {
            val authError = createAuthError(request)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        if (!refreshOnSuccess) {
            // If not refreshing, return the current response
            val authSuccess = createAuthSuccess(request)
            callbacks.onSuccess?.invoke(authSuccess)
            return
        }

        // Perform a getAccountInfo to refresh the data
        getAccountInfo(mutableMapOf(), callbacks)
    }

    /**
     * Applications (mobile/web) within the same site group are now able to share a session from the mobile application
     * to a web page running the JS SDK.
     *
     * Request code required to exchange the session.
     */
    suspend fun getAuthCode(
        parameters: MutableMap<String, String>,
        callbacks: AuthCallbacks
    ) {
        parameters["response_type"] = "code"
        CDCDebuggable.log(
            AccountAuthFlow.Companion.LOG_TAG,
            "getAuthCode: with parameters:$parameters"
        )

        val request = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNTS_ID_TOKEN_EXCHANGE,
            parameters
        )
        // Error case
        if (request.isError()) {
            val authError = createAuthError(request)
            callbacks.onError?.invoke(authError)
            return
        }
        // Success case
        val authSuccess = createAuthSuccess(request)
        callbacks.onSuccess?.invoke(authSuccess)
    }
}
