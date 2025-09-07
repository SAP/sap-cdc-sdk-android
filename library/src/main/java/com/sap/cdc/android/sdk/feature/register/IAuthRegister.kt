package com.sap.cdc.android.sdk.feature.register

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.session.SessionService

//region REGISTER INTERFACE

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

    override suspend fun credentials(
        credentials: Credentials,
        configure: AuthCallbacks.() -> Unit,
        parameters: MutableMap<String, String>
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthRegisterFlow(coreClient, sessionService).register(
            credentials = credentials,
            parameters = parameters,
            callbacks = callbacks
        )
    }

    override suspend fun parameters(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthRegisterFlow(coreClient, sessionService).register(parameters, callbacks)
    }

    override fun resolve(): IAuthRegisterResolvers =
        AuthRegisterResolvers(coreClient, sessionService)

}

//endregion

//region REGISTER INTERFACE RESOLVERS

interface IAuthRegisterResolvers {

    /**
     * Finalize registration process interface.
     */
    suspend fun finalize(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    )
}

internal class AuthRegisterResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthRegisterResolvers {

    /**
     * Finalize registration process implementation.
     */
    override suspend fun finalize(
        parameters: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        AuthRegisterFlow(coreClient, sessionService).finalize(parameters, callbacks)
    }

    override suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>,
        configure: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(configure)
        missingFields["regToken"] = regToken
        AuthRegisterFlow(coreClient, sessionService).resolveWith(missingFields, callbacks)
    }
}

//endregion

