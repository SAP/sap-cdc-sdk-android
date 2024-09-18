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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */


enum class AuthState {
    ERROR, // Indicates an unresolvable error in the the API response.
    SUCCESS, // API success or end of flow.
    INTERRUPTED // Indicates an resolvable error occurred in the the API response. Flow can continue according to the error.
}

/**
 * Authentication response main class interface.
 */
interface IAuthResponse {

    fun cdcResponse(): CDCResponse
    fun asJsonString(): String?
    fun asJsonObject(): JsonObject?
    fun toDisplayError(): CDCError?
    fun state(): AuthState
}

/**
 * Authentication flow main response class.
 */
class AuthResponse(private val cdcResponse: CDCResponse) : IAuthResponse {

    override fun cdcResponse(): CDCResponse = cdcResponse

    override fun asJsonString(): String? = this.cdcResponse.asJson()

    override fun asJsonObject(): JsonObject? = this.cdcResponse.jsonObject

    private fun isError(): Boolean = cdcResponse.isError()

    override fun toDisplayError(): CDCError = this.cdcResponse.toCDCError()

    private fun isResolvable(): Boolean =
        AuthResolvable.resolvables.containsKey(cdcResponse.errorCode())

    /**
     * Defines flow state.
     * Success - marks the end of the flow.
     * Error - indicates an unresolvable error in the the API response.
     * Interrupted - indicates a continuation of the flow is available providing additional data/steps/
     */
    override fun state(): AuthState {
        if (isResolvable()) {
            return AuthState.INTERRUPTED
        }
        if (isError()) {
            return AuthState.ERROR
        }
        return AuthState.SUCCESS
    }
}

/**
 * Authentication APIs interface.
 */
interface IAuthApis {

    suspend fun register(
        parameters: MutableMap<String, String>
    ): IAuthResponse

    suspend fun login(
        parameters: MutableMap<String, String>
    ): IAuthResponse

    suspend fun providerLogin(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>? = mutableMapOf(),
    ): IAuthResponse

    suspend fun removeConnection(
        provider: String
    ): CDCResponse

    suspend fun logout(): CDCResponse
}

/**
 * Authentication APIs initiators.
 */
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
        return CDCResponse().fromJSON(authResponse.asJsonString()!!)
    }

}

/**
 * Authentication set providers interface.
 */
interface IAuthApisSet {

    suspend fun setAccountInfo(parameters: MutableMap<String, String>): IAuthResponse

}

/**
 * Authentication set providers initiators.
 */
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

/**
 * Authentication get providers interface.
 */
interface IAuthApisGet {
    suspend fun getAccountInfo(parameters: MutableMap<String, String>): IAuthResponse
}

/**
 * Authentication get providers initiators.
 */
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

/**
 * Available authentication resolvers interface.
 */
interface IAuthResolvers {

    /**
     * Finalize registration to complete registration process.
     */
    suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun getConflictingAccounts(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun linkAccount(parameters: MutableMap<String, String>): IAuthResponse

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    suspend fun pendingRegistrationWith(missingFields: MutableMap<String, String>): IAuthResponse

    fun getConflictingAccountsLoginProviders(authResponse: IAuthResponse): List<String>
}

/***
 * Authentication resolvers initiators.
 */
internal class AuthResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthResolvers {

    /**
     * Finalize registration to complete registration process.
     */
    override suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse {
        val resolver = RegistrationAuthFlow(coreClient, sessionService)
        resolver.withParameters(parameters)
        return resolver.finalize()
    }

    /**
     * Request conflicting accounts information required for linking to site/social account.
     */
    override suspend fun getConflictingAccounts(parameters: MutableMap<String, String>): IAuthResponse {
        val conflictingAccountsResolver = AccountAuthFlow(coreClient, sessionService)
        return conflictingAccountsResolver.getConflictingAccounts(parameters)
    }

    /**
     * Link account.
     */
    override suspend fun linkAccount(parameters: MutableMap<String, String>): IAuthResponse {
        return TODO("Provide the return value")
    }

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    override suspend fun pendingRegistrationWith(missingFields: MutableMap<String, String>): IAuthResponse {
        val setAccountResolver = AccountAuthFlow(coreClient, sessionService)
        val setAccountAuthResponse = setAccountResolver.setAccountInfo(missingFields)
        when (setAccountAuthResponse.state()) {
            AuthState.SUCCESS -> {
                // Error in flow.
                val finalizeRegistrationResolver = RegistrationAuthFlow(coreClient, sessionService)
                return finalizeRegistrationResolver.finalize()
            }

            else -> {
                return setAccountAuthResponse
            }
        }
    }

    /**
     * Get login providers list required for link account continuation flow.
     * The method will serialize the conflicting accounts response.
     */
    override fun getConflictingAccountsLoginProviders(authResponse: IAuthResponse): List<String> {
        val conflictingAccountJson = authResponse.asJsonObject()?.get("conflictingAccount")
            ?: return listOf()
        val loginProviders =
            authResponse.cdcResponse().json.decodeFromString<List<String>>(conflictingAccountJson.jsonObject["loginProviders"].toString())
        return loginProviders
    }

}
