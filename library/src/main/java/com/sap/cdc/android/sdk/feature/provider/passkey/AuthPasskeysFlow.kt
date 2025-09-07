package com.sap.cdc.android.sdk.feature.provider.passkey

import android.annotation.SuppressLint
import android.util.Base64
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OAUTH_AUTHORIZE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OAUTH_CONNECT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OAUTH_DISCONNECT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_OAUTH_TOKEN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_DELETE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_GET_ASSERTION_OPTIONS
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_GET_CREDENTIALS
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_INIT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_REGISTER
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_PASSKEYS_VERIFY_ASSERTION
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService
import kotlinx.serialization.SerializationException

class AuthPasskeysFlow(
    coreClient: CoreClient,
    sessionService: SessionService,
    private val authenticationProvider: IPasskeysAuthenticationProvider? = null
) : AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthPasskeysFlow"
    }

    @SuppressLint("PublicKeyCredential")
    suspend fun create(authCallbacks: AuthCallbacks) {
        try {
            // Call init registration to retrieve Json parameters to create the passkey.
            val init =
                AuthenticationApi(coreClient, sessionService).send(EP_PASSKEYS_INIT)
            // error case (init)
            if (init.isError()) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(init))
            }

            val initRegisterCredentialsEntity =
                init.serializeTo<OptionsResponseModel>()

            val options = initRegisterCredentialsEntity?.options
            if (options == null) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                return
            }
            // Use credentials manager to create the passkey.
            val registrationResponseJson =
                authenticationProvider?.createPasskey(options)

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

                val register = AuthenticationApi(coreClient, sessionService).send(
                    EP_PASSKEYS_REGISTER,
                    registerParameters
                )

                // error case (register)
                if (register.isError()) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(register))
                    return
                }

                val idToken =
                    register.stringField("idToken")
                if (idToken == null) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                    return
                }

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: idToken:\n $idToken")

                // Connect credentials to the session.
                val connect = AuthenticationApi(coreClient, sessionService).send(
                    EP_OAUTH_CONNECT,
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )

                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "createPasskey: connectResponse:\n ${connect.jsonResponse}"
                )

                // error case (connect)
                if (connect.isError()) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(connect))
                    return
                }

                // Success case
                val authSuccess = createAuthSuccess(connect)
                authCallbacks.onSuccess?.invoke(authSuccess)
            } else {
                //TODO: Special error here. edge case (extend provider error)
                authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
            }
        } catch (e: SerializationException) {
            //TODO: Special error here. edge case  (extend provider error)
            authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
        }
    }

    suspend fun login(authCallbacks: AuthCallbacks) {
        try {
            // Get assertions options from server.
            val assertionOptions =
                AuthenticationApi(coreClient, sessionService).send(
                    EP_PASSKEYS_GET_ASSERTION_OPTIONS
                )

            // error case (assertionOptions)
            if (assertionOptions.isError()) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(assertionOptions))
            }

            val optionsResponseModel =
                assertionOptions.serializeTo<OptionsResponseModel>()

            if (optionsResponseModel?.options == null) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                return
            }

            // Use credentials manager get the passkey.
            val authenticationResponseJson =
                authenticationProvider?.getPasskey(optionsResponseModel.options!!)

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
                val verifyAssertion =
                    AuthenticationApi(coreClient, sessionService).send(
                        EP_PASSKEYS_VERIFY_ASSERTION,
                        verifyAssertionParameters
                    )

                // error case (verifyAssertion)
                if (verifyAssertion.isError()) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(verifyAssertion))
                    return
                }

                val idToken =
                    verifyAssertion.stringField("idToken")
                if (idToken == null) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                    return
                }

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "authenticateWithPasskey: idToken:\n $idToken")

                // Authorize
                val authorize = AuthenticationApi(coreClient, sessionService).send(
                    EP_OAUTH_AUTHORIZE,
                    parameters = mutableMapOf("response_type" to "code"),
                    headers = mutableMapOf("Authorization" to "Bearer $idToken")
                )

                // error case (authorize)
                if (authorize.isError()) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(authorize))
                    return
                }

                val code = authorize.stringField("code")
                if (code == null) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                    return
                }

                // Debug logs
                CDCDebuggable.log(LOG_TAG, "createPasskey: code:\n $code")

                // Toke
                val token = AuthenticationApi(coreClient, sessionService).send(
                    EP_OAUTH_TOKEN,
                    parameters = mutableMapOf("grant_type" to "authorization_code", "code" to code),
                )

                // error case (token)
                if (token.isError()) {
                    // End flow with error.
                    authCallbacks.onError?.invoke(createAuthError(token))
                    return
                }

                // Debug logs
                CDCDebuggable.log(
                    LOG_TAG,
                    "authenticateWithPasskey: tokenResponse:\n ${token.jsonResponse}"
                )

                secureNewSession(token)

                // Success case
                val authSuccess = createAuthSuccess(token)
                authCallbacks.onSuccess?.invoke(authSuccess)
            } else {
                //TODO: Special error here. edge case (extend provider error)
                authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
            }
        } catch (e: SerializationException) {
            //TODO: Special error here. edge case (extend provider error)
            authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
        }
    }

    suspend fun get(authCallbacks: AuthCallbacks) {
        val getCredentials =
            AuthenticationApi(coreClient, sessionService).send(
                EP_PASSKEYS_GET_CREDENTIALS
            )

        // error case (getCredentials)
        if (getCredentials.isError()) {
            // End flow with error.
            authCallbacks.onError?.invoke(createAuthError(getCredentials))
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(getCredentials)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }

    suspend fun revoke(id: String, authCallbacks: AuthCallbacks) {
        try {
            val removeCredentials =
                AuthenticationApi(coreClient, sessionService).send(
                    EP_PASSKEYS_DELETE,
                    mutableMapOf(
                        "credentialId" to Base64.encodeToString(id.toByteArray(), Base64.NO_WRAP).trim()
                    )
                )
            // error case (removeCredentials)
            if (removeCredentials.isError()) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(removeCredentials))
                return
            }

            val idToken =
                removeCredentials.stringField("idToken")
            if (idToken == null) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                return
            }

            // Debug logs
            CDCDebuggable.log(LOG_TAG, "authenticateWithPasskey: idToken:\n $idToken")

            val disconnect = AuthenticationApi(coreClient, sessionService).send(
                EP_OAUTH_DISCONNECT,
                parameters = mutableMapOf(),
                headers = mutableMapOf("Authorization" to "Bearer $idToken")
            )
            if (disconnect.isError()) {
                // End flow with error.
                authCallbacks.onError?.invoke(createAuthError(disconnect))
                return
            }
            // Success case
            val authSuccess = createAuthSuccess(removeCredentials)
            authCallbacks.onSuccess?.invoke(authSuccess)

        } catch (e: SerializationException) {
            //TODO: Special error here. edge case (extend provider error)
            authCallbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
        }
    }
}