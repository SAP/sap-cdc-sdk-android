package com.sap.cdc.android.sdk.feature.account

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.login.AuthLoginFlow
import com.sap.cdc.android.sdk.feature.provider.AuthProviderFlow
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.session.SessionService
import java.lang.ref.WeakReference

interface IAuthLink {

    suspend fun toSite(
        parameters: MutableMap<String, String>,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun toSocial(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}


class AuthLink(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthLink {

    override suspend fun toSite(
        parameters: MutableMap<String, String>,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        AuthLoginFlow(coreClient, sessionService).linkToSite(
            parameters = parameters,
            linkingContext = linkingContext,
            authCallbacks = authCallbacks
        )
    }

    override suspend fun toSocial(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        AuthProviderFlow(coreClient, sessionService, authenticationProvider, WeakReference(hostActivity))
            .linkToProvider(linkingContext = linkingContext, authCallbacks = authCallbacks)
    }

}
