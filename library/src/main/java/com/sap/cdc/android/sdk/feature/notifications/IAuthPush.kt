package com.sap.cdc.android.sdk.feature.notifications

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthPush {

    suspend fun optInForNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    )

    suspend fun verifyNotification(
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    )
}

internal class AuthPush(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthPush {

    override suspend fun optInForNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPushFlow(coreClient, sessionService).registerAuthDevice(callbacks)
    }

    override suspend fun verifyNotification(
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthPushFlow(coreClient, sessionService).verifyAuthPush(vToken, callbacks)
    }

}