package com.sap.cdc.android.sdk.example.ui.view.flow

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelScreenSet
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSConfig
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.CANCELED
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.HIDE
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.LOGIN
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSEvent.Companion.LOGOUT
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSWebChromeClient
import com.sap.cdc.android.sdk.sceensets.WebBridgeJSWebViewClient
import com.sap.cdc.android.sdk.screensets.ScreenSetUrlBuilder
import com.sap.cdc.android.sdk.screensets.WebBridgeJS

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Custom view for loading screen-sets.
 * Compose requires WebViews to be created within an AndroidView composable.
 * This view demonstrates the basic usage of the WebBridgeJS element that allows streaming
 * events from the WebSDK to the mobile SDK.
 */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ViewScreenSet(viewModel: ViewModelScreenSet, screenSet: String, startScreen: String) {

    val context = LocalContext.current

    var screenSetError by remember { mutableStateOf("") }

    // Create only when file access is required..
    val bridgingWebChromeClient = WebBridgeJSWebChromeClient()
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            bridgingWebChromeClient.onActivityResult(uri)
        }
    )

    // Create screen set request parameters.
    val params = mutableMapOf<String, Any>(
        "screenSet" to screenSet,
        "startScreen" to startScreen
    )

    // Build Uri.
    val screenSetUrl = ScreenSetUrlBuilder.Builder(
        context.getString(R.string.com_sap_cxcdc_apikey)
    )
        .domain(context.getString(R.string.com_sap_cxcdc_domain))
        .params(params)
        .build()

    // Set native social provider authenticators.
    val webBridgeJS: WebBridgeJS = viewModel.newWebBridgeJS()
    // Add specific web bridge configurations.
    webBridgeJS.addConfig(
        WebBridgeJSConfig.Builder().obfuscate(true).build()
    )

    Box {
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

                // Set external authenticators. SDK will no longer use reflection to
                // retrieve external providers.
                webBridgeJS.setNativeSocialProviders(
                    viewModel.identityService.getAuthenticatorMap()
                )

                // Register for JS events.
                webBridgeJS.registerForEvents { webBridgeJSEvent ->
                    val eventName = webBridgeJSEvent.name()
                    // Log event.
                    Log.d("ScreenSetView", "event: $eventName")
                    when (eventName) {
                        CANCELED -> {
                            screenSetError = "Operation canceled"
                            Handler(Looper.getMainLooper()).postDelayed({
                                screenSetError = ""
                            }, 2000)
                        }

                        HIDE -> {
                            // Destroy the WebView instance.
                            webView.post {
                                webView.destroy()
                            }
                        }

                        LOGIN -> {
                            // Flow successful. Navigate to profile screen.
                            webView.post {
                                NavigationCoordinator.INSTANCE.popAndNavigate(ProfileScreenRoute.MyProfile.route)
                            }
                        }

                        LOGOUT -> {
                            // Navigate back to close the screen set view.
                            NavigationCoordinator.INSTANCE.navigateUp()
                        }
                    }
                }

                // Load URL.
                webBridgeJS.load(webView, screenSetUrl)
            }
        )
        // Screen-set error optional display.
        if (screenSetError.isNotEmpty()) {
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "",
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = screenSetError,
                    color = Color.Red,
                )
            }
        }
    }
}