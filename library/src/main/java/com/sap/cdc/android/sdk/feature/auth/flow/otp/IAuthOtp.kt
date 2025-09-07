package com.sap.cdc.android.sdk.feature.auth.flow.otp

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

//region OTP INTERFACE
interface IAuthOtp {

    fun resolve(): IAuthOtpResolvers

    suspend fun sendCode(
        parameters: MutableMap<String, String>,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}

internal class AuthOtp(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthOtp {

    override fun resolve(): IAuthOtpResolvers = AuthOtpResolvers(coreClient, sessionService)

    override suspend fun sendCode(
        parameters: MutableMap<String, String>,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthOtpFlow(coreClient, sessionService).otpSendCode(parameters, callbacks)
    }

}

// endregion

//region OTP RESOLVERS INTERFACE

interface IAuthOtpResolvers {

    suspend fun login(
        code: String,
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun otpUpdate(
        code: String,
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}

internal class AuthOtpResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthOtpResolvers {

    override suspend fun login(
        code: String,
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthOtpFlow(coreClient, sessionService).otpLogin(
            mutableMapOf(
                "code" to code,
                "vToken" to vToken
            ), callbacks
        )
    }

    override suspend fun otpUpdate(
        code: String,
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthOtpFlow(coreClient, sessionService).otpUpdate(
            mutableMapOf(
                "code" to code,
                "vToken" to vToken
            ), callbacks
        )
    }
}