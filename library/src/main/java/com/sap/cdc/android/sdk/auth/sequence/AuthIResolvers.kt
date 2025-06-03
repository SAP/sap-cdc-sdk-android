package com.sap.cdc.android.sdk.auth.sequence

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.auth.model.LinkEntities
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.lang.ref.WeakReference

/**
 * Available authentication resolvers interface.
 */
interface IAuthResolvers {

    /**
     * Get conflicting accounts interface.
     */
    suspend fun getConflictingAccounts(parameters: MutableMap<String, String>): IAuthResponse

    /**
     * Link account using site credentials interface.
     */
    suspend fun linkSiteAccount(
        parameters: MutableMap<String, String>,
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    /**
     * Link account using a social provider interface.
     */
    suspend fun linkSocialAccount(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    fun parseConflictingAccounts(authResponse: IAuthResponse): LinkEntities
}

/***
 * Authentication resolvers initiators.
 */
internal class AuthResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService,
) : IAuthResolvers {

    /**
     * Request conflicting accounts information required for linking to site/social account implementation.
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
    override suspend fun linkSiteAccount(
        parameters: MutableMap<String, String>,
        resolvableContext: ResolvableContext,
    ): IAuthResponse {
        val linkAccountResolver = LoginAuthFlow(coreClient, sessionService)
        parameters["loginMode"] = "link" // Making sure login mode is link
        val linkAccountResolverAuthResponse = linkAccountResolver.login(parameters)
        return when (linkAccountResolverAuthResponse.state()) {
            AuthState.SUCCESS -> {
                connectAccount(
                    resolvableContext.linking?.provider,
                    resolvableContext.linking?.authToken
                )
            }

            else -> linkAccountResolverAuthResponse
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
        resolvableContext: ResolvableContext,
    ): IAuthResponse {
        val linkAccountResolver = ProviderAuthFow(
            coreClient, sessionService, authenticationProvider, WeakReference(hostActivity)
        )
        val linkAccountResolverAuthResponse =
            linkAccountResolver.signIn(mutableMapOf("provider" to resolvableContext.linking?.provider!!))
        return when (linkAccountResolverAuthResponse.state()) {
            AuthState.SUCCESS -> {
                connectAccount(
                    resolvableContext.linking?.provider,
                    resolvableContext.linking?.authToken
                )
            }

            else -> linkAccountResolverAuthResponse
        }
    }

    /**
     * Get login providers list required for link account continuation flow.
     * The method will serialize the conflicting accounts response.
     */
    override fun parseConflictingAccounts(authResponse: IAuthResponse): LinkEntities {
        val conflictingAccountJson = authResponse.asJsonObject()?.get("conflictingAccount")
            ?: return LinkEntities(listOf())
        val caEntity = authResponse.cdcResponse().json.decodeFromString<LinkEntities>(
            conflictingAccountJson.jsonObject.toString()
        )
        return caEntity
    }

    /**
     * Connect accounts.
     * This method is the last step of the linking account flow.
     */
    private suspend fun connectAccount(provider: String?, authToken: String?): IAuthResponse {
        val json = JsonObject(
            mapOf(
                "provider" to JsonPrimitive(provider),
                "authToken" to JsonPrimitive(authToken),
            )
        )
        val providerSession = json.toString()
        val parameters =
            mutableMapOf("providerSession" to providerSession, "loginMode" to "connect")

        val connectResolver = LoginAuthFlow(coreClient, sessionService)
        val authResponse = connectResolver.notifySocialLogin(parameters)
        return authResponse
    }

}