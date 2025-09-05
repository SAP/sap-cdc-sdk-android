package com.sap.cdc.android.sdk.feature.auth.flow

import android.annotation.SuppressLint
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_OAUTH_AUTHORIZE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_OAUTH_CONNECT
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_OAUTH_TOKEN
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_PASSKEYS_GET_ASSERTION_OPTIONS
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_PASSKEYS_INIT
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_PASSKEYS_REGISTER
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_PASSKEYS_VERIFY_ASSERTION
import com.sap.cdc.android.sdk.feature.auth.AuthResponse
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.OptionsResponseModel
import com.sap.cdc.android.sdk.feature.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import kotlinx.serialization.SerializationException

class PasskeysAuthFlow(
    coreClient: CoreClient,
    sessionService: SessionService,
    private val authenticationProvider: IPasskeysAuthenticationProvider
) : AuthFlow(coreClient, sessionService) {

    companion object {
        private const val LOG_TAG = "PasskeysAuthFlow"
    }

    @SuppressLint("PublicKeyCredential")
    suspend fun createPasskey(): IAuthResponse {
        try {
            // Call init registration to retrieve Json parameters to create the passkey.
            val initResponse =
                AuthenticationApi(coreClient, sessionService).send(EP_PASSKEYS_INIT)
            if (initResponse.isError()) {
                // End flow with error.
                return AuthResponse(initResponse)
            }
            val initRegisterCredentialsEntity =
                initResponse.serializeTo<OptionsResponseModel>()
                    ?: // End flow with error.
                    return AuthResponse(initResponse)

            // Use credentials manager to create the passkey.
            val registrationResponseJson =
                authenticationProvider.createPasskey(initRegisterCredentialsEntity.options!!)

            // Check for correct response
            if (registrationResponseJson != null) {
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
                    "token" to initRegisterCredentialsEntity.token!!
                )

                val registerResponse = AuthenticationApi(coreClient, sessionService).send(
                    EP_PASSKEYS_REGISTER,
                    registerParameters
                )
                if (registerResponse.isError()) return AuthResponse(registerResponse)
                val idToken =
                    registerResponse.stringField("idToken") ?: return AuthResponse(registerResponse)

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: idToken:\n $idToken")

                // Connect credentials to the session.
                val connectResponse = AuthenticationApi(coreClient, sessionService).send(
                    EP_OAUTH_CONNECT,
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )

                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "createPasskey: connectResponse:\n ${connectResponse.jsonResponse}"
                )

                // Notify success. flow has ended.
                return AuthResponse(connectResponse)
            } else {
                //TODO: Special error here. edge case
                return AuthResponse(CDCResponse().providerError())
            }
        } catch (e: SerializationException) {
            //TODO: Special error here. edge case
            return AuthResponse(CDCResponse().fromException(e))
        }
    }

    suspend fun authenticateWithPasskey(): IAuthResponse {
        try {
            // Get assertions options from server.
            val assertionOptionsResponse =
                AuthenticationApi(coreClient, sessionService).send(
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
            val authenticationResponseJson =
                authenticationProvider.getPasskey(optionsResponseModel.options!!)

            if (authenticationResponseJson != null) {
                // Verify assertion.
                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "authenticateWithPasskey: authenticatorAssertion:\n $authenticationResponseJson"
                )
                CDCDebuggable.log(
                    LOG_TAG,
                    "authenticateWithPasskey: token:\n ${optionsResponseModel.token}"
                )

                val verifyAssertionParameters = mutableMapOf(
                    "token" to optionsResponseModel.token!!,
                    "authenticatorAssertion" to authenticationResponseJson
                )
                val verifyAssertionResponse =
                    AuthenticationApi(coreClient, sessionService).send(
                        EP_PASSKEYS_VERIFY_ASSERTION,
                        verifyAssertionParameters
                    )
                if (verifyAssertionResponse.isError()) return AuthResponse(verifyAssertionResponse)

                val idToken =
                    verifyAssertionResponse.stringField("idToken") ?: return AuthResponse(
                        verifyAssertionResponse
                    )

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "authenticateWithPasskey: idToken:\n $idToken")

                // Authorize
                val authorizeResponse = AuthenticationApi(coreClient, sessionService).send(
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
                val tokenResponse = AuthenticationApi(coreClient, sessionService).send(
                    EP_OAUTH_TOKEN,
                    parameters = mutableMapOf("grant_type" to "authorization_code", "code" to code),
                )

                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "authenticateWithPasskey: tokenResponse:\n ${tokenResponse.jsonResponse}"
                )
                if (!tokenResponse.isError()) {
                    secureNewSession(tokenResponse)
                }
                return AuthResponse(tokenResponse)
            } else {
                //TODO: Special error here. edge case
                return AuthResponse(CDCResponse().providerError())
            }
        } catch (e: SerializationException) {
            return AuthResponse(CDCResponse().fromException(e))
        }
    }

    suspend fun clearPasskeyCredential(): IAuthResponse {
        try {
            // Get assertions options from server.
            val assertionOptionsResponse =
                AuthenticationApi(coreClient, sessionService).send(
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

            // Get passkeys from the user's public key credential provider.
            val authenticationResponseJson =
                authenticationProvider.getPasskey(optionsResponseModel.options!!)

            if (authenticationResponseJson != null) {
                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "clearPasskeyCredential: responseJson:\n $authenticationResponseJson"
                )

//                val parsed =
//                    json.decodeFromString<GetCredentialResultEntity>(authenticationResponseJson)

                // decode last key
//                val decodedKey: ByteArray = Base64.decode(
//                    parsed.rawId!!.toByteArray(),
//                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
//                )
//
//                val removeCredentialsParameters = mutableMapOf(
//                    "credentialId" to Base64.encodeToString(decodedKey, Base64.NO_WRAP).trim()
//                )
//                val removeCredentialsResponse =
//                    AuthenticationApi(coreClient, sessionService).genericSend(
//                        EP_PASSKEYS_DELETE,
//                        removeCredentialsParameters
//                    )
//                if (removeCredentialsResponse.isError()) return AuthResponse(
//                    removeCredentialsResponse
//                )
//
//                val idToken =
//                    removeCredentialsResponse.stringField("idToken") ?: return AuthResponse(
//                        removeCredentialsResponse
//                    )
//
//
//                // Debug logs
//                CDCDebuggable.log(LOG_TAG, "authenticateWithPasskey: idToken:\n $idToken")
//
//                val authorizeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
//                    EP_OAUTH_DISCONNECT,
//                    parameters = mutableMapOf(
//                        "regToken" to parsed.rawId!!,
//                        "ignoreApiQueue" to true.toString()
//                    ),
//                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
//                )

                return AuthResponse(assertionOptionsResponse)
            }

            return AuthResponse(CDCResponse().providerError())
        } catch (e: SerializationException) {
            return AuthResponse(CDCResponse().fromException(e))
        }
    }

}
