package com.sap.cdc.android.sdk.feature.captcha

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.session.SessionService

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