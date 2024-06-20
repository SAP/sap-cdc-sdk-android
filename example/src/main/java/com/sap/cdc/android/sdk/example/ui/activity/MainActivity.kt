package com.sap.cdc.android.sdk.example.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sap.cdc.android.sdk.example.ui.theme.CDCAndroidSdkExampleTheme
import com.sap.cdc.android.sdk.example.ui.view.ViewApp
import com.sap.cdc.android.sdk.example.wxapi.WXEntryActivity
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            CDCAndroidSdkExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ViewApp()
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val weChatApi: IWXAPI =
                WXAPIFactory.createWXAPI(this, WXEntryActivity.API_ID, false)
            weChatApi.registerApp(WXEntryActivity.API_ID)

            val request = SendAuth.Req()
            request.scope = "snsapi_userinfo"
            request.state = ""
            weChatApi.sendReq(request)
        }, 8000)



    }
}







