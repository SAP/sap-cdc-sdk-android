package com.sap.cdc.android.sdk.example.social

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.linecorp.linesdk.LineApiResponseCode
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.api.LineApiClientBuilder
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import com.sap.cdc.android.sdk.auth.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.ProviderType
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.R
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class LineAuthenticationProvider() : IAuthenticationProvider {

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = "line"

    override suspend fun providerSignIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    com.sap.cdc.android.sdk.auth.provider.ProviderException(
                        com.sap.cdc.android.sdk.auth.provider.ProviderExceptionType.CANCELED,
                        CDCError.operationCanceled()
                    )
                )
                return@suspendCoroutine
            }

            val channelId = hostActivity.getString(R.string.line_channel_id)
            val loginIntent: Intent = LineLoginApi.getLoginIntent(
                hostActivity,
                channelId,
                LineAuthenticationParams.Builder()
                    .scopes(listOf(Scope.PROFILE))
                    .build()
            )

            launcher = hostActivity.activityResultRegistry.register(
                "line-login",
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
                val resultData = result.second
                val lineResult = LineLoginApi.getLoginResultFromIntent(resultData)
                when (lineResult.responseCode) {
                    LineApiResponseCode.SUCCESS -> {
                        Log.d("LineAuthenticationProvider", "SUCCESS")
                        val token = lineResult.lineCredential?.accessToken?.tokenString
                        val data = JsonObject(
                            mapOf(
                                "line" to JsonObject(
                                    mapOf(
                                        "authToken" to JsonPrimitive(token),
                                    )
                                )
                            )
                        )

                        val providerSession = data.toString()

                        val authenticatorProviderResult = AuthenticatorProviderResult(
                            provider = getProvider(),
                            type = ProviderType.NATIVE,
                            providerSessions = providerSession
                        )

                        dispose()
                        continuation.resume(authenticatorProviderResult)
                    }

                    LineApiResponseCode.CANCEL -> {
                        Log.d("LineAuthenticationProvider", "CANCEL")

                        dispose()
                        continuation.resumeWithException(
                            com.sap.cdc.android.sdk.auth.provider.ProviderException(
                                com.sap.cdc.android.sdk.auth.provider.ProviderExceptionType.CANCELED,
                                CDCError.operationCanceled()
                            )
                        )
                    }

                    else -> {
                        Log.d("LineAuthenticationProvider", "ERROR")
                        Log.d("LineAuthenticationProvider", lineResult.errorData.toString())

                        val providerException =
                            com.sap.cdc.android.sdk.auth.provider.ProviderException(
                                com.sap.cdc.android.sdk.auth.provider.ProviderExceptionType.PROVIDER_FAILURE,
                                CDCError.providerError()
                            )
                        providerException.error?.addDynamic(
                            "providerMessage",
                            lineResult.errorData.toString()
                        )

                        dispose()
                        continuation.resumeWithException(providerException)
                    }
                }
            }
            launcher?.launch(loginIntent)
        }

    override suspend fun providerSignOut(hostActivity: ComponentActivity?) {
        if (hostActivity == null) return
        val channelId =""// hostActivity.getString(R.string.line_channel_id)
        val client = LineApiClientBuilder(hostActivity, channelId).build()
        client.logout()
    }

    override fun dispose() {
        launcher?.unregister()
    }
}