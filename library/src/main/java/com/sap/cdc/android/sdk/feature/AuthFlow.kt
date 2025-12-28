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
 * Base class for authentication flow implementations.
 *
 * Provides core functionality for handling authentication responses, session management,
 * and resolvable interruptions (2FA, linking, pending registration, OTP).
 *
 * ## Key Responsibilities
 * - Session management when authentication succeeds
 * - Handling authentication interruptions (2FA, account linking, OTP)
 * - Creating standardized AuthResult objects from API responses
 * - Coordinating context updates for multi-step flows
 *
 * ## Usage
 * This is a base class used internally by authentication flows. Developers typically
 * interact with higher-level interfaces like `IAuthApis` rather than this class directly.
 *
 * ```kotlin
 * // Example of a flow that extends AuthFlow
 * class AuthLoginFlow : AuthFlow {
 *     suspend fun execute(credentials: Credentials, callbacks: AuthCallbacks) {
 *         val response = sendLoginRequest(credentials)
 *
 *         if (isResolvableContext(response)) {
 *             handleResolvableInterruption(response, callbacks)
 *         } else if (response.isError()) {
 *             callbacks.onError?.invoke(createAuthError(response))
 *         } else {
 *             secureNewSession(response)
 *             callbacks.onSuccess?.invoke(createAuthSuccess(response))
 *         }
 *     }
 * }
 * ```
 *
 * @param coreClient Core API client for making CDC requests
 * @param sessionService Service for managing user sessions
 * @see IAuthApis
 * @see AuthCallbacks
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
     * Extracts and secures a new session from the authentication response.
     *
     * When a CDC API response contains session information (under the "sessionInfo" key),
     * this method extracts the session and stores it securely via the SessionService.
     * This changes the login state of the application.
     *
     * @param response The CDC response that may contain session information
     */
    fun secureNewSession(response: CDCResponse) {
        if (response.containsKey("sessionInfo")) {
            val session = response.serializeObject<Session>("sessionInfo")
            if (session != null) {
                sessionService.setSession(session)
            }
        }
    }


    /**
     * Handles resolvable authentication interruptions.
     *
     * Routes the response to the appropriate handler based on the error code:
     * - Two-factor authentication required (verification or registration)
     * - OTP (One-Time Password) required
     * - Pending registration completion
     * - Account linking required
     *
     * @param response The CDC response containing the interruption
     * @param callbacks Authentication callbacks to invoke for interruption handling
     */
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
                    code = response.errorCode(),
                    message = "Unknown interruption occurred: ${response.errorCode()}"
                )
                callbacks.onError?.invoke(authError)
            }
        }
    }

    /**
     * Handles two-factor authentication requirements.
     *
     * Fetches available TFA providers and creates a TwoFactorContext to provide
     * to the application. If a specific TFA handler is registered, it will be invoked;
     * otherwise, the error is passed to the generic error handler.
     *
     * @param initiator Whether this is for verification or initial registration
     * @param response The CDC response containing the TFA requirement
     * @param regToken Registration token for the current session
     * @param callbacks Authentication callbacks to invoke
     */
    protected suspend fun handleTwoFactorRequired(
        initiator: TwoFactorInitiator,
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
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
            regToken = regToken
        )

        // Try invoking the specific handler first
        if (callbacks.onTwoFactorRequired != null) {
            callbacks.onTwoFactorRequired?.invoke(tfaContext)
        } else {
            // If no specific handler, pass to onError so ScreenSets can handle it
            tfaContext.originatingError?.let { callbacks.onError?.invoke(it) }
        }
    }

    /**
     * Handles OTP (One-Time Password) requirements.
     *
     * Creates an OTPContext with the verification token and invokes the appropriate
     * callback handler. If no specific OTP handler is registered, the error is passed
     * to the generic error handler.
     *
     * @param response The CDC response containing the OTP requirement
     * @param callbacks Authentication callbacks to invoke
     */
    protected fun handleOTPRequired(
        response: CDCResponse,
        callbacks: AuthCallbacks
    ) {
        val otpContext = OTPContext(
            vToken = response.stringField("vToken"),
            originatingError = createAuthError(response),
        )

        // Try invoking the specific handler first
        if (callbacks.onOTPRequired != null) {
            callbacks.onOTPRequired?.invoke(otpContext)
        } else {
            // If no specific handler, pass to onError so ScreenSets can handle it
            otpContext.originatingError?.let { callbacks.onError?.invoke(it) }
        }
    }

    /**
     * Handles pending registration completion requirements.
     *
     * Creates a RegistrationContext with the registration token and invokes the
     * appropriate callback handler. If no specific handler is registered, the error
     * is passed to the generic error handler.
     *
     * @param response The CDC response containing the pending registration requirement
     * @param regToken Registration token for completing the registration
     * @param callbacks Authentication callbacks to invoke
     */
    protected fun handlePendingRegistration(
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
        val registrationContext = RegistrationContext(
            regToken = regToken,
            originatingError = createAuthError(response),
        )

        // Try invoking the specific handler first
        if (callbacks.onPendingRegistration != null) {
            callbacks.onPendingRegistration?.invoke(registrationContext)
        } else {
            // If no specific handler, pass to onError so ScreenSets can handle it
            registrationContext.originatingError?.let { callbacks.onError?.invoke(it) }
        }
    }

    /**
     * Handles account linking requirements.
     *
     * Fetches conflicting account information and creates a LinkingContext with
     * the social provider details. If a specific linking handler is registered, it will
     * be invoked; otherwise, the error is passed to the generic error handler.
     *
     * @param response The CDC response containing the linking requirement
     * @param regToken Registration token for the linking process
     * @param callbacks Authentication callbacks to invoke
     */
    protected suspend fun handleLinkingRequired(
        response: CDCResponse,
        regToken: String?,
        callbacks: AuthCallbacks
    ) {
        val provider = response.stringField("provider")
        val authToken = response.stringField("access_token")

        // Get conflicting accounts
        var linkEntities: LinkEntities? = null
        val authResult = getConflictingAccountsSync(mutableMapOf("regToken" to (regToken ?: "")))
        if (authResult is AuthResult.Success) {
            val conflictingJson = authResult.authSuccess.data["conflictingAccount"] as JsonObject
            linkEntities = json.decodeFromJsonElement(conflictingJson)
        }

        val linkingContext = LinkingContext(
            provider = provider,
            authToken = authToken,
            conflictingAccounts = linkEntities,
            originatingError = createAuthError(response),
        )

        // Try invoking the specific handler first
        if (callbacks.onLinkingRequired != null) {
            callbacks.onLinkingRequired?.invoke(linkingContext)
        } else {
            // If no specific handler, pass to onError so ScreenSets can handle it
            linkingContext.originatingError?.let { callbacks.onError?.invoke(it) }
        }
    }

    /**
     * Determines if a CDC response contains a resolvable authentication interruption.
     *
     * Checks if the response error code represents a resolvable context (2FA, linking,
     * pending registration) or contains an OTP verification token.
     *
     * @param response The CDC response to evaluate
     * @return true if the response contains a resolvable interruption, false otherwise
     */
    protected fun isResolvableContext(response: CDCResponse): Boolean {
        return ResolvableContext.Companion.resolvables.containsKey(response.errorCode()) ||
                response.containsKey("vToken")
    }

    /**
     * Creates an AuthSuccess object from a CDC response.
     *
     * Extracts user data from the response and packages it into an AuthSuccess
     * result for delivery to application callbacks.
     *
     * @param response The successful CDC response
     * @return AuthSuccess containing the response data
     */
    protected fun createAuthSuccess(response: CDCResponse): AuthSuccess {
        val userData = response.jsonObject?.toMap() ?: emptyMap()
        return AuthSuccess(response.jsonResponse ?: "{}", userData)
    }

    /**
     * Creates an AuthError object from a CDC response.
     *
     * Converts a CDC error response into an AuthError with standardized error
     * information including code, message, and details.
     *
     * @param response The error CDC response
     * @return AuthError containing the error information
     */
    protected fun createAuthError(response: CDCResponse): AuthError {
        return AuthError(
            code = response.errorCode(),
            message = response.errorMessage() ?: "Unknown error",
            details = response.errorDetails() ?: "Unknown error",
            asJson = response.jsonResponse
        )
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
