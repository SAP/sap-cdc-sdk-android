package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.sequence.AuthResolvers
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.ResolvableLinking
import com.sap.cdc.android.sdk.auth.ResolvableOtp
import com.sap.cdc.android.sdk.auth.ResolvableRegistration
import com.sap.cdc.android.sdk.auth.ResolvableTFA
import com.sap.cdc.android.sdk.auth.sequence.AuthTFA
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.extensions.parseRequiredMissingFieldsForRegistration
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
        if (ResolvableContext.resolvables.containsKey(cdcResponse.errorCode()) || cdcResponse.containsKey(
                "vToken"
            )
        ) {
            val resolvableContext =
                ResolvableContext(cdcResponse.stringField("regToken"))
            val resolve = AuthResolvers(coreClient, sessionService)
            when (cdcResponse.errorCode()) {

                //OTP
                ResolvableContext.ERR_NONE -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_NONE")
                    // Resolvable state can occur on successful call in OTP flows.
                    // vToken is required for OTP verification.
                    resolvableContext.otp =
                        ResolvableOtp(cdcResponse.stringField("vToken"))
                }


                ResolvableContext.ERR_CAPTCHA_REQUIRED -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_SAPTCHA_REQUIRED")
                }

                // REGISTRATION
                ResolvableContext.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                    CDCDebuggable.log(LOG_TAG, "ERR_ACCOUNT_PENDING_REGISTRATION")
                    // Parse missing fields required for registration.
                    val missingFields =
                        cdcResponse.errorDetails()
                            ?.parseRequiredMissingFieldsForRegistration()
                    resolvableContext.registration = ResolvableRegistration(missingFields)
                }

                // LINKING
                ResolvableContext.ERR_ENTITY_EXIST_CONFLICT -> {
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
                ResolvableContext.ERR_PENDING_TWO_FACTOR_REGISTRATION,
                ResolvableContext.ERR_PENDING_TWO_FACTOR_VERIFICATION -> {
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

}
