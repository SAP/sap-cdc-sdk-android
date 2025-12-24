package com.sap.cdc.android.sdk.feature.login

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.ATokenCredentials
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_ID_CREATE_TOKEN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_LOGIN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.feature.AuthError
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.LoginIdCredentials
import com.sap.cdc.android.sdk.feature.session.SessionService

class AuthLoginFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthLoginFlow"
    }

    suspend fun login(
        parameters: MutableMap<String, String>,
        callbacks: AuthCallbacks,
    ) {
        CDCDebuggable.log(LOG_TAG, "login: with parameters:$parameters")

        val login = AuthenticationApi(coreClient, sessionService)
            .send(EP_ACCOUNTS_LOGIN, parameters)

        // Direct callback handling - no IAuthResponse creation
        handleLoginResponse(login, callbacks)
    }

    suspend fun login(
        parameters: MutableMap<String, String>,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        login(parameters, callbacks)
    }


    suspend fun login(
        credentials: LoginIdCredentials,
        callbacks: AuthCallbacks
    ) {
        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        parameters["loginID"] = credentials.loginId
        parameters["password"] = credentials.password
        login(parameters, callbacks)
    }

    suspend fun login(
        credentials: ATokenCredentials,
        callbacks: AuthCallbacks
    ) {
        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        parameters["aToken"] = credentials.aToken
        parameters["password"] = credentials.password
        login(parameters, callbacks)
    }

    suspend fun login(
        credentials: CustomIdCredentials,
        callbacks: AuthCallbacks
    ) {
        CDCDebuggable.log(LOG_TAG, "login: with customID:$credentials")

        // Create parameter map according to credentials input.
        val parameters = mutableMapOf<String, String>()
        parameters["identifier"] = credentials.identifier
        parameters["identifierType"] = credentials.identifierType

        val createToken = AuthenticationApi(coreClient, sessionService)
            .send(
                EP_ACCOUNTS_ID_CREATE_TOKEN,
                parameters
            )
        // Error case
        if (createToken.isError()) {
            val authError = createAuthError(createToken)
            callbacks.onError?.invoke(authError)
            return
        }

        // Create token success case
        val aToken: String? = createToken.stringField("aToken")

        // Reuse parameters map
        parameters.clear()
        parameters["aToken"] = aToken ?: ""
        parameters["password"] = credentials.password
        login(parameters = parameters, callbacks = callbacks)
    }

    private suspend fun handleLoginResponse(
        response: CDCResponse,
        callbacks: AuthCallbacks,
    ) {
        if (isResolvableContext(response)) {
            // Resolvable interruption case
            handleResolvableInterruption(response, callbacks)
            return
        }
        if (response.isError()) {
            // Error case
            val authError = createAuthError(response)
            callbacks.onError?.invoke(authError)
            return
        }
        // Success case
        secureNewSession(response)
        val authSuccess = createAuthSuccess(response)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * Initiate social login related authentication flow.
     * "NotifySocialLogin" call is used with social sign in flows (simple & link).
     * @see [accounts.notifySocialLogin](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/413795be70b21014bbc5a10ce4041860.html?q=notifySocialLogin)
     */
    suspend fun notifySocialLogin(
        parameters: MutableMap<String, String>,
        callbacks: AuthCallbacks
    ) {
        CDCDebuggable.log(LOG_TAG, "notifySocialLogin: with parameters:$parameters")
        val notifySocialLogin =
            AuthenticationApi(coreClient, sessionService).send(
                EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN,
                parameters
            )

        // Resolvable case
        if (isResolvableContext(notifySocialLogin)) {
            handleResolvableInterruption(notifySocialLogin, callbacks)
            return
        }

        // Error case
        if (notifySocialLogin.isError()) {
            val authError = createAuthError(notifySocialLogin)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        secureNewSession(notifySocialLogin)
        val authSuccess = createAuthSuccess(notifySocialLogin)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * Link an existing account to the site after credentials-based login.
     * 
     * This method handles the account linking flow by:
     * 1. Validating LinkingContext has required provider and authToken
     * 2. Initiating login with provided credentials
     * 3. Automatically calling connectAccount when login succeeds
     * 4. Handling any authentication interruptions (2FA, pending registration, etc.)
     * 
     * The linking process uses an override transformer that waits for actual Success,
     * persisting through any authentication interruptions before executing the final
     * connectAccount operation.
     * 
     * @param parameters Login parameters (loginID, password, etc.)
     * @param linkingContext Context containing provider and authToken for linking
     * @param authCallbacks Callback configuration for handling authentication results
     */
    suspend fun linkToSite(
        parameters: MutableMap<String, String>,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Validate linkingContext before entering flow
        if (linkingContext.provider == null || linkingContext.authToken == null) {
            val callbacks = AuthCallbacks().apply(authCallbacks)
            val error = AuthError(
                code = null,
                message = "LinkingContext missing required provider or authToken",
                details = "MISSING_PROVIDER_DATA"
            )
            callbacks.onError?.invoke(error)
            return
        }
        
        login(parameters) {
            // Use Success override to wait for actual Success (persists through interruptions)
            doOnSuccessAndOverride { authSuccess ->
                // Execute connectAccount and return its result (Success or Error)
                connectAccountSync(linkingContext.provider!!, linkingContext.authToken!!)
            }

            // Apply user auth callbacks
            authCallbacks()
        }
    }


}
