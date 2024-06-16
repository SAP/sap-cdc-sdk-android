package com.sap.cdc.android.sdk.authentication.flow

import com.sap.cdc.android.sdk.authentication.AuthResponse
import com.sap.cdc.android.sdk.authentication.IAuthResponse
import com.sap.cdc.android.sdk.session.SessionService

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

open class AuthFlow(val sessionService: SessionService) {

    var response: AuthResponse = AuthResponse()

    var parameters: MutableMap<String, String> = mutableMapOf()

    fun withParameters(parameters: MutableMap<String, String>) {
        this.parameters.putAll(parameters)
    }

    open suspend fun authenticate(): IAuthResponse {
        //Stub.
        return this.response
    }

    open fun dispose() {
        // Stub.
    }

}