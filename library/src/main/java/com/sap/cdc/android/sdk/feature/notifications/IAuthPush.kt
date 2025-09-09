package com.sap.cdc.android.sdk.feature.notifications

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthPush {

    suspend fun registerForAuthNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    )

    suspend fun verifyAuthNotification(
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    )
}

internal class AuthPush(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthPush {

    override suspend fun registerForAuthNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPushFlow(coreClient, sessionService).registerAuthDevice(callbacks)
    }

    override suspend fun verifyAuthNotification(
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPushFlow(coreClient, sessionService).verifyAuthPush(vToken, callbacks)
    }

}