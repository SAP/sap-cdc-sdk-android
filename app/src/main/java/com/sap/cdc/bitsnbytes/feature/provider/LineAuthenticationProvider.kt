package com.sap.cdc.bitsnbytes.feature.provider

import android.R.attr.data
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Pair
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.linecorp.linesdk.LineApiResponseCode
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.api.LineApiClientBuilder
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import com.sap.cdc.android.sdk.feature.AuthErrorCodes
import com.sap.cdc.android.sdk.feature.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.ProviderException
import com.sap.cdc.android.sdk.feature.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.feature.provider.ProviderType
import com.sap.cdc.bitsnbytes.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class LineAuthenticationProvider() : IAuthenticationProvider {

    companion object {

        const val LOG_TAG = "LineAuthenticationProvider"
    }

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = "line"

    override suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.CANCELED,
                        AuthErrorCodes.operationCanceled()
                    )
                )
                return@suspendCoroutine
            }

            val channelId = hostActivity.getString(R.string.line_channel_id)
            val loginIntent: Intent = LineLoginApi.getLoginIntent(
                hostActivity,
                channelId,
                LineAuthenticationParams.Builder()
                    .scopes(listOf(Scope.PROFILE, Scope.OC_EMAIL, Scope.OPENID_CONNECT))
                    .build()
            )

            launcher = hostActivity.activityResultRegistry.register(
                "line-login",
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
                val resultData = result.second
                val lineResult = LineLoginApi.getLoginResultFromIntent(resultData)
                when (lineResult.responseCode) {
                    LineApiResponseCode.SUCCESS -> {
                        Log.d(LOG_TAG, "SUCCESS")
                        val token = lineResult.lineCredential?.accessToken?.tokenString
                        val idToken = lineResult.lineIdToken?.rawString

                        val providerSession = data.toString()

                        val authenticatorProviderResult = AuthenticatorProviderResult(
                            provider = getProvider(),
                            type = ProviderType.NATIVE,
                            providerSessionData = mapOf(
                                "authToken" to token,
                                "idToken" to idToken
                            )
                        )

                        dispose()
                        continuation.resume(authenticatorProviderResult)
                    }

                    LineApiResponseCode.CANCEL -> {
                        Log.d(LOG_TAG, "CANCEL")

                        dispose()
                        continuation.resumeWithException(
                            ProviderException(
                                ProviderExceptionType.CANCELED,
                                AuthErrorCodes.operationCanceled()
                            )
                        )
                    }

                    else -> {
                        Log.d(LOG_TAG, "ERROR")
                        Log.d(LOG_TAG, lineResult.errorData.toString())

                        val providerException =
                            ProviderException(
                                ProviderExceptionType.PROVIDER_FAILURE,
                                AuthErrorCodes.providerError().copy(details = lineResult.errorData.toString())
                            )

                        dispose()
                        continuation.resumeWithException(providerException)
                    }
                }
            }
            launcher?.launch(loginIntent)
        }

    override suspend fun signOut(hostActivity: ComponentActivity?) {
        if (hostActivity == null) return
        val channelId = ""// hostActivity.getString(R.string.line_channel_id)
        val client = LineApiClientBuilder(hostActivity, channelId).build()
        client.logout()
    }

    override fun dispose() {
        launcher?.unregister()
    }
}
