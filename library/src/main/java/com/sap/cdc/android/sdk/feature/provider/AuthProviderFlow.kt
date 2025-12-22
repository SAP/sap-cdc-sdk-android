package com.sap.cdc.android.sdk.feature.provider

import android.util.Log
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.CDCRequest
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_SOCIALIZE_REMOVE_CONNECTION
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.provider.sso.SSOResponseEntity
import com.sap.cdc.android.sdk.feature.provider.sso.SSOUtil
import com.sap.cdc.android.sdk.feature.session.SessionService
import java.lang.ref.WeakReference

class AuthProviderFlow(
    coreClient: CoreClient, sessionService: SessionService,
    private val provider: IAuthenticationProvider? = null,
    private val weakActivity: WeakReference<ComponentActivity>? = null
) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthProviderFlow"
    }

    suspend fun signIn(
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Register callbacks.
        val callbacks = AuthCallbacks().apply(authCallbacks)

        CDCDebuggable.log(LOG_TAG, "signIn: with parameters:$parameters")

        if (provider == null) {
            // End flow with error.
            callbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
            return
        }
        try {
            val signIn: AuthenticatorProviderResult = provider.signIn(weakActivity?.get())

            // Vary flow based on provider type.
            when (signIn.type) {
                // Native flows refer to social networks that require native SDK implementation
                // in order to authenticate the user (eg. Facebook, Google, etc.).
                ProviderType.NATIVE -> {
                    CDCDebuggable.log(LOG_TAG, "signIn: native")
                    val parameters = parameters ?: mutableMapOf()
                    if (!parameters.containsKey("loginMode")) {
                        parameters["loginMode"] = "standard"
                    }
                    parameters["provider"] = signIn.provider
                    parameters["providerSessions"] = signIn.providerSessions!!
                    parameters["conflictHandling"] = "fail"
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
                    dispose()
                    val authSuccess = createAuthSuccess(notifySocialLogin)
                    callbacks.onSuccess?.invoke(authSuccess)
                }

                // Web flows refer to all social provider types that are not native SDK based.
                // These providers require a web view to authenticate the user.
                ProviderType.WEB -> {
                    CDCDebuggable.log(LOG_TAG, "signIn: web")

                    // Handle error and potential interruption.
                    if (signIn.cdcResponse != null) {
                        val response = signIn.cdcResponse!!
                        if (isResolvableContext(response)) {
                            handleResolvableInterruption(response, callbacks)
                            return
                        }
                        return
                    }

                    // Handle successful login given active session.
                    val session = signIn.session!!
                    // Session will be secured when set.
                    sessionService.setSession(session)

                    // Refresh account information for flow response.
                    val getAccountInfo =
                        AuthenticationApi(coreClient, sessionService).send(
                            EP_ACCOUNTS_GET_ACCOUNT_INFO,
                            mutableMapOf("include" to "data,profile,emails")
                        )

                    // Error case
                    if (getAccountInfo.isError()) {
                        val authError = createAuthError(getAccountInfo)
                        callbacks.onError?.invoke(authError)
                        return
                    }

                    // Success case
                    secureNewSession(getAccountInfo)
                    val authSuccess = createAuthSuccess(getAccountInfo)
                    callbacks.onSuccess?.invoke(authSuccess)
                }

                // SSO provider authentication using a Central Login Page (CLP) only.
                ProviderType.SSO -> {
                    CDCDebuggable.log(LOG_TAG, "signIn: sso")
                    val ssoData = signIn.ssoData!!
                    val ssoUtil = SSOUtil()
                    val tokenResponse = ssoToken(ssoUtil, ssoData)
                    if (tokenResponse.containsKey("access_token")) {
                        // parse session info.
                        val ssoResponseEntity =
                            tokenResponse.serializeTo<SSOResponseEntity>()

                        if (ssoResponseEntity == null) {
                            callbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                            return
                        }

                        val session = ssoUtil.parseSessionInfo(ssoResponseEntity)
                        sessionService.setSession(session)

                        // Refresh account information for flow response.
                        val getAccountInfo =
                            AuthenticationApi(coreClient, sessionService).send(
                                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                                mutableMapOf("include" to "data,profile,emails")
                            )


                        // Error case
                        if (getAccountInfo.isError()) {
                            val authError = createAuthError(getAccountInfo)
                            callbacks.onError?.invoke(authError)
                            return
                        }

                        // Success case
                        secureNewSession(getAccountInfo)
                        val authSuccess = createAuthSuccess(getAccountInfo)
                        callbacks.onSuccess?.invoke(authSuccess)
                    } else {
                        callbacks.onError?.invoke(createAuthError(CDCResponse().providerError()))
                        return
                    }
                }
            }
        } catch (exception: ProviderException) {
            Log.d(LOG_TAG, exception.type.ordinal.toString())
            callbacks.onError?.invoke(createAuthError(CDCResponse().fromError(exception.error!!)))
        }
    }

    suspend fun removeConnection(
        provider: String,
        callbacks: AuthCallbacks
    ) {
        CDCDebuggable.log(LOG_TAG, "removeConnection: for provider:$provider")

        val remove = AuthenticationApi(coreClient, sessionService).send(
            EP_SOCIALIZE_REMOVE_CONNECTION, mutableMapOf("provider" to provider)
        )
        // Error case
        if (remove.isError()) {
            val authError = createAuthError(remove)
            callbacks.onError?.invoke(authError)
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(remove)
        callbacks.onSuccess?.invoke(authSuccess)
    }

    /**
     * Link a social provider account to the current site account.
     * 
     * This method handles the account linking flow by:
     * 1. Validating LinkingContext has required provider and authToken
     * 2. Initiating sign-in with loginMode=link
     * 3. Automatically calling connectAccount when sign-in succeeds
     * 4. Handling any authentication interruptions (2FA, pending registration, etc.)
     * 
     * The linking process uses an override transformer that waits for actual Success,
     * persisting through any authentication interruptions before executing the final
     * connectAccount operation.
     * 
     * @param parameters Additional parameters for the sign-in request
     * @param linkingContext Context containing provider and authToken for linking
     * @param authCallbacks Callback configuration for handling authentication results
     */
    suspend fun linkToProvider(
        parameters: MutableMap<String, String> = mutableMapOf(),
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Validate linkingContext before entering flow
        if (linkingContext.provider == null || linkingContext.authToken == null) {
            val callbacks = AuthCallbacks().apply(authCallbacks)
            callbacks.executeOnError(
                com.sap.cdc.android.sdk.feature.AuthError(
                    "LinkingContext missing required provider or authToken",
                    "MISSING_PROVIDER_DATA"
                )
            )
            return
        }
        
        parameters["loginMode"] = "link"

        signIn(parameters) {
            // Use Success override to wait for actual Success (persists through interruptions)
            doOnSuccessAndOverride { authSuccess ->
                // Execute connectAccount and return its result (Success or Error)
                connectAccountSync(linkingContext.provider!!, linkingContext.authToken!!)
            }

            // Register user callbacks AFTER override
            authCallbacks()
        }
    }


    override fun dispose() {
        weakActivity?.clear()
    }

    private suspend fun ssoToken(
        ssoUtil: SSOUtil,
        data: SSOAuthenticationData
    ): CDCResponse {
        val headers = hashMapOf(
            "apikey" to sessionService.siteConfig.apiKey
        )

        val parameters = mutableMapOf<String, String>()
        parameters["redirect_uri"] = data.redirectUri!!
        parameters["client_id"] = sessionService.siteConfig.apiKey
        parameters["grant_type"] = "authorization_code"
        parameters["code"] = data.code!!
        parameters["code_verifier"] = data.verifier!!
        val urlString = ssoUtil.getUrl(sessionService.siteConfig, SSOUtil.TOKEN)
        return Api(coreClient).post(
            CDCRequest(siteConfig = sessionService.siteConfig)
                .api(urlString)
                .parameters(parameters)
                .headers(headers)
        )

    }
}
