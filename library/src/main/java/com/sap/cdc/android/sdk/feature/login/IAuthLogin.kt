package com.sap.cdc.android.sdk.feature.login

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.ATokenCredentials
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.LoginIdCredentials
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthLogin {

    suspend fun withLoginId(
        credentials: LoginIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun withToken(
        credentials: ATokenCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun withCustomIdentifier(
        credentials: CustomIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}

internal class AuthLogin(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthLogin {

    override suspend fun withLoginId(
        credentials: LoginIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthLoginFlow(coreClient, sessionService)
            .login(credentials, callbacks)
    }

    override suspend fun withToken(
        credentials: ATokenCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthLoginFlow(coreClient, sessionService)
            .login(credentials, callbacks)
    }

    override suspend fun withCustomIdentifier(
        credentials: CustomIdCredentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthLoginFlow(coreClient, sessionService).login(credentials, callbacks)
    }

    internal suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthLoginFlow(coreClient, sessionService).login(parameters, callbacks)
    }

}
