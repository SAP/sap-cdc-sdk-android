package com.sap.cdc.android.sdk.feature.provider.sso

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.parseQueryStringParams
import com.sap.cdc.android.sdk.feature.AuthErrorCodes
import com.sap.cdc.android.sdk.feature.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.ProviderException
import com.sap.cdc.android.sdk.feature.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.feature.provider.ProviderType
import com.sap.cdc.android.sdk.feature.provider.SSOAuthenticationData
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * SSO authentication provider using Custom Tabs and PKCE.
 * 
 * Implements single sign-on authentication flow using OAuth 2.0 with PKCE
 * (Proof Key for Code Exchange) for enhanced security. Uses Chrome Custom Tabs
 * for a seamless authentication experience.
 * 
 * @property siteConfig CIAM site configuration
 * @property params Optional authentication parameters
 * 
 * @author Tal Mirmelshtein
 * @since 13/12/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see IAuthenticationProvider
 * @see PKCEUtil
 */
class SSOAuthenticationProvider(
    private val siteConfig: SiteConfig,
    private val params: MutableMap<String, Any>?

) : IAuthenticationProvider {

    companion object {
        const val LOG_TAG = "SSOAuthenticationProvider"
    }

    private lateinit var redirectUri: String
    private var pkceUtil = PKCEUtil()
    private val ssoUtil = SSOUtil()

    init {
        pkceUtil.newChallenge()
    }

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = "sso"

    override suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine { continuation ->

            CIAMDebuggable.log(
                LOG_TAG,
                "SSOAuthenticationProvider: signIn"
            )

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.CANCELED,
                        AuthErrorCodes.operationCanceled()
                    )
                )
                return@suspendCoroutine
            }

            redirectUri = "gsapi://${hostActivity.packageName}/login/"

            val url = ssoUtil.getAuthorizeUrl(
                siteConfig = siteConfig,
                params = params,
                redirectUri = redirectUri,
                challenge = pkceUtil.challenge!!
            )

            val ssoProviderIntent = Intent(hostActivity, SSOLoginActivity::class.java)
            ssoProviderIntent.putExtra(SSOLoginActivity.Companion.EXTRA_URI, url)

            CIAMDebuggable.log(
                LOG_TAG,
                "SSOAuthenticationProvider: signIn: url: $url"
            )

            launcher = hostActivity.activityResultRegistry.register(
                "sso-login",
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
                        CIAMDebuggable.log(
                            LOG_TAG,
                            "SSOAuthenticationProvider: signIn: RESULT_CANCELED"
                        )
                        dispose()
                        continuation.resumeWithException(
                            ProviderException(
                                ProviderExceptionType.CANCELED,
                                AuthErrorCodes.operationCanceled()
                            )
                        )
                    }

                    Activity.RESULT_OK -> {
                        CIAMDebuggable.log(
                            LOG_TAG,
                            "SSOAuthenticationProvider: signIn: RESULT_OK"
                        )
                        val resultData = result.second
                        Log.d(
                            WebAuthenticationProvider.Companion.LOG_TAG,
                            "onActivityResult: intent null: ${resultData == null}"
                        )
                        if (resultData != null && resultData.data != null && resultData.data is Uri) {
                            val uri: Uri = resultData.data as Uri
                            val parsed = getQueryKeyValueMap(uri)
                            if (parsed.containsKey("code")) {
                                val authenticatorProviderResult = AuthenticatorProviderResult(
                                    provider = getProvider(),
                                    type = ProviderType.SSO,
                                    ssoData = SSOAuthenticationData(
                                        code = parsed["code"] as String,
                                        redirectUri = redirectUri,
                                        verifier = pkceUtil.verifier!!
                                    )
                                )
                                continuation.resume(authenticatorProviderResult)
                            } else {
                                if (parsed.containsKey("error")) {
                                    val jsonError = parseErrorUri(parsed["error_uri"] as String)
                                }
                            }
                        } else {
                            continuation.resumeWithException(
                                ProviderException(
                                    ProviderExceptionType.CANCELED,
                                    AuthErrorCodes.operationCanceled()
                                )
                            )
                        }
                        dispose()
                    }
                }
            }
            launcher?.launch(ssoProviderIntent)
        }


    override suspend fun signOut(hostActivity: ComponentActivity?) {
        //Stub - No specific sign out implementation using CLP SSO. Default session logout applies.
    }

    override fun dispose() {
        launcher?.unregister()
    }

    /**
     * Extracts query parameters from URI into a map.
     * @param uri The URI to parse
     * @return HashMap of query parameter key-value pairs
     */
    private fun getQueryKeyValueMap(uri: Uri): HashMap<String, Any> {
        val keyValueMap = HashMap<String, Any>()
        var key: String
        var value: String

        val keyNamesList = uri.queryParameterNames
        val iterator = keyNamesList.iterator()

        while (iterator.hasNext()) {
            key = iterator.next() as String
            value = uri.getQueryParameter(key) as String
            keyValueMap[key] = value
        }
        return keyValueMap
    }

    /**
     * Parses error URI from OAuth error response.
     * Extracts error code and details from the error_uri parameter.
     * @param uriString The error URI string
     * @return JSON string containing error details
     */
    private fun parseErrorUri(uriString: String): String {
        val uri = uriString.toUri()

        // Map query string parameters.
        val queryParams = uri.encodedFragment!!.parseQueryStringParams()
        val json = JSONObject()
        json.put("callId", queryParams["callId"])
        json.put("errorCode", (queryParams["error_code"] as String).toInt())
        json.put("errorDetails", queryParams["error_description"])
        return json.toString()
    }
}
