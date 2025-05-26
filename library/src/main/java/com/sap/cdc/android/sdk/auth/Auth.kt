package com.sap.cdc.android.sdk.auth

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.auth.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.auth.flow.TFAAuthFlow
import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.auth.tfa.TFAPhoneMethod
import com.sap.cdc.android.sdk.auth.tfa.TFAProvider
import com.sap.cdc.android.sdk.auth.tfa.TFAProvidersEntity
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.lang.ref.WeakReference

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */


/**
 * Authentication result state enum.
 * ERROR - Indicates an unresolvable error in the the API response.
 * SUCCESS - API success or end of flow.
 * INTERRUPTED - Indicates an resolvable error occurred in the the API response. Flow can continue according to the error.
 */
enum class AuthState {
    ERROR, SUCCESS, INTERRUPTED
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
    fun resolvable(): ResolvableContext?
}

/**
 * Authentication flow main response class.
 */
class AuthResponse(private val cdcResponse: CDCResponse) : IAuthResponse {

    var resolvableContext: ResolvableContext? = null

    override fun cdcResponse(): CDCResponse = cdcResponse

    override fun asJsonString(): String? = this.cdcResponse.asJson()

    override fun asJsonObject(): JsonObject? = this.cdcResponse.jsonObject

    internal fun isError(): Boolean = cdcResponse.isError()

    override fun toDisplayError(): CDCError = this.cdcResponse.toCDCError()

    fun isResolvable(): Boolean =
        ResolvableContext.resolvables.containsKey(cdcResponse.errorCode()) || cdcResponse.containsKey(
            "vToken"
        )

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

    /**
     * Get reference to resolvable data entity.
     * This data is required to complete interrupted flows.
     */
    override fun resolvable(): ResolvableContext? = resolvableContext
}

//region IAuthResolvers

/**
 * Available authentication resolvers interface.
 */
interface IAuthResolvers {

    /**
     * Finalize registration process interface.
     */
    suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse

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

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse


    suspend fun otpLogin(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse

    suspend fun otpUpdate(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse

    fun parseConflictingAccounts(authResponse: IAuthResponse): ConflictingAccountsEntity
}

/***
 * Authentication resolvers initiators.
 */
internal class AuthResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService,
) : IAuthResolvers {

    /**
     * Finalize registration process implementation.
     */
    override suspend fun finalizeRegistration(parameters: MutableMap<String, String>): IAuthResponse {
        val resolver = RegistrationAuthFlow(coreClient, sessionService)
        return resolver.finalize(parameters)
    }

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
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    override suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse {
        val setAccountResolver = AccountAuthFlow(coreClient, sessionService)
        missingFields["regToken"] = regToken
        val setAccountAuthResponse = setAccountResolver.setAccountInfo(missingFields)
        when (setAccountAuthResponse.state()) {
            AuthState.SUCCESS -> {
                // Error in flow.
                val finalizeRegistrationResolver = RegistrationAuthFlow(coreClient, sessionService)
                return finalizeRegistrationResolver.finalize(mutableMapOf("regToken" to regToken))
            }

            else -> {
                return setAccountAuthResponse
            }
        }
    }

    /**
     * Resolve phone login flow using provided code/vToken available in the "AuthResolvable" entity.
     */
    override suspend fun otpLogin(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val codeVerify = LoginAuthFlow(coreClient, sessionService)
        return codeVerify.otpLogin(
            mutableMapOf(
                "vToken" to resolvableContext.otp?.vToken!!,
                "code" to code
            )
        )
    }

    /**
     * Resolve phone update flow provided code/vToken available in the "AuthResolvable" entity.
     */
    override suspend fun otpUpdate(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val codeVerify = LoginAuthFlow(coreClient, sessionService)
        return codeVerify.otpUpdate(
            mutableMapOf(
                "vToken" to resolvableContext.otp?.vToken!!,
                "code" to code
            )
        )
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

//endregion

//region IAuthTFA

interface IAuthTFA {

    suspend fun getProviders(regToken: String): IAuthResponse

    fun parseTFAProviders(authResponse: IAuthResponse): TFAProvidersEntity

    suspend fun optInForPushAuthentication(): IAuthResponse

    suspend fun finalizeOtpInForPushAuthentication(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    suspend fun sendEmailCode(
        resolvableContext: ResolvableContext,
        emailAddress: String,
        language: String?
    ): IAuthResponse

    suspend fun registerPhone(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS
    ): IAuthResponse

    suspend fun registerTOTP(
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    suspend fun getRegisteredPhoneNumbers(resolvableContext: ResolvableContext): IAuthResponse

    suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        phoneId: String,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS,
        language: String? = "en"
    ): IAuthResponse

    suspend fun verifyEmailCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false,
    ): IAuthResponse

    suspend fun verifyPhoneCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false
    ): IAuthResponse

    suspend fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false
    ): IAuthResponse
}

internal class AuthTFA(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthTFA {

    override suspend fun getProviders(regToken: String): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getTFAProviders(mutableMapOf("regToken" to regToken))
    }

    override fun parseTFAProviders(authResponse: IAuthResponse): TFAProvidersEntity {
        val parsedEntity = authResponse.cdcResponse().json.decodeFromString<TFAProvidersEntity>(
            authResponse.asJsonString()!!
        )
        return parsedEntity
    }

    override suspend fun optInForPushAuthentication(): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.PUSH.value, "mode" to "register"
        )
        return tfaFlow.optInForPushTFA(parameters = parameters)
    }

    override suspend fun finalizeOtpInForPushAuthentication(
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.finalizeOptInForPushTFA(parameters)
    }

    override suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyPushTFA(parameters)
    }

    override suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getRegisteredEmails(
            resolvableContext,
            mutableMapOf("provider" to TFAProvider.EMAIL.value, "mode" to "verify")
        )
    }

    override suspend fun sendEmailCode(
        resolvableContext: ResolvableContext,
        emailAddress: String,
        language: String?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.sendEmailCode(
            resolvableContext,
            mutableMapOf("emailID" to emailAddress, "lang" to (language ?: "en"))
        )
    }

    override suspend fun registerPhone(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        method: TFAPhoneMethod?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.registerPhone(
            resolvableContext, phoneNumber,
            mutableMapOf(
                "provider" to TFAProvider.PHONE.value,
                "lang" to (language ?: "en"),
                "mode" to "register",
                "method" to (method?.value ?: TFAPhoneMethod.SMS.value)
            )
        )
    }

    override suspend fun registerTOTP(resolvableContext: ResolvableContext): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.registerTOTP(
            resolvableContext, mutableMapOf(
                "provider" to TFAProvider.TOTP.value,
                "mode" to "register"
            )
        )
    }

    override suspend fun getRegisteredPhoneNumbers(resolvableContext: ResolvableContext): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getRegisteredPhoneNumbers(
            resolvableContext, mutableMapOf(
                "provider" to TFAProvider.PHONE.value,
                "mode" to "verify"
            )
        )
    }

    override suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        phoneId: String,
        method: TFAPhoneMethod?,
        language: String?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.sendPhoneCode(
            resolvableContext,
            mutableMapOf(
                "lang" to (language ?: "en"),
                "phoneID" to phoneId,
                "method" to (method?.value ?: TFAPhoneMethod.SMS.value)
            )
        )
    }

    override suspend fun verifyEmailCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.EMAIL,
            rememberDevice = rememberDevice!!
        )
    }

    override suspend fun verifyPhoneCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.PHONE,
            rememberDevice = rememberDevice!!
        )
    }

    override suspend fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.TOTP,
            rememberDevice = rememberDevice!!
        )
    }
}

//endregion
