package com.sap.cdc.android.sdk.auth.provider

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_SOCIALIZE_LOGIN
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_GMID
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionEncryption
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.core.api.Signing
import com.sap.cdc.android.sdk.core.api.SigningSpec
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.core.api.toEncodedQuery
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import io.ktor.http.HttpMethod
import io.ktor.util.generateNonce
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class WebAuthenticationProvider(
    private val socialProvider: String,
    private val sessionService: SessionService
) :
    IAuthenticationProvider {

    companion object {
        const val LOG_TAG = "WebAuthenticationProvider"
    }

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = this.socialProvider

    override suspend fun providerSignIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult {
        return suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    com.sap.cdc.android.sdk.auth.provider.ProviderException(
                        com.sap.cdc.android.sdk.auth.provider.ProviderExceptionType.HOST_NULL,
                        CDCError.contextError()
                    )
                )
                return@suspendCoroutine
            }

            val webProviderIntent = Intent(hostActivity, WebLoginActivity::class.java)
            val uri = generateUri(hostActivity)
            webProviderIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            webProviderIntent.putExtra(WebLoginActivity.EXTRA_URI, uri)

            launcher = hostActivity.activityResultRegistry.register(
                "web-login",
                object : ActivityResultContract<Intent, android.util.Pair<Int, Intent>>() {
                    override fun createIntent(context: Context, input: Intent): Intent = input

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): android.util.Pair<Int, Intent> {
                        return android.util.Pair.create(resultCode, intent)
                    }
                }
            ) { result ->
                val resultCode = result.first
                when (resultCode) {
                    RESULT_CANCELED -> {
                        dispose()
                        continuation.resumeWithException(
                            ProviderException(
                                ProviderExceptionType.CANCELED,
                                CDCError.operationCanceled()
                            )
                        )
                    }

                    RESULT_OK -> {
                        val resultData = result.second
                        Log.d(LOG_TAG, "onActivityResult: intent null: ${resultData == null}")

                        if (resultData != null) {
                            val status = resultData.getStringExtra("status")
                            if (status != null && status == "ok") {
                                // Parse session information.
                                val session = handleSessionInfo(resultData)

                                val authenticatorProviderResult = AuthenticatorProviderResult(
                                    provider = getProvider(),
                                    type = ProviderType.WEB,
                                    session = session
                                )

                                dispose()
                                continuation.resume(authenticatorProviderResult)
                            } else {
                                // Parse error information.
                                val cdcError = handleErrorInfo(resultData)

                                dispose()
                                continuation.resumeWithException(
                                    ProviderException(
                                        ProviderExceptionType.PROVIDER_FAILURE,
                                        cdcError
                                    )
                                )
                            }
                        }
                    }
                }
            }
            launcher?.launch(webProviderIntent)
        }
    }

    /**
     * Generate authentication URI.
     */
    private fun generateUri(hostActivity: ComponentActivity): String {
        // Fetch gmid
        val esp =
            sessionService.siteConfig.applicationContext.getEncryptedPreferences(
                CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val gmid = esp.getString(CDC_GMID, "")

        val uriParameters = mutableMapOf(
            "redirect_uri" to "gigya://gsapi/" + hostActivity.packageName + "/login_result",
            "response_type" to "token",
            "client_id" to sessionService.siteConfig.apiKey,
            "gmid" to gmid!!,
            "x_secret_type" to "oauth1",
            "x_sdk" to "Android_1.0.0",
            "x_provider" to getProvider(),
            "nonce" to generateNonce()
        )

        // Check session state to apply authentication parameters.
        val session = sessionService.sessionSecure.getSession()
        if (session != null) {
            uriParameters["oauth_token"] = session.token
            uriParameters["timestamp"] = sessionService.siteConfig.getServerTimestamp()
            Signing().newSignature(
                SigningSpec(
                    session.secret,
                    EP_SOCIALIZE_LOGIN,
                    HttpMethod.Get.value,
                    uriParameters
                )
            )
        }

        return String.format(
            "%s://%s.%s/%s?%s", "https", "socialize", sessionService.siteConfig.domain,
            EP_SOCIALIZE_LOGIN, uriParameters.toEncodedQuery()
        )
    }

    /**
     * Parse result session information.
     */
    private fun handleSessionInfo(result: Intent): Session {
        // Parse session information.
        val sessionToken = result.getStringExtra("access_token")
        val sessionSecret = result.getStringExtra("x_access_token_secret")
        val sessionExpiration = result.getStringExtra("expires_in")?.toLong()

        // Generate  session.
        return Session(
            sessionToken!!,
            sessionSecret!!,
            sessionExpiration!!,
            SessionEncryption.DEFAULT
        )
    }

    /**
     * Parse error information. May result in a continuous flow to resolve the error.
     */
    //TODO: Change flow to base response. Not handling CDCError as a object.
    private fun handleErrorInfo(result: Intent): CDCError {
        val errorDescription = result.getStringExtra("error_description")
        val parts =
            errorDescription!!.replace("+", "").split("-".toRegex()).dropLastWhile { it.isEmpty() }
        val errorCode = parts[0].trim { it <= ' ' }.toInt()
        val errorMessage = parts[1].trim { it <= ' ' }

        // Generate error from parsed data.
        val error = CDCError(errorCode, errorMessage)

        // Extract registration token if available. Should be by default...
        if (result.extras?.containsKey("x_regToken") == true) {
            val regToken = result.getStringExtra("x_regToken")
            //error.addDynamic("regToken", regToken!!)
        }

        return error
    }

    override suspend fun providerSignOut(hostActivity: ComponentActivity?) {
        // Stub. Not implemented via web authentication provider.
    }

    override fun dispose() {
        launcher?.unregister()
    }

}
