package com.sap.cdc.android.sdk.feature.auth.flow.register

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.sequence.AuthRegisterResolvers
import com.sap.cdc.android.sdk.feature.auth.sequence.IAuthRegisterResolvers
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

interface IAuthRegister {

    fun resolve(): IAuthRegisterResolvers

    // DSL methods with lambda receivers
    suspend fun credentials(
        credentials: Credentials,
        configure: AuthCallbacks.() -> Unit,
        parameters: MutableMap<String, String> = mutableMapOf()
    )

    suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )
}

internal class AuthRegister(
    private val coreClient: CoreClient,
    private val sessionService: SessionService

) : IAuthRegister {

    override fun resolve(): IAuthRegisterResolvers =
        AuthRegisterResolvers(coreClient, sessionService)

    override suspend fun credentials(
        credentials: Credentials,
        configure: AuthCallbacks.() -> Unit,
        parameters: MutableMap<String, String>
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthRegisterFlow(coreClient, sessionService).register(credentials, callbacks)
    }

    override suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthRegisterFlow(coreClient, sessionService).register(parameters, callbacks)
    }
}