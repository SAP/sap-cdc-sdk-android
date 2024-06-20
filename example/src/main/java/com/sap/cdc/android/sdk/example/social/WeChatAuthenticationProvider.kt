package com.sap.cdc.android.sdk.example.social

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import com.sap.cdc.android.sdk.authentication.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.authentication.provider.ProviderException
import com.sap.cdc.android.sdk.authentication.provider.ProviderExceptionType
import com.sap.cdc.android.sdk.authentication.provider.WebAuthenticationProvider.Companion.LOG_TAG
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.wxapi.WXEntryActivity
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class WeChatAuthenticationProvider : IAuthenticationProvider {

    override fun getProvider(): String = "wechat"

    override suspend fun providerSignIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
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

            val launcherIntent = Intent(hostActivity, WXEntryActivity::class.java)
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

            val launcher = hostActivity.activityResultRegistry.register(
                "wechat-login",
                object : ActivityResultContract<Intent, android.util.Pair<Int, Intent>>() {
                    override fun createIntent(context: Context, input: Intent): Intent  {
                        return input
                    }

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

                    }
                }
            }
//            launcher.launch(launcherIntent)

            val request = SendAuth.Req()
            request.scope = "snsapi_userinfo"
            request.state = ""
            weChatApi.sendReq(request)
        }

    override suspend fun providerSignOut(hostActivity: ComponentActivity?) {
        //
    }
}