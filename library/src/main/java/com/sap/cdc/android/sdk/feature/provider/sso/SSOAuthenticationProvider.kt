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
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.extensions.parseQueryStringParams
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
 * Created by Tal Mirmelshtein on 13/12/2024
 * Copyright: SAP LTD.
 */

/**
 * Single sign on authentication provider class.
 *
 * Initiate CLP SSO authentication flow.
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

            CDCDebuggable.log(
                LOG_TAG,
                "SSOAuthenticationProvider: signIn"
            )

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.CANCELED,
                        CDCError.Companion.operationCanceled()
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

            CDCDebuggable.log(
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
                        CDCDebuggable.log(
                            LOG_TAG,
                            "SSOAuthenticationProvider: signIn: RESULT_CANCELED"
                        )
                        dispose()
                        continuation.resumeWithException(
                            ProviderException(
                                ProviderExceptionType.CANCELED,
                                CDCError.Companion.operationCanceled()
                            )
                        )
                    }

                    Activity.RESULT_OK -> {
                        CDCDebuggable.log(
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
                                    CDCError.Companion.operationCanceled()
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
     * Parse error_uri from error response.
     */
    private fun parseErrorUri(uriString: String): String {
        val uri = Uri.parse(uriString)

        // Map query string parameters.
        val queryParams = uri.encodedFragment!!.parseQueryStringParams()
        val json = JSONObject()
        json.put("callId", queryParams["callId"])
        json.put("errorCode", (queryParams["error_code"] as String).toInt())
        json.put("errorDetails", queryParams["error_description"])
        return json.toString()
    }
}