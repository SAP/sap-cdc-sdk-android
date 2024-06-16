package com.sap.cdc.android.sdk.example.social

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.authentication.provider.AuthenticatorProviderResult
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class WeChatAuthenticationProvider : IAuthenticationProvider {

    override fun getProvider(): String = "wechat"

    override suspend fun providerSignIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult =
        suspendCoroutine {
            //
        }

    override suspend fun providerSignOut(hostActivity: ComponentActivity?) {
        //
    }
}