package com.sap.cdc.android.sdk.feature.auth.flow.captcha

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

interface IAuthCaptcha {

    suspend fun getSaptchaToken(authCallbacks: AuthCallbacks.() -> Unit)

}

internal class AuthCaptcha(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthCaptcha {

    override suspend fun getSaptchaToken(authCallbacks: AuthCallbacks.() -> Unit) {
        AuthCaptchaFlow(coreClient, sessionService).getSaptchaToken(
            AuthCallbacks().apply(authCallbacks)
        )
    }
}