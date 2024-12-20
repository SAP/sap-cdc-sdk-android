package com.sap.cdc.android.sdk.auth.flow

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OAUTH_CONNECT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_INIT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_REGISTER
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.model.CreateCredentialResultEntity
import com.sap.cdc.android.sdk.auth.model.InitRegisterCredentialsEntity
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

class PasskeysAuthFlow(
    coreClient: CoreClient,
    sessionService: SessionService,
    private val weakActivity: WeakReference<ComponentActivity>? = null
) : AuthFlow(coreClient, sessionService) {

    companion object {
        private const val LOG_TAG = "PasskeysAuthFlow"
    }

    @SuppressLint("PublicKeyCredential")
    suspend fun createPasskey(): IAuthResponse {
        // Call init registration to retrieve Json parameters to create the passkey.
        val initResponse =
            AuthenticationApi(coreClient, sessionService).genericSend(EP_PASSKEYS_INIT)
        if (initResponse.isError()) {
            // End flow with error.
            return AuthResponse(initResponse)
        }
        val initRegisterCredentialsEntity =
            initResponse.serializeTo<InitRegisterCredentialsEntity>()
                ?: // End flow with error.
                return AuthResponse(initResponse)
        // Use credentials manager to create the passkey.
        val credentialManager =
            weakActivity?.get()?.let { CredentialManager.create(it) } ?: return AuthResponse(
                initResponse
            )
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            // Contains the request in JSON format. Uses the standard WebAuthn
            // web JSON spec.
            requestJson = initRegisterCredentialsEntity.options,
            // Defines whether you prefer to use only immediately available credentials,
            // not hybrid credentials, to fulfill this request. This value is false
            // by default.
            preferImmediatelyAvailableCredentials = false,
        )
        try {
            // Use the createCredential method to create the passkey.
            val result: CreateCredentialResponse = credentialManager.createCredential(
                request = createPublicKeyCredentialRequest,
                context = weakActivity.get()!!,
            )
            // Check for correct response
            if (result is CreatePublicKeyCredentialResponse) {
                // Parse response data for attestation object.
                val registrationResponseJson = result.registrationResponseJson
                CDCDebuggable.log(LOG_TAG, registrationResponseJson)
                val parsedRegistrationResponse =
                    Json.decodeFromString<CreateCredentialResultEntity>(registrationResponseJson)

                // Register the credentials with the server.
                val registerParameters = mutableMapOf(
                    "attestation" to parsedRegistrationResponse.response?.attestationObject!!,
                    "token" to initRegisterCredentialsEntity.token
                )
                val registerResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_PASSKEYS_REGISTER,
                    registerParameters
                )
                if (registerResponse.isError()) return AuthResponse(registerResponse)
                val idToken =
                    registerResponse.stringField("idToken") ?: return AuthResponse(registerResponse)

                // Connect credentials to the session.
                val connectResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_OAUTH_CONNECT,
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )
                return AuthResponse(connectResponse)

                // Notify success. flow has ended.
            } else {
                //TODO: Special error here. edge case
                return AuthResponse(initResponse)
            }
        } catch (e: GetCredentialException) {
            //TODO: Need to translate the credentials api exception to CDCError somehow.
            return AuthResponse(initResponse)
        } catch (e: SerializationException) {
            //TODO: Special error here. edge case
            return AuthResponse(initResponse)
        }
    }

    suspend fun authenticateWithPasskey() {

    }

    suspend fun deletePasskey() {

    }

}