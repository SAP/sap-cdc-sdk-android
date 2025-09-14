package com.sap.cdc.bitsnbytes.feature.provider

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Pair
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.ProviderException
import com.sap.cdc.android.sdk.feature.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.feature.provider.ProviderType
import com.sap.cdc.android.sdk.feature.provider.ResultLoginActivity
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider.Companion.LOG_TAG
import com.sap.cdc.bitsnbytes.wxapi.WXEntryActivity
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class WeChatAuthenticationProvider : IAuthenticationProvider {

    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun getProvider(): String = "wechat"

    override suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine { continuation ->

            if (hostActivity == null) {
                continuation.resumeWithException(
                    ProviderException(
                        ProviderExceptionType.CANCELED,
                        CDCError.operationCanceled()
                    )
                )
                return@suspendCoroutine
            }

            val weChatApi: IWXAPI =
                WXAPIFactory.createWXAPI(hostActivity, WXEntryActivity.API_ID, false)
            weChatApi.registerApp(WXEntryActivity.API_ID)

            val launcherIntent = Intent(hostActivity, ResultLoginActivity::class.java)
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

            launcher = hostActivity.activityResultRegistry.register(
                "wechat-login",
                object : ActivityResultContract<Intent, Pair<Int, Intent>>() {
                    override fun createIntent(context: Context, input: Intent): Intent {
                        return input
                    }

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
                        if (resultData.hasExtra("canceled")) {
                            dispose()
                            continuation.resumeWithException(
                                ProviderException(
                                    ProviderExceptionType.CANCELED,
                                    CDCError.operationCanceled()
                                )
                            )
                        }
                        if (resultData.hasExtra("error")) {
                            val weChatErrorCode = resultData.getIntExtra("errorCode", -4)
                            // Handle we chat error however you want.
                            // Make sure to resumeWithException.
                            dispose()
                            continuation.resumeWithException(
                                ProviderException(
                                    ProviderExceptionType.PROVIDER_FAILURE,
                                    CDCError.providerError()
                                )
                            )
                        }
                        Log.d(LOG_TAG, "onActivityResult: intent null: ${resultData == null}")
                        val code = resultData.getStringExtra("code")
                        if (code != null) {

                            // Generate the relevant providerSession object required for CDC servers to validate the token.
                            val data = JsonObject(
                                mapOf(
                                    "wechat" to JsonObject(
                                        mapOf(
                                            "authToken" to JsonPrimitive(code),
                                            "providerID" to JsonPrimitive(WXEntryActivity.API_ID)
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
                        } else {
                            dispose()
                            continuation.resumeWithException(
                                ProviderException(
                                    ProviderExceptionType.PROVIDER_FAILURE,
                                    CDCError.providerError()
                                )
                            )
                        }
                    }
                }
            }
            launcher?.launch(launcherIntent)

            val request = SendAuth.Req()
            request.scope = "snsapi_userinfo"
            request.state = ""
            weChatApi.sendReq(request)
        }

    override suspend fun signOut(hostActivity: ComponentActivity?) {
        val weChatApi: IWXAPI =
            WXAPIFactory.createWXAPI(hostActivity, WXEntryActivity.API_ID, false)
        weChatApi.detach()
    }

    override fun dispose() {
        launcher?.unregister()
    }
}