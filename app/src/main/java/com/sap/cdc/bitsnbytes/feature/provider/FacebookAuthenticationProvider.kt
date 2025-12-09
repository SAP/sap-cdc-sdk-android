package com.sap.cdc.bitsnbytes.feature.provider

import androidx.activity.ComponentActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.ProviderException
import com.sap.cdc.android.sdk.feature.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.feature.provider.ProviderType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class FacebookAuthenticationProvider : IAuthenticationProvider {

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    override fun getProvider(): String = "facebook"

    override suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.HOST_NULL,
                        CDCError.contextError()
                    )
                )
                return@suspendCoroutine
            }

            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    continuation.resumeWithException(
                        ProviderException(
                            ProviderExceptionType.CANCELED,
                            CDCError.operationCanceled()
                        )
                    )
                }

                override fun onError(error: FacebookException) {
                    val providerException = ProviderException(
                        ProviderExceptionType.PROVIDER_FAILURE,
                        CDCError.providerError()
                    )
                    providerException.error?.errorDetails = error.message
                    continuation.resumeWithException(providerException)
                }

                override fun onSuccess(result: LoginResult) {
                    val accessToken = result.accessToken

                    val authenticatorProviderResult = AuthenticatorProviderResult(
                        provider = getProvider(),
                        type = ProviderType.NATIVE,
                        providerSessionData = mapOf(
                            "authToken" to accessToken.token,
                            "tokenExpiration" to (accessToken.expires.time / 1000).toString()
                        ),
                    )
                    continuation.resume(authenticatorProviderResult)
                }

            })
            LoginManager.getInstance()
                .logInWithReadPermissions(
                    hostActivity,
                    callbackManager,
                    listOf("public_profile")
                );
        }

    override suspend fun signOut(hostActivity: ComponentActivity?) {
        LoginManager.getInstance().logOut()
    }

    override fun dispose() {
        // Stub.
        // Facebook SDK unregisters the activityResultRegistry.
    }

}