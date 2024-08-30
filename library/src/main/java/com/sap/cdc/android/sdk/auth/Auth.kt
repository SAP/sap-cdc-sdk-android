package com.sap.cdc.android.sdk.auth

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LogoutAuthFlow
import com.sap.cdc.android.sdk.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.auth.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionService
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
    fun isResolvable(): Boolean
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

    override fun isResolvable(): Boolean =
        AuthResolvable.resolvables.containsKey(authenticationError?.errorCode)


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

    suspend fun logout(): CDCResponse
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

    /**
     * Remove social connection from current account.
     */
    override suspend fun removeConnection(provider: String): CDCResponse =
        ProviderAuthFow(
            coreClient,
            sessionService
        ).removeConnection(provider)

    /**
     * Log out of current account.
     * Logging out will remove all session data.
     */
    override suspend fun logout(): CDCResponse {
        val flow = LogoutAuthFlow(coreClient, sessionService)
        val authResponse: IAuthResponse = flow.authenticate()
        return CDCResponse().fromJSON(authResponse.authenticationJson()!!)
    }

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

    suspend fun pendingRegistrationWith(parameters: MutableMap<String, String>): IAuthResponse
}

internal class AuthResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthResolvers {

    /**
     * Finalize pending registration.
     */
    override suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse {
        val resolver = RegistrationAuthFlow(coreClient, sessionService)
        resolver.withParameters(parameters)
        return resolver.finalize()
    }

    /**
     * Link account.
     */
    override suspend fun linkAccount(parameters: MutableMap<String, String>): IAuthResponse {
        TODO("Not yet implemented")
    }

    override suspend fun pendingRegistrationWith(parameters: MutableMap<String, String>): IAuthResponse {
        val setAccountResolver = AccountAuthFlow(coreClient, sessionService)
        val setAccountAuthResponse = setAccountResolver.setAccountInfo(parameters)
        if (setAccountAuthResponse.authenticationError() != null) {
            // Error in flow.
            return setAccountAuthResponse
        }
        val finalizeRegistrationResolver = RegistrationAuthFlow(coreClient, sessionService)
        return finalizeRegistrationResolver.finalize()
    }

}
