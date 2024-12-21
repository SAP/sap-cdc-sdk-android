package com.sap.cdc.android.sdk.auth.flow

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OAUTH_AUTHORIZE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OAUTH_CONNECT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_OAUTH_TOKEN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_GET_ASSERTION_OPTIONS
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_INIT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_REGISTER
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_PASSKEYS_VERIFY_ASSERTION
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.model.CreateCredentialResultEntity
import com.sap.cdc.android.sdk.auth.model.OptionsResponseModel
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import kotlinx.serialization.SerializationException
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
        try {
            // Call init registration to retrieve Json parameters to create the passkey.
            val initResponse =
                AuthenticationApi(coreClient, sessionService).genericSend(EP_PASSKEYS_INIT)
            if (initResponse.isError()) {
                // End flow with error.
                return AuthResponse(initResponse)
            }
            val initRegisterCredentialsEntity =
                initResponse.serializeTo<OptionsResponseModel>()
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

            // Use the createCredential method to create the passkey.
            val result: CreateCredentialResponse = credentialManager.createCredential(
                request = createPublicKeyCredentialRequest,
                context = weakActivity.get()!!,
            )
            // Check for correct response
            if (result is CreatePublicKeyCredentialResponse) {
                // Parse response data for attestation object.
                val registrationResponseJson = result.registrationResponseJson
                val parsedRegistrationResponse =
                    json.decodeFromString<CreateCredentialResultEntity>(registrationResponseJson)

                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "createPasskey: attestation:\n $registrationResponseJson"
                )
                CDCDebuggable.log(
                    LOG_TAG,
                    "createPasskey: token:\n ${initRegisterCredentialsEntity.token}"
                )

                // Register the credentials with the server.
                val registerParameters = mutableMapOf(
                    "attestation" to registrationResponseJson,
                    "token" to initRegisterCredentialsEntity.token
                )

                val registerResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_PASSKEYS_REGISTER,
                    registerParameters
                )
                if (registerResponse.isError()) return AuthResponse(registerResponse)
                val idToken =
                    registerResponse.stringField("idToken") ?: return AuthResponse(registerResponse)

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: idToken:\n $idToken")

                // Connect credentials to the session.
                val connectResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_OAUTH_CONNECT,
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )
                // Notify success. flow has ended.
                return AuthResponse(connectResponse)
            } else {
                //TODO: Special error here. edge case
                return AuthResponse(CDCResponse().providerError())
            }
        } catch (e: GetCredentialException) {
            //TODO: Need to translate the credentials api exception to CDCError somehow.
            return AuthResponse(CDCResponse().fromException(e))
        } catch (e: SerializationException) {
            //TODO: Special error here. edge case
            return AuthResponse(CDCResponse().fromException(e))
        }
    }

    suspend fun authenticateWithPasskey(): IAuthResponse {
        try {
            // Get assertions options from server.
            val assertionOptionsResponse =
                AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_PASSKEYS_GET_ASSERTION_OPTIONS
                )
            if (assertionOptionsResponse.isError()) {
                // End flow with error.
                return AuthResponse(assertionOptionsResponse)
            }
            val optionsResponseModel =
                assertionOptionsResponse.serializeTo<OptionsResponseModel>()
                    ?: // End flow with error.
                    return AuthResponse(assertionOptionsResponse)

            // Use credentials manager get the passkey.
            val credentialManager =
                weakActivity?.get()?.let { CredentialManager.create(it) } ?: return AuthResponse(
                    assertionOptionsResponse
                )
            // Get passkeys from the user's public key credential provider.
            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                requestJson = optionsResponseModel.options,
            )

            val getCredRequest = GetCredentialRequest(
                listOf(getPublicKeyCredentialOption)
            )

            val result: GetCredentialResponse = credentialManager.getCredential(
                // Use an activity-based context to avoid undefined system UI
                // launching behavior.
                context = weakActivity.get()!!,
                request = getCredRequest
            )

            val credential = result.credential
            if (credential is PublicKeyCredential) {
                val responseJson = credential.authenticationResponseJson

                // Verify assertion.
                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: authenticatorAssertion:\n $responseJson")
                CDCDebuggable.log(LOG_TAG, "createPasskey: token:\n ${optionsResponseModel.token}")

                val verifyAssertionParameters = mutableMapOf(
                    "token" to optionsResponseModel.token,
                    "authenticatorAssertion" to responseJson
                )
                val verifyAssertionResponse =
                    AuthenticationApi(coreClient, sessionService).genericSend(
                        EP_PASSKEYS_VERIFY_ASSERTION,
                        verifyAssertionParameters
                    )
                if (verifyAssertionResponse.isError()) return AuthResponse(verifyAssertionResponse)

                val idToken =
                    verifyAssertionResponse.stringField("idToken") ?: return AuthResponse(
                        verifyAssertionResponse
                    )

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: idToken:\n $idToken")

                // Authorize
                val authorizeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_OAUTH_AUTHORIZE,
                    parameters = mutableMapOf("response_type" to "code"),
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )
                if (authorizeResponse.isError()) return AuthResponse(authorizeResponse)

                val code = authorizeResponse.stringField("code") ?: return AuthResponse(
                    authorizeResponse
                )

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: code:\n $code")

                // Toke
                val tokenResponse = AuthenticationApi(coreClient, sessionService).genericSend(
                    EP_OAUTH_TOKEN,
                    parameters = mutableMapOf("grant_type" to "authorization_code", "code" to code),
                )

                CDCDebuggable.log(
                    LOG_TAG,
                    "createPasskey: tokenResponse:\n ${tokenResponse.jsonResponse}"
                )
                if (!tokenResponse.isError()) {
                    secureNewSession(tokenResponse)
                }
                return AuthResponse(tokenResponse)
            } else {
                //TODO: Special error here. edge case
                return AuthResponse(CDCResponse().providerError())
            }
        } catch (e: GetCredentialException) {
            return AuthResponse(CDCResponse().fromException(e))
        } catch (e: SerializationException) {
            return AuthResponse(CDCResponse().fromException(e))
        }
    }

    suspend fun deletePasskey() {

    }

}