package com.sap.cdc.android.sdk.feature.login

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.model.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthLogin {

    // DSL methods with lambda receivers
    suspend fun credentials(
        credentials: Credentials,
        configure: AuthCallbacks.() -> Unit
    )

    suspend fun customIdentifier(
        credentials: CustomIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )
}

internal class AuthLogin(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthLogin {

    override suspend fun credentials(
        credentials: Credentials,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthLoginFlow(coreClient, sessionService).login(credentials, callbacks)
    }

    override suspend fun customIdentifier(
        credentials: CustomIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthLoginFlow(coreClient, sessionService).login(credentials, callbacks)
    }

    override suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthLoginFlow(coreClient, sessionService).login(parameters, callbacks)
    }

}
