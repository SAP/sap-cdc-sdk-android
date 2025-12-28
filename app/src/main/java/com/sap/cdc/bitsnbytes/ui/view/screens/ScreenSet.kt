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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebChromeClient
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSWebViewClient
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.ScreenSetNavigationEvent
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Custom view for loading screen-sets.
 * This is a pure UI component that observes state from ScreenSetViewModel.
 * All business logic and WebView lifecycle management is handled by the ViewModel.
 */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScreenSetView(
    viewModel: ScreenSetViewModel,
    screenSet: String,
    startScreen: String
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    // Store WebView reference for proper disposal
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    // Initialize ScreenSet when composable is first launched
    LaunchedEffect(screenSet, startScreen) {
        Log.d("ScreenSetView", "Initializing ScreenSet: $screenSet - $startScreen")
        viewModel.initializeScreenSet(screenSet, startScreen)
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            // Log that we're processing a navigation event
            com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logNavigationEvent(
                source = "ScreenSetView",
                event = event
            )
            
            when (event) {
                is ScreenSetNavigationEvent.NavigateBack -> {
                    Log.d("ScreenSetView", "Navigation event: NavigateBack")
                    NavigationCoordinator.INSTANCE.navigateUp()
                }
                is ScreenSetNavigationEvent.NavigateToMyProfile -> {
                    Log.d("ScreenSetView", "Navigation event: NavigateToMyProfile")
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = ProfileScreenRoute.MyProfile.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
                }
                is ScreenSetNavigationEvent.NavigateToRoute -> {
                    Log.d("ScreenSetView", "Navigation event: NavigateToRoute(${event.route})")
                    NavigationCoordinator.INSTANCE.navigate(event.route)
                }
            }
        }
    }

    // File chooser launcher (optional - only if needed)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle file chooser result if needed
    }

    // WebChromeClient for file chooser support (optional)
    val webBridgeJSWebChromeClient = remember {
        WebBridgeJSWebChromeClient { intent ->
            fileChooserLauncher.launch(intent)
        }
    }

    // Get WebBridgeJS instance once
    val webBridgeJS = remember { viewModel.flowDelegate.getWebBridge() }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Only show WebView when URL is ready
        if (state.screenSetUrl != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { factoryContext ->
                    Log.d("ScreenSetView", "Creating WebView with key: ${state.webViewKey}")
                    
                    WebView(factoryContext).apply {
                        // Configure WebView settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        settings.allowFileAccess = true

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        // Set WebViewClient with loading callbacks
                        webViewClient = WebBridgeJSWebViewClient(
                            webBridge = webBridgeJS,
                            onBrowserIntent = { uri ->
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            onPageStarted = {
                                viewModel.onWebViewPageStarted()
                            },
                            onPageFinished = {
                                viewModel.onWebViewPageFinished()
                            }
                        )
                        
                        // Set WebChromeClient
                        webChromeClient = webBridgeJSWebChromeClient
                        
                        // Store reference for cleanup
                        webViewRef = this

                        // Setup WebBridge and load URL via ViewModel
                        viewModel.setupWebBridge(this)

                        Log.d("ScreenSetView", "WebView created and setup initiated")
                    }
                },
                update = { webView ->
                    // Update callback - ensure we have the latest reference
                    webViewRef = webView
                    Log.d("ScreenSetView", "WebView update callback - state.isInitialized: ${state.isInitialized}")
                }
            )
        }

        // Loading indicator
        if (state.isLoading) {
            IndeterminateLinearIndicator(true)
        }

        // Error message display
        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(text = error)
            }
        }
    }

    // Cleanup when composable is disposed
    DisposableEffect(state.webViewKey) {
        onDispose {
            Log.d("ScreenSetView", "Disposing ScreenSetView for key: ${state.webViewKey}")
            
            // Log WebView lifecycle event
            com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logWebViewLifecycle(
                source = "ScreenSetView",
                lifecycleEvent = "DisposableEffect onDispose",
                details = "webViewKey=${state.webViewKey}, disposing WebView"
            )
            
            // Dispose WebView on main thread to ensure thread safety
            webViewRef?.let { webView ->
                webView.post {
                    try {
                        viewModel.handleWebViewDisposal(webView)
                        webViewRef = null
                        Log.d("ScreenSetView", "WebView disposed successfully")
                    } catch (e: Exception) {
                        Log.e("ScreenSetView", "Error during WebView disposal", e)
                    }
                }
            }
        }
    }
}
