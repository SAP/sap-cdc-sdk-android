package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_ID_TOKEN_EXCHANGE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_SET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNT_AUTH_DEVICE_REGISTER
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_DEVICE_INFO
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class AccountAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AccountAuthFlow"
    }

    /**
     * Request updated account information.
     *
     * @see [accounts.getAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/cab69a86edae49e2be93fd51b78fc35b.html?q=accounts.getAccountInfo)
     */
    suspend fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getAccountInfo: with parameters:$parameters")
        val getAccountInfo =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                parameters!!
            )
        return AuthResponse(getAccountInfo)
    }

    /**
     * Update account information.
     * NOTE: some account parameters needs to be JSON serialized.
     *
     * @see [accounts.setAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41398a8670b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    suspend fun setAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "setAccountInfo: with parameters:$parameters")
        val setAccount =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_SET_ACCOUNT_INFO,
                parameters ?: mutableMapOf()
            )
        return AuthResponse(setAccount)
    }

    /**
     * Request conflicting accounts information.
     * NOTE: Call requires regToken due to interruption source.
     *
     * @see [accounts.getConflictingAccounts](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4134d7df70b21014bbc5a10ce4041860.html?q=conflictingAccounts)
     */
    suspend fun getConflictingAccounts(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getConflictingAccounts: with parameters:$parameters")
        val getConflictingAccounts = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS,
            parameters ?: mutableMapOf()
        )
        return AuthResponse(getConflictingAccounts)
    }

    /**
     * Applications (mobile/web) within the same site group are now able to share a session from the mobile application
     * to a web page running the JS SDK.
     *
     * Request code required to exchange the session.
     */
    suspend fun getAuthCode(parameters: MutableMap<String, String>): IAuthResponse {
        parameters["response_type"] = "code"
        CDCDebuggable.log(LOG_TAG, "getAuthCode: with parameters:$parameters")
        val tokenExchange = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_ID_TOKEN_EXCHANGE,
            parameters
        )
        return AuthResponse(tokenExchange)
    }

    suspend fun registerAuthDevice(): IAuthResponse {
        // Obtain device info from secure storage.
        val esp = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val deviceInfo = esp.getString(CDC_DEVICE_INFO, "") ?: ""

        CDCDebuggable.log(LOG_TAG, "registerDevice: with deviceInfo:$deviceInfo")
        val registerDevice = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNT_AUTH_DEVICE_REGISTER,
            mutableMapOf("deviceInfo" to deviceInfo)
        )
        return AuthResponse(registerDevice)
    }

    suspend fun verifyAuthPush(vToken: String): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "verifyAuthPush: with vToken:$vToken")
        val verifyAuthPush = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNT_AUTH_DEVICE_REGISTER,
            mutableMapOf("vToken" to vToken)
        )
        return AuthResponse(verifyAuthPush)
    }

}