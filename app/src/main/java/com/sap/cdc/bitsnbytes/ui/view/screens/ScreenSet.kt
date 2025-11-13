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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebChromeClient
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebViewClient
import com.sap.cdc.android.sdk.feature.screensets.disposeWebViewImmediately
import com.sap.cdc.android.sdk.feature.screensets.onScreenSetEvents
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import java.util.UUID

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
    screenSet: String, startScreen: String
) {
    val context = LocalContext.current

    var screenSetError by remember { mutableStateOf("") }

    // Use a unique key that changes each time we enter the composable
    // This forces AndroidView to recreate the WebView completely
    val webViewKey = remember { UUID.randomUUID().toString() }

    // Track if the WebView has been initialized to prevent multiple loads
    var isInitialized by remember { mutableStateOf(false) }

    // Store WebView reference for proper cleanup
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
    val params = remember(screenSet, startScreen) {
        mutableMapOf<String, Any>(
            "screenSet" to screenSet,
            "startScreen" to startScreen
        )
    }

    // Build Uri.
    val screenSetUrl = remember(
        screenSet,
        startScreen,
        viewModel.flowDelegate.siteConfig.apiKey,
        viewModel.flowDelegate.siteConfig.domain
    ) {
        ScreenSetUrlBuilder.Builder()
            .apiKey(viewModel.flowDelegate.siteConfig.apiKey)
            .domain(viewModel.flowDelegate.siteConfig.domain)
            .params(params)
            .build()
    }

    // Set native social provider authenticators.
    val webBridgeJS: WebBridgeJS = remember { viewModel.newWebBridgeJS() }

    // Add specific web bridge configurations.
    LaunchedEffect(webBridgeJS) {
        webBridgeJS.addConfig(
            WebBridgeJSConfig.Builder().obfuscate(true).build()
        )
    }

    Box {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Log.d("ScreenSetView", "factory - creating new WebView with key: $webViewKey")
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    settings.allowFileAccess = true

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    webViewClient = WebBridgeJSWebViewClient(webBridgeJS) { browserUri ->
                        val intent = Intent(Intent.ACTION_VIEW, browserUri)
                        context.startActivity(intent)
                    }

                    webChromeClient = webBridgeJSWebChromeClient

                    // Store WebView reference for cleanup
                    webViewRef = this

                    // Initialize the WebView immediately
                    initializeWebView(this, webBridgeJS, viewModel, screenSetUrl) { error ->
                        screenSetError = error
                    }
                }
            },
            update = { webView ->
                Log.d("ScreenSetView", "update - webViewKey: $webViewKey")

                // Only initialize once per WebView instance
                if (!isInitialized) {
                    initializeWebView(webView, webBridgeJS, viewModel, screenSetUrl) { error ->
                        screenSetError = error
                    }
                    isInitialized = true
                }
            }
        )

        // Screen-set error optional display.
        if (screenSetError.isNotEmpty()) {
            SimpleErrorMessages(screenSetError)
        }
    }

    // Cleanup when the composable is disposed
    DisposableEffect(webViewKey) {
        onDispose {
            Log.d("ScreenSetView", "Properly disposing WebBridgeJS and WebView for key: $webViewKey")
            webViewRef?.let { webView ->
                try {
                    webBridgeJS.detachBridgeFrom(webView)
                    Log.d("ScreenSetView", "Successfully detached WebBridgeJS from WebView")
                } catch (e: Exception) {
                    Log.e("ScreenSetView", "Error during WebBridgeJS cleanup", e)
                }
            }
            webViewRef = null
        }
    }
}

private fun initializeWebView(
    webView: WebView,
    webBridgeJS: WebBridgeJS,
    viewModel: ScreenSetViewModel,
    screenSetUrl: String,
    onError: (String) -> Unit
) {
    try {
        Log.d("ScreenSetView", "initializeWebView - starting initialization")

        // Attach the web bridge to the web view element.
        webBridgeJS.attachBridgeTo(webView, viewModel.viewModelScope)

        webBridgeJS.onScreenSetEvents {
            onLoad = {
                Log.d("ScreenSetView", "Screen set loaded: ${it.screenSetId}")
            }
            onHide = {
                webView.post {
                    Log.d("ScreenSetView", "HIDE event - navigating back")
                    // Dispose WebView immediately before navigation
                    webBridgeJS.disposeWebViewImmediately(webView)
                    NavigationCoordinator.INSTANCE.navigateUp()
                }
            }
            onCanceled = {
                Log.d("ScreenSetView", "Screen set canceled")
                webView.post {
                    onError("Operation canceled")
                    // Dispose WebView immediately before navigation
                    webBridgeJS.disposeWebViewImmediately(webView)
                    NavigationCoordinator.INSTANCE.navigateUp()
                }
            }

            onLogin = {
                webView.post {
                    Log.d("ScreenSetView", "Login event received")
                    // Dispose WebView immediately before navigation
                    webBridgeJS.disposeWebViewImmediately(webView)
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = ProfileScreenRoute.MyProfile.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
                }
            }

            onLogout = {
                webView.post {
                    Log.d("ScreenSetView", "LOGOUT event - navigating back")
                    // Dispose WebView immediately before navigation
                    webBridgeJS.disposeWebViewImmediately(webView)
                    NavigationCoordinator.INSTANCE.navigateUp()
                }
            }
        }

        // Load the screen set URL
        webBridgeJS.load(webView, screenSetUrl)

        Log.d("ScreenSetView", "initializeWebView - initialization completed")

    } catch (e: Exception) {
        Log.e("ScreenSetView", "Error during WebView initialization", e)
        onError("Failed to initialize WebView: ${e.message}")
    }
}
