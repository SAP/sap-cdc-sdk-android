package com.sap.cdc.android.sdk.authentication

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.authentication.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.authentication.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.authentication.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.model.CDCError
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IAuthResponse {

    fun authenticationJson(): String?
    fun authenticationError(): CDCError?
}

class AuthResponse : IAuthResponse {

    private var authenticationError: CDCError? = null
    private var authJson: String? = null

    override fun authenticationJson(): String? {
        return this.authJson
    }

    override fun authenticationError(): CDCError? {
        return this.authenticationError
    }

    fun failedAuthenticationWith(error: CDCError) = apply {
        this.authenticationError = error
    }

    fun withAuthenticationData(json: String) = apply {
        this.authJson = json
    }

    fun authenticationFailed(): Boolean = authenticationError != null

}

interface IAuthApis {

    suspend fun register(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun login(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun providerLogin(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>? = mutableMapOf(),
    ): IAuthResponse

    suspend fun removeConnection(provider: String): CDCResponse
}

internal class AuthApis(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthApis {

    /**
     * initiate credentials registration flow
     */
    override suspend fun register(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = RegistrationAuthFlow(coreClient, sessionService)
        flow.withParameters(parameters)
        return flow.authenticate()
    }

    /**
     * initiate credentials login flow.
     */
    override suspend fun login(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = LoginAuthFlow(coreClient, sessionService)
        flow.withParameters(parameters)
        return flow.authenticate()
    }

    /**
     * initiate provider authentication flow.
     */
    override suspend fun providerLogin(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>?
    ): IAuthResponse {
        val flow = ProviderAuthFow(
            coreClient,
            sessionService,
            authenticationProvider,
            WeakReference(hostActivity)
        )
        flow.withParameters(parameters ?: mutableMapOf())
        return flow.authenticate()
    }

    override suspend fun removeConnection(provider: String): CDCResponse =
        ProviderAuthFow(
            coreClient,
            sessionService
        ).removeConnection(provider)

}

interface IAuthApisSet {

    suspend fun setAccountInfo(parameters: MutableMap<String, String>): IAuthResponse

}

internal class AuthApisSet(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthApisSet {

    override suspend fun setAccountInfo(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        flow.withParameters(parameters)
        return flow.setAccountInfo()
    }

}

interface IAuthApisGet {

    suspend fun getAccountInfo(parameters: MutableMap<String, String>): IAuthResponse

}

internal class AuthApisGet(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthApisGet {

    /**
     * Request account information..
     */
    override suspend fun getAccountInfo(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        flow.withParameters(parameters)
        return flow.getAccountInfo()
    }

}

interface IAuthResolvers {

    suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun linkAccount(parameters: MutableMap<String, String>): IAuthResponse
}

internal class AuthResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthResolvers {

    override suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse {
        val resolver = RegistrationAuthFlow(coreClient, sessionService)
        resolver.withParameters(parameters)
        return resolver.finalize()
    }

    override suspend fun linkAccount(parameters: MutableMap<String, String>): IAuthResponse {
        TODO("Not yet implemented")
    }

}
