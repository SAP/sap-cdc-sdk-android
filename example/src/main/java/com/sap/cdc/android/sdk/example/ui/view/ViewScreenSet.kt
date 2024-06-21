package com.sap.cdc.android.sdk.example.ui.view

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.social.FacebookAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.GoogleAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.LineAuthenticationProvider
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelScreenSet
import com.sap.cdc.android.sdk.sceensets.ScreenSetBuilder
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.CANCELED
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.HIDE
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.LOGIN
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.LOGIN_STARTED
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.LOGOUT
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSWebChromeClient
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSWebViewClient

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ViewScreenSet(viewModel: ViewModelScreenSet) {

    val context = LocalContext.current

    // Create only when file access is required..
    val bridgingWebChromeClient = WebBridgeJSWebChromeClient()
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            bridgingWebChromeClient.onActivityResult(uri)
        }
    )

    // Create screen set request parameters.
    val params = mutableMapOf<String, Any>("screenSet" to "Default-RegistrationLogin")

    // Build Uri.
    val screenSetUrl = ScreenSetBuilder.Builder(
        context.getString(R.string.com_sap_cxcdc_apikey)
    )
        .domain(context.getString(R.string.com_sap_cxcdc_domain))
        .params(params)
        .build().screenSetUrl

    // Set native social provider authenticators.
    val webBridgeJS: WebBridgeJS = viewModel.newWebBridgeJS()

    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { it ->
            WebView(it).apply {
                settings.javaScriptEnabled = true
                settings.setSupportZoom(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                webViewClient = WebBridgeJSWebViewClient(webBridgeJS) { browserUri ->
                    //TODO: Check for legacy action. is it required??
                    val intent = Intent(Intent.ACTION_VIEW, browserUri)
                    context.startActivity(intent)
                }

                // Add only hen file access is required.
                settings.allowFileAccess = true
                webChromeClient = bridgingWebChromeClient
            }
        }, update = { webView ->
            Log.d("ScreenSetView", "update")
            webBridgeJS.attachBridgeTo(webView)
            webBridgeJS.setNativeSocialProviders(
                mutableMapOf(
                    "facebook" to FacebookAuthenticationProvider(),
                    "google" to GoogleAuthenticationProvider(),
                    "line" to LineAuthenticationProvider()
                )
            )
            webBridgeJS.registerForEvents { webBridgeJSEvent ->
                val eventName = webBridgeJSEvent.name()
                Log.d("ScreenSetView", "event: $eventName")
                when (eventName) {
                    LOGIN_STARTED -> {

                    }

                    CANCELED -> {

                    }

                    HIDE -> {

                    }

                    LOGIN -> {

                    }

                    LOGOUT -> {

                    }
                }
            }

            webView.loadDataWithBaseURL(
                WebBridgeJS.BASE_URL,
                screenSetUrl,
                WebBridgeJS.MIME_TYPE,
                WebBridgeJS.ENCODING,
                null
            )
        }
    )
}