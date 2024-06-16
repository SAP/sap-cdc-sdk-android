package com.sap.cdc.android.sdk.authentication.flow

import com.sap.cdc.android.sdk.authentication.AuthResponse
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.api.CDCResponse
import com.sap.cdc.android.sdk.session.session.Session

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

open class AuthFlow(val sessionService: SessionService) {

    var response: AuthResponse = AuthResponse()
    var parameters: MutableMap<String, String> = mutableMapOf()

    /**
     * Params setter/accumulator.
     */
    fun withParameters(parameters: MutableMap<String, String>) {
        this.parameters.putAll(parameters)
    }

    /**
     * Base authentication flow stub.
     */
    open suspend fun authenticate(): IAuthResponse {
        //Stub.
        return this.response
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
                sessionService.sessionSecure.setSession(session)
            }
        }
    }

}