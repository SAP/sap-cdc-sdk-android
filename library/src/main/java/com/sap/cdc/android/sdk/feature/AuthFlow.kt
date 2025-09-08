package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.android.sdk.feature.auth.ResolvableLinking
import com.sap.cdc.android.sdk.feature.auth.ResolvableOtp
import com.sap.cdc.android.sdk.feature.auth.ResolvableRegistration
import com.sap.cdc.android.sdk.feature.auth.ResolvableTFA
import com.sap.cdc.android.sdk.feature.auth.sequence.AuthResolvers
import com.sap.cdc.android.sdk.feature.auth.sequence.AuthTFA
import com.sap.cdc.android.sdk.feature.session.Session
import com.sap.cdc.android.sdk.feature.session.SessionService
import kotlinx.serialization.json.Json

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

    /**
     * Initialize resolvable state.
     * According to provided error, the method will determine if this error is resolvable. If so, it will
     * populate the "AuthResolvable" class with the data required to complete the flow.
     */
    suspend fun initResolvableState(cdcResponse: CDCResponse): ResolvableContext? {
        // Init auth resolvable entity with RegToken field.
        if (ResolvableContext.Companion.resolvables.containsKey(cdcResponse.errorCode()) || cdcResponse.containsKey(
                "vToken"
            )
        ) {
            val resolvableContext =
                ResolvableContext(cdcResponse.stringField("regToken"))
            val resolve = AuthResolvers(coreClient, sessionService)
            when (cdcResponse.errorCode()) {

                //OTP
                ResolvableContext.Companion.ERR_NONE -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_NONE")
                    // Resolvable state can occur on successful call in OTP flows.
                    // vToken is required for OTP verification.
                    resolvableContext.otp =
                        ResolvableOtp(cdcResponse.stringField("vToken"))
                }


                ResolvableContext.Companion.ERR_CAPTCHA_REQUIRED -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_SAPTCHA_REQUIRED")
                }

                // REGISTRATION
                ResolvableContext.Companion.ERR_ACCOUNT_PENDING_REGISTRATION -> {
//                    CDCDebuggable.log(LOG_TAG, "ERR_ACCOUNT_PENDING_REGISTRATION")
//                    // Parse missing fields required for registration.
//                    val missingFields =
//                        cdcResponse.errorDetails()
//                            ?.parseRequiredMissingFieldsForRegistration()
                    resolvableContext.registration = ResolvableRegistration()
                }

                // LINKING
                ResolvableContext.Companion.ERR_ENTITY_EXIST_CONFLICT -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_ENTITY_EXIST_CONFLICT")
                    // Add fields required for v2 linking flow.
                    val provider = cdcResponse.stringField("provider")
                    val authToken = cdcResponse.stringField("access_token")
                    resolvableContext.linking = ResolvableLinking(provider, authToken)

                    // Request conflicting accounts.
                    val conflictingAccounts =
                        resolve.getConflictingAccounts(mutableMapOf("regToken" to resolvableContext.regToken!!))
                    // Add conflicting accounts data to resolvable context.
                    resolvableContext.linking!!.conflictingAccounts =
                        resolve.parseConflictingAccounts(conflictingAccounts)
                }

                // TFA
                ResolvableContext.Companion.ERR_PENDING_TWO_FACTOR_REGISTRATION,
                ResolvableContext.Companion.ERR_PENDING_TWO_FACTOR_VERIFICATION -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_ERROR_PENDING_TWO_FACTOR_REGISTRATION")
                    // Get providers
                    val tfaResolve = AuthTFA(coreClient, sessionService)
                    val tfaProviders =
                        tfaResolve.getProviders(regToken = resolvableContext.regToken!!)
                    resolvableContext.tfa = ResolvableTFA(
                        tfaProviders = tfaResolve.parseTFAProviders(tfaProviders)
                    )
                }
            }
            return resolvableContext
        }
        return null
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
            val tfaResolve = AuthTFA(coreClient, sessionService)
            val tfaProviders = tfaResolve.getProviders(regToken = regToken ?: "")
            val parsedProviders = tfaResolve.parseTFAProviders(tfaProviders)

            val tfaContext = TwoFactorContext(
                initiator = initiator,
                originatingError = createAuthError(response),
                tfaProviders = parsedProviders,
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
            val resolve = AuthResolvers(coreClient, sessionService)
            val conflictingAccounts = resolve.getConflictingAccounts(
                mutableMapOf("regToken" to (regToken ?: ""))
            )
            val parsedConflictingAccounts = resolve.parseConflictingAccounts(conflictingAccounts)

            val linkingContext = LinkingContext(
                provider = provider,
                authToken = authToken,
                conflictingAccounts = parsedConflictingAccounts,
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

}