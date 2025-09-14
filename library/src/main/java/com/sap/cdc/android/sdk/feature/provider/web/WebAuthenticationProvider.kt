package com.sap.cdc.android.sdk.feature.provider.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Pair
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.core.api.utils.AndroidBase64Encoder
import com.sap.cdc.android.sdk.core.api.utils.Signing
import com.sap.cdc.android.sdk.core.api.utils.SigningSpec
import com.sap.cdc.android.sdk.core.api.utils.toEncodedQuery
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthEndpoints
import com.sap.cdc.android.sdk.feature.AuthenticationService
import com.sap.cdc.android.sdk.feature.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.ProviderException
import com.sap.cdc.android.sdk.feature.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.feature.provider.ProviderType
import com.sap.cdc.android.sdk.feature.session.Session
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
    private val siteConfig: SiteConfig,
    private val session: Session?,
) :
    IAuthenticationProvider {

    companion object {
        const val LOG_TAG = "WebAuthenticationProvider"
    }

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = this.socialProvider

    override suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult {
        CDCDebuggable.log(LOG_TAG, "signIn: with parameters: $socialProvider")
        return suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.HOST_NULL,
                        CDCError.Companion.contextError()
                    )
                )
                return@suspendCoroutine
            }

            val webProviderIntent = Intent(hostActivity, WebLoginActivity::class.java)
            val uri = generateUri(hostActivity)
            webProviderIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            webProviderIntent.putExtra(WebLoginActivity.Companion.EXTRA_URI, uri)

            CDCDebuggable.log(LOG_TAG, "signIn: launching web login activity with uri: $uri")

            launcher = hostActivity.activityResultRegistry.register(
                "web-login",
                object : ActivityResultContract<Intent, Pair<Int, Intent>>() {
                    override fun createIntent(context: Context, input: Intent): Intent = input

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): Pair<Int, Intent> {
                        return Pair.create(resultCode, intent)
                    }
                }
            ) { result ->
                val resultCode = result.first
                when (resultCode) {
                    Activity.RESULT_CANCELED -> {
                        CDCDebuggable.log(LOG_TAG, "signIn: RESULT_CANCELED")
                        dispose()
                        continuation.resumeWithException(
                            ProviderException(
                                ProviderExceptionType.CANCELED,
                                CDCError.Companion.operationCanceled()
                            )
                        )
                    }

                    Activity.RESULT_OK -> {
                        CDCDebuggable.log(LOG_TAG, "signIn: RESULT_OK")
                        val resultData = result.second
                        CDCDebuggable.log(
                            LOG_TAG,
                            "onActivityResult: intent null: ${resultData == null}"
                        )

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
            siteConfig.applicationContext.getEncryptedPreferences(
                AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
            )
        val gmid = esp.getString(AuthenticationService.Companion.CDC_GMID, "")

        val uriParameters = mutableMapOf(
            "redirect_uri" to "gigya://gsapi/" + hostActivity.packageName + "/login_result",
            "response_type" to "token",
            "client_id" to siteConfig.apiKey,
            "gmid" to gmid!!,
            "x_secret_type" to "oauth1",
            "x_sdk" to "Android_1.0.0",
            "x_provider" to getProvider(),
            "nonce" to generateNonce()
        )

        // Check session state to apply authentication parameters.
        if (session != null) {
            uriParameters["oauth_token"] = session.token
            uriParameters["timestamp"] = siteConfig.getServerTimestamp()
            Signing(base64Encoder = AndroidBase64Encoder()).newSignature(
                SigningSpec(
                    session.secret,
                    AuthEndpoints.Companion.EP_SOCIALIZE_LOGIN,
                    HttpMethod.Companion.Get.value,
                    uriParameters
                )
            )
        }

        return String.format(
            "%s://%s.%s/%s?%s", "https", "socialize", siteConfig.domain,
            AuthEndpoints.Companion.EP_SOCIALIZE_LOGIN, uriParameters.toEncodedQuery()
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
            sessionExpiration!!
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

    override suspend fun signOut(hostActivity: ComponentActivity?) {
        // Stub. Not implemented via web authentication provider.
    }

    override fun dispose() {
        launcher?.unregister()
    }

}