package com.sap.cdc.bitsnbytes.ui.view.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetUrlBuilder
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSConfig
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSEvent.Companion.CANCELED
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSEvent.Companion.HIDE
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSEvent.Companion.LOGIN
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSEvent.Companion.LOGOUT
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebChromeClient
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebViewClient
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages

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
fun ScreenSetView(
    viewModel: ScreenSetViewModel,
    screenSet: String, startScreen: String) {
    val context = LocalContext.current

    var screenSetError by remember { mutableStateOf("") }

    // Declare webBridgeJSWebChromeClient as a mutable variable
    var webBridgeJSWebChromeClient: WebBridgeJSWebChromeClient? by remember { mutableStateOf(null) }

    // Define the file chooser launcher (Optional - only if you need file chooser)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        webBridgeJSWebChromeClient?.handleActivityResult(result.resultCode, result.data)
    }

    // Define the WebChromeClient (Optional - only if you need file chooser)
    webBridgeJSWebChromeClient = remember {
        WebBridgeJSWebChromeClient { intent ->
            fileChooserLauncher.launch(intent)
        }
    }

    // Create screen set request parameters.
    val params = mutableMapOf<String, Any>(
        "screenSet" to screenSet,
        "startScreen" to startScreen
    )

    // Build Uri.
    val screenSetUrl = ScreenSetUrlBuilder.Builder()
//        .apiKey(viewModel.identityService.getConfig().apiKey)
//        .domain(viewModel.identityService.getConfig().domain)
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
                    webChromeClient = webBridgeJSWebChromeClient
                }
            },
            update = { webView ->
                Log.d("ScreenSetView", "update")

                // Attach the web bridge to the web view element.
                webBridgeJS.attachBridgeTo(webView, viewModel.viewModelScope)

                // Set external authenticators. SDK will no longer use reflection to
                // retrieve external providers.
//                webBridgeJS.setNativeSocialProviders(
//                    viewModel.identityService.getAuthenticatorMap()
//                )

                // Register for JS events.
                webBridgeJS.registerForEvents { webBridgeJSEvent ->
                    val eventName = webBridgeJSEvent.name()
                    // Log event.
                    Log.d("ScreenSetView", "event: $eventName")
                    when (eventName) {
                        CANCELED -> {
                            screenSetError = "Operation canceled"
                        }

                        HIDE -> {
                            // Destroy the WebView instance.
                            webView.post { webView.destroy() }
                            NavigationCoordinator.INSTANCE.navigateUp()
                        }

                        LOGIN -> {
                            Log.d("ScreenSetView", "Login event received")
                            // Login flow Success.
//                            webView.post {
//                                NavigationCoordinator.INSTANCE.popToRootAndNavigate(
//                                    toRoute = ProfileScreenRoute.MyProfile.route,
//                                    rootRoute = ProfileScreenRoute.Welcome.route
//                                )
//                            }
                        }

                        LOGOUT -> {
                            // Navigate back to close the screen set view.
                            NavigationCoordinator.INSTANCE.navigateUp()
                        }


                    }
                }

                // Load URL.
                webBridgeJS.load(webView, screenSetUrl)
            },
            onRelease = {  webView ->
                Log.d("ScreenSetView", "onRelease")
                webBridgeJS.detachBridgeFrom(webView)
            }
        )

        // Screen-set error optional display.
        if (screenSetError.isNotEmpty()) {
            SimpleErrorMessages(screenSetError)
        }
    }
}