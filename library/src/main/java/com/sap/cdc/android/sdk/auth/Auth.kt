package com.sap.cdc.android.sdk.auth

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LogoutAuthFlow
import com.sap.cdc.android.sdk.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.auth.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
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

    suspend fun logout(): IAuthResponse
}

/**
 * Authentication APIs initiators.
 */
internal class AuthApis(
    private val coreClient: CoreClient, private val sessionService: SessionService
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
            coreClient, sessionService, authenticationProvider, WeakReference(hostActivity)
        )
        flow.withParameters(parameters ?: mutableMapOf())
        return flow.authenticate()
    }

    /**
     * Remove social connection from current account.
     */
    override suspend fun removeConnection(provider: String): CDCResponse = ProviderAuthFow(
        coreClient, sessionService
    ).removeConnection(provider)

    /**
     * Log out of current account.
     * Logging out will remove all session data.
     */
    override suspend fun logout(): IAuthResponse {
        val flow = LogoutAuthFlow(coreClient, sessionService)
        return flow.authenticate()
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

    private val coreClient: CoreClient, private val sessionService: SessionService
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
    private val coreClient: CoreClient, private val sessionService: SessionService
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

    suspend fun linkSiteAccount(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun linkSocialAccount(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>
    ): IAuthResponse

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse

    fun parseConflictingAccounts(authResponse: IAuthResponse): ConflictingAccountsEntity
}

/***
 * Authentication resolvers initiators.
 */
internal class AuthResolvers(
    private val coreClient: CoreClient, private val sessionService: SessionService
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
     * Link account using site credentials.
     * Will initiate a login call using loginMode = link.
     * RegToken is required.
     */
    override suspend fun linkSiteAccount(parameters: MutableMap<String, String>): IAuthResponse {
        val linkAccountResolver = LoginAuthFlow(coreClient, sessionService)
        parameters["loginMode"] = "link" // Making sure login mode is link
        linkAccountResolver.withParameters(parameters)
        val linkAccountResolverAuthResponse = linkAccountResolver.authenticate()
        when (linkAccountResolverAuthResponse.state()) {
            AuthState.INTERRUPTED -> {
                when (linkAccountResolverAuthResponse.cdcResponse().errorCode()) {
                    AuthResolvable.ERR_ACCOUNT_LINKED -> {
                        val finalizeRegistrationResolver =
                            RegistrationAuthFlow(coreClient, sessionService)
                        val regToken =
                            linkAccountResolverAuthResponse.cdcResponse().stringField("regToken")
                        finalizeRegistrationResolver.parameters["regToken"] = regToken!!
                        return finalizeRegistrationResolver.finalize()
                    }

                    else -> return linkAccountResolverAuthResponse
                }
            }

            else -> return linkAccountResolverAuthResponse
        }
    }

    /**
     * Link account using a social provider.
     * Will initiate a login call using loginMode = link.
     * RegToken is required.
     */
    override suspend fun linkSocialAccount(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val linkAccountResolver = ProviderAuthFow(
            coreClient, sessionService, authenticationProvider, WeakReference(hostActivity)
        )
        parameters["loginMode"] = "link"  // Making sure login mode is link
        linkAccountResolver.withParameters(parameters)
        return linkAccountResolver.authenticate()
    }

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    override suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse {
        val setAccountResolver = AccountAuthFlow(coreClient, sessionService)
        setAccountResolver.parameters["regToken"] = regToken
        val setAccountAuthResponse = setAccountResolver.setAccountInfo(missingFields)
        when (setAccountAuthResponse.state()) {
            AuthState.SUCCESS -> {
                // Error in flow.
                val finalizeRegistrationResolver = RegistrationAuthFlow(coreClient, sessionService)
                finalizeRegistrationResolver.parameters["regToken"] = regToken
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
    override fun parseConflictingAccounts(authResponse: IAuthResponse): ConflictingAccountsEntity {
        val conflictingAccountJson = authResponse.asJsonObject()?.get("conflictingAccount")
            ?: return ConflictingAccountsEntity(listOf())
        val caEntity = authResponse.cdcResponse().json.decodeFromString<ConflictingAccountsEntity>(
            conflictingAccountJson.jsonObject.toString()
        )
        return caEntity
    }

}
