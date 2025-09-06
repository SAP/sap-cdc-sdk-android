package com.sap.cdc.android.sdk.feature.auth.flow.account

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

interface IAuthAccount {

    suspend fun get(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )

    suspend fun set(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )

    suspend fun authCode(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )
}

internal class AuthAccount(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthAccount {

    override suspend fun get(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthAccountFlow(coreClient, sessionService).getAccountInfo(parameters, callbacks)
    }

    override suspend fun set(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthAccountFlow(coreClient, sessionService).setAccountInfo(parameters, callbacks)
    }

    override suspend fun authCode(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthAccountFlow(coreClient, sessionService).getAuthCode(parameters, callbacks)
    }

}

