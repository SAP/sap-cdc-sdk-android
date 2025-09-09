package com.sap.cdc.android.sdk.feature.provider

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.session.SessionService
import java.lang.ref.WeakReference

interface IAuthProvider {

    /**
     * initiate provider authentication flow interface.
     */
    suspend fun signIn(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>? = null,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    /**
     * Remove social connection from current account interface.
     */
    suspend fun removeConnection(
        provider: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    )
}

internal class AuthProvider(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthProvider {

    /**
     * initiate provider authentication flow implementation.
     */
    override suspend fun signIn(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthProviderFlow(coreClient, sessionService, authenticationProvider, WeakReference(hostActivity))
            .signIn(parameters = parameters, callbacks = callbacks)
    }

    /**
     * Remove social connection from current account implementation.
     */
    override suspend fun removeConnection(
        provider: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthProviderFlow(coreClient, sessionService).removeConnection(provider, callbacks)
    }

}
