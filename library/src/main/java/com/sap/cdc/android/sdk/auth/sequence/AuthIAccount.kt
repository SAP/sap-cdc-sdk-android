package com.sap.cdc.android.sdk.auth.sequence

import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient

interface IAuthAccount {

    suspend fun get(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun set(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun authCode(parameters: MutableMap<String, String>): IAuthResponse
}

internal class AuthAccount(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthAccount {

    /**
     * Request account information..
     */
    override suspend fun get(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        return flow.getAccountInfo(parameters)
    }

    override suspend fun set(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        return flow.setAccountInfo(parameters)
    }

    /**
     * Request session exchange auth code.
     */
    override suspend fun authCode(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        return flow.getAuthCode(parameters)
    }
}

