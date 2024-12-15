package com.sap.cdc.android.sdk.example.social

import androidx.activity.ComponentActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.sap.cdc.android.sdk.auth.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.ProviderType
import com.sap.cdc.android.sdk.auth.provider.util.ProviderException
import com.sap.cdc.android.sdk.auth.provider.util.ProviderExceptionType
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

                    // Generate the relevant providerSession object required for CDC servers to validate the token.
                    val data = JsonObject(
                        mapOf(
                            "facebook" to JsonObject(
                                mapOf(
                                    "authToken" to JsonPrimitive(accessToken.token),
                                    "tokenExpiration" to JsonPrimitive(accessToken.expires.time / 1000)
                                )
                            )
                        )
                    )

                    val providerSession = data.toString()

                    val authenticatorProviderResult = AuthenticatorProviderResult(
                        provider = getProvider(),
                        type = ProviderType.NATIVE,
                        providerSessions = providerSession,
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