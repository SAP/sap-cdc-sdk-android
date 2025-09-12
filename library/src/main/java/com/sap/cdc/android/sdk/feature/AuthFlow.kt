package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_GET_PROVIDERS
import com.sap.cdc.android.sdk.feature.account.LinkEntities
import com.sap.cdc.android.sdk.feature.session.Session
import com.sap.cdc.android.sdk.feature.session.SessionService
import com.sap.cdc.android.sdk.feature.tfa.TFAProvidersEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

open class AuthFlow(val coreClient: CoreClient, val sessionService: SessionService) {

    companion object {
        const val LOG_TAG = "AuthFlow"
    }

    internal open val json: Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    /**
     * Override if needed to dispose various objects.
     */
    open fun dispose() {
        // Stub.
    }

    /**
     * When an authentication response contains a session object (specified by the "sessionInfo" key),
     * Secure the new session. This will actually change the login state of the host app if needed.
     */
    fun secureNewSession(response: CDCResponse) {
        if (response.containsKey("sessionInfo")) {
            val session = response.serializeObject<Session>("sessionInfo")
            if (session != null) {
                sessionService.setSession(session)
            }
        }
    }

    protected suspend fun handleResolvableInterruption(
        response: CDCResponse,
        callbacks: AuthCallbacks
    ) {
        val regToken: String? = response.stringField("regToken")

        when (response.errorCode()) {
            ResolvableContext.Companion.ERR_PENDING_TWO_FACTOR_VERIFICATION -> {
                handleTwoFactorRequired(
                    TwoFactorInitiator.VERIFICATION,
                    response,
                    regToken,
                    callbacks
                )
            }

            ResolvableContext.Companion.ERR_PENDING_TWO_FACTOR_REGISTRATION -> {
                handleTwoFactorRequired(
                    TwoFactorInitiator.REGISTRATION,
                    response,
                    regToken,
                    callbacks
                )
            }

            ResolvableContext.Companion.ERR_NONE -> {
                if (response.containsKey("vToken")) {
                    handleOTPRequired(response, callbacks)
                }
            }

            ResolvableContext.Companion.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                handlePendingRegistration(response, regToken, callbacks)
            }

            ResolvableContext.Companion.ERR_ENTITY_EXIST_CONFLICT -> {
                handleLinkingRequired(response, regToken, callbacks)
            }

            else -> {
                // Unknown interruption - treat as error
                val authError = AuthError(
                    message = "Unknown interruption occurred: ${response.errorCode()}",
                    code = response.errorCode().toString()
                )
                callbacks.onError?.invoke(authError)
            }
        }
    }

    protected suspend fun handleTwoFactorRequired(
        initiator: TwoFactorInitiator,
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
        if (callbacks.onTwoFactorRequired != null) {
            // Get TFA providers

            var tfaProviders: TFAProvidersEntity? = null
            val authResult = getTwoFactorProvidersSync(mutableMapOf("regToken" to (regToken ?: "")))
            if (authResult is AuthResult.Success) {
                tfaProviders = json.decodeFromString<TFAProvidersEntity>(authResult.authSuccess.jsonData)
            }

            val tfaContext = TwoFactorContext(
                initiator = initiator,
                originatingError = createAuthError(response),
                tfaProviders = tfaProviders,
            )

            callbacks.onTwoFactorRequired?.invoke(tfaContext)
        } else {
            // No TFA handler provided - treat as error
            val authError = AuthError(
                message = "Two-factor authentication required but no handler provided",
                code = response.errorCode().toString()
            )
            callbacks.onError?.invoke(authError)
        }
    }

    protected fun handleOTPRequired(
        response: CDCResponse,
        callbacks: AuthCallbacks
    ) {
        if (callbacks.onOTPRequired != null) {
            val otpContext = OTPContext(
                vToken = response.stringField("vToken"),
                originatingError = createAuthError(response),
            )
            callbacks.onOTPRequired?.invoke(otpContext)
        } else {
            // No OTP handler provided - treat as error
            val authError = AuthError(
                message = "OTP verification required but no handler provided",
                code = "OTP_REQUIRED"
            )
            callbacks.onError?.invoke(authError)
        }
    }

    protected fun handlePendingRegistration(
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
        if (callbacks.onPendingRegistration != null) {
            val registrationContext = RegistrationContext(
                regToken = regToken,
                originatingError = createAuthError(response),
            )

            callbacks.onPendingRegistration?.invoke(registrationContext)
        } else {
            // No registration handler provided - treat as error
            val authError = AuthError(
                message = "Additional registration information required but no handler provided",
                code = response.errorCode().toString()
            )
            callbacks.onError?.invoke(authError)
        }
    }

    protected suspend fun handleLinkingRequired(
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
        if (callbacks.onLinkingRequired != null) {
            val provider = response.stringField("provider")
            val authToken = response.stringField("access_token")

            // Get conflicting accounts
            var linkEntities: LinkEntities? = null
            val authResult = getConflictingAccountsSync(mutableMapOf("regToken" to (regToken ?: "")))
            if (authResult is AuthResult.Success) {
                val conflictingJson = authResult.authSuccess.userData["conflictingAccount"] as JsonObject
                linkEntities = json.decodeFromJsonElement(conflictingJson)
            }

            val linkingContext = LinkingContext(
                provider = provider,
                authToken = authToken,
                conflictingAccounts = linkEntities,
                originatingError = createAuthError(response),
            )
            callbacks.onLinkingRequired?.invoke(linkingContext)
        } else {
            // No linking handler provided - treat as error
            val authError = AuthError(
                message = "Account linking required but no handler provided",
                code = response.errorCode().toString()
            )
            callbacks.onError?.invoke(authError)
        }
    }

    protected fun isResolvableContext(response: CDCResponse): Boolean {
        return ResolvableContext.Companion.resolvables.containsKey(response.errorCode()) ||
                response.containsKey("vToken")
    }

    protected fun createAuthSuccess(response: CDCResponse): AuthSuccess {
        val userData = response.jsonObject?.toMap() ?: emptyMap()
        return AuthSuccess(response.jsonResponse ?: "{}", userData)
    }

    protected fun createAuthError(response: CDCResponse): AuthError {
        val error = response.toCDCError()
        val authError = AuthError(
            message = error.errorDescription ?: "Unknown error",
            code = error.errorCode.toString(),
            details = error.errorDetails ?: "Unknown error",
            asJson = response.jsonResponse
        )
        return authError
    }

    /**
     * Request conflicting accounts information.
     * NOTE: Call requires regToken due to interruption source.
     *
     * @see [accounts.getConflictingAccounts](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4134d7df70b21014bbc5a10ce4041860.html?q=conflictingAccounts)
     */
    protected suspend fun getConflictingAccountsSync(
        parameters: MutableMap<String, String>? = mutableMapOf()
    ): AuthResult {
        CDCDebuggable.log(LOG_TAG, "getConflictingAccounts: with parameters:$parameters")

        val response = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS,
            parameters ?: mutableMapOf()
        )

        return if (response.isError()) {
            AuthResult.Error(createAuthError(response))
        } else {
            secureNewSession(response)
            AuthResult.Success(createAuthSuccess(response))
        }
    }


    /**
     * Request account two factor authentication providers:
     * Active - Providers that are currently active and the user can use to authenticate.
     * Inactive - Providers that are currently inactive and the user can activate to use for authentication.
     */
    protected suspend fun getTwoFactorProvidersSync(parameters: MutableMap<String, String>? = mutableMapOf())
            : AuthResult {
        CDCDebuggable.log(LOG_TAG, "getTFAProviders: with parameters:$parameters")
        val response = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_GET_PROVIDERS,
            parameters!!
        )

        return if (response.isError()) {
            AuthResult.Error(createAuthError(response))
        } else {
            secureNewSession(response)
            AuthResult.Success(createAuthSuccess(response))
        }
    }

    /**
     * Connect accounts synchronously - returns AuthResult for use in override transformers.
     * This method is shared between AuthProviderFlow and AuthLoginFlow.
     * Only returns Success or Error - no resolvable interruptions.
     */
    protected suspend fun connectAccountSync(provider: String, authToken: String): AuthResult {
        val providerSessions = JsonObject(
            mapOf(
                provider to JsonObject(
                    mapOf(
                        "authToken" to JsonPrimitive(authToken)
                    )
                )
            )
        )
        val parameters =
            mutableMapOf("providerSessions" to providerSessions.toString(), "loginMode" to "connect")

        val response = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
            parameters
        )

        return if (response.isError()) {
            AuthResult.Error(createAuthError(response))
        } else {
            secureNewSession(response)
            AuthResult.Success(createAuthSuccess(response))
        }
    }

}
