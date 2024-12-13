package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.AuthResolvers
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.extensions.parseRequiredMissingFieldsForRegistration

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

open class AuthFlow(val coreClient: CoreClient, val sessionService: SessionService) {

    var parameters: MutableMap<String, String> = mutableMapOf()

    /**
     * Params setter/accumulator.
     */
    fun withParameters(parameters: MutableMap<String, String>) {
        this.parameters.putAll(parameters)
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
    suspend fun initResolvableState(authResponse: AuthResponse) {
        // Init auth resolvable entity with RegToken field.
        if (authResponse.isResolvable()) {
            val resolvableContext = ResolvableContext(authResponse.cdcResponse().stringField("regToken"))
            val resolve = AuthResolvers(coreClient, sessionService)
            when (authResponse.cdcResponse().errorCode()) {
                ResolvableContext.ERR_NONE -> {
                    // Resolvable state can occur on successful call in OTP flows.
                    resolvableContext.vToken = authResponse.cdcResponse().stringField("vToken")
                }

                ResolvableContext.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                    val missingFields =
                        authResponse.cdcResponse().errorDetails()
                            ?.parseRequiredMissingFieldsForRegistration()
                    resolvableContext.missingRequiredFields = missingFields
                }

                ResolvableContext.ERR_ENTITY_EXIST_CONFLICT -> {
                    resolvableContext.provider = authResponse.cdcResponse().stringField("provider")
                    resolvableContext.authToken =
                        authResponse.cdcResponse().stringField("access_token")
                    // Request conflicting accounts.
                    val conflictingAccounts =
                        resolve.getConflictingAccounts(mutableMapOf("regToken" to resolvableContext.regToken!!))
                    resolvableContext.conflictingAccounts =
                        resolve.parseConflictingAccounts(conflictingAccounts)
                }
            }
            authResponse.resolvableContext = resolvableContext
        }
    }

}
