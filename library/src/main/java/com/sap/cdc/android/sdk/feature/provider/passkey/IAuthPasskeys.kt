package com.sap.cdc.android.sdk.feature.provider.passkey

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthPasskeys {

    suspend fun create(
        authenticationProvider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun login(
        authenticationProvider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun get(
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun revoke(
        id: String,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}

internal class AuthPasskeys(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthPasskeys {

    override suspend fun create(
        authenticationProvider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPasskeysFlow(coreClient, sessionService, authenticationProvider).create(
            authCallbacks = callbacks
        )
    }

    override suspend fun login(
        authenticationProvider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPasskeysFlow(coreClient, sessionService, authenticationProvider).login(
            authCallbacks = callbacks
        )
    }

    override suspend fun get(authCallbacks: AuthCallbacks.() -> Unit) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPasskeysFlow(coreClient, sessionService).get(
            authCallbacks = callbacks
        )
    }

    override suspend fun revoke(
        id: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPasskeysFlow(coreClient, sessionService, null).revoke(
            id = id,
            authCallbacks = callbacks
        )
    }
}