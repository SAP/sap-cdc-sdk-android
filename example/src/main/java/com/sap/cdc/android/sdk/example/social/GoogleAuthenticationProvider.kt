package com.sap.cdc.android.sdk.example.social

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sap.cdc.android.sdk.authentication.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.ProviderException
import com.sap.cdc.android.sdk.authentication.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.authentication.provider.ProviderType
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.session.api.model.CDCError
import io.ktor.util.generateNonce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class GoogleAuthenticationProvider : IAuthenticationProvider {

    override fun getProvider(): String = "google"

    override suspend fun providerSignIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
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

            // Create instance of CredentialsManager.
            val credentialManager = CredentialManager.create(hostActivity)

            // Reference required Google server client ID (Resource id is not strict).
            val serverClientId = hostActivity.getString(R.string.google_server_client_id)

            CoroutineScope(Dispatchers.IO).launch {
                var result = credentialsManagerSignIn(
                    credentialManager,
                    serverClientId,
                    true,
                    hostActivity
                )

                // Try again using showFilterByAuthorizedAccounts as FALSE.
                if (result.failed()) {
                    result = credentialsManagerSignIn(
                        credentialManager,
                        serverClientId,
                        false,
                        hostActivity
                    )
                }

                if (result.failed()) {
                    val providerException = ProviderException(
                        ProviderExceptionType.PROVIDER_FAILURE,
                        CDCError.providerError()
                    )
                    providerException.error?.addDynamic(
                        "providerMessage",
                        result.exception?.message.toString()
                    )
                    continuation.resumeWithException(providerException)
                }

                // Handle result.
                val credential = result.response!!.credential
                // Extract ID Token.
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                val data = JsonObject(
                    mapOf(
                        "google" to JsonObject(
                            mapOf(
                                "idToken" to JsonPrimitive(googleIdTokenCredential.idToken),
                            )
                        )
                    )
                )
                val providerSession = data.toString()

                val authenticatorProviderResult = AuthenticatorProviderResult(
                    provider = getProvider(),
                    type = ProviderType.NATIVE,
                    providerSessions = providerSession
                )
                continuation.resume(authenticatorProviderResult)
            }
        }


    /**
     * Initiate getCredential request.
     */
    private suspend fun credentialsManagerSignIn(
        credentialManager: CredentialManager,
        serverClientId: String,
        showFilterByAuthorizedAccounts: Boolean,
        context: Context
    ): CredentialSignInResult {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(showFilterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .setNonce(generateNonce())
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context,
            )
            return CredentialSignInResult(result, null)
        } catch (exception: GetCredentialException) {
            // This may cause a race condition.....
            Log.d("GoogleAuthenticationProvider", exception.message.toString())
            return CredentialSignInResult(null, exception)
        }
    }

    override suspend fun providerSignOut(hostActivity: ComponentActivity?) {
        if (hostActivity == null) {
            Log.d("GoogleAuthenticationProvider", "Context missing. Cannot sign out")
        }
        // Ambiguous... need to be tested.
        val request = ClearCredentialStateRequest()
        CredentialManager.create(hostActivity!!).clearCredentialState(request)
    }
}

/**
 * Helper class for Google sign in flow. For some reason Google needs to do the same
 * call twice with setFilterByAuthorizedAccounts (TRUE/FALSE) for a complete flow...
 */
data class CredentialSignInResult(
    val response: GetCredentialResponse?,
    val exception: GetCredentialException?
) {

    fun failed(): Boolean = exception != null
}