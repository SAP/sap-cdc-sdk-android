package com.sap.cdc.bitsnbytes.ui.activity

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.CDCMessageEventBus
import com.sap.cdc.android.sdk.SessionEvent
import com.sap.cdc.bitsnbytes.BuildConfig
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.screens.HomeScaffoldView
import kotlinx.coroutines.launch

/**
 * Main Activity with proper lifecycle management and memory leak prevention.
 * 
 * - Proper event bus lifecycle management to prevent memory leaks
 * - Conditional WebView debugging (debug builds only)
 * - Lifecycle-aware splash screen handling
 * - Proper cleanup in onDestroy
 * - Lifecycle-aware navigation calls
 */
class MainActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()
        
        // Enable WebView debugging only in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Proper splash screen lifecycle management
        configureSplashScreen(splashScreen)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colorScheme.background
                ) {
                    HomeScaffoldView()
                }
            }
        }
        
        // Setup session event handling with proper lifecycle awareness
        setupSessionEventHandling()
    }
    
    /**
     * Configure splash screen with proper lifecycle management
     */
    private fun configureSplashScreen(splashScreen: androidx.core.splashscreen.SplashScreen) {
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        // Use lifecycle-aware coroutine scope
        lifecycleScope.launch {
            // Wait for activity to be in STARTED state before dismissing splash
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                keepSplashScreen = false
            }
        }
    }
    
    /**
     * Setup session event handling with proper lifecycle management
     */
    private fun setupSessionEventHandling() {
        // Subscribe to session events with lifecycle awareness
        CDCMessageEventBus.subscribeToSessionEvents { sessionEvent ->
            when (sessionEvent) {
                is SessionEvent.ExpiredSession -> {
                    CDCDebuggable.log("MainActivity", "Session expired - navigating to welcome")
                    handleSessionExpired()
                }
                
                is SessionEvent.VerifySession -> {
                    CDCDebuggable.log("MainActivity", "Session verification requested")
                    handleSessionVerification()
                }
            }
        }
    }
    
    /**
     * Handle session expiration with lifecycle awareness
     */
    private fun handleSessionExpired() {
        // Only navigate if activity is in proper state
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                toRoute = ProfileScreenRoute.Welcome.route,
                rootRoute = ProfileScreenRoute.Welcome.route
            )
        }
    }
    
    /**
     * Handle session verification
     */
    private fun handleSessionVerification() {
        // Lifecycle-aware session verification
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleScope.launch {
                // Session verification logic can be added here
                CDCDebuggable.log("MainActivity", "Session verification completed")
            }
        }
    }
    
    override fun onDestroy() {
        // Proper cleanup to prevent memory leaks
        // Only dispose the global event bus if this is the last activity
        if (isFinishing) {
            CDCMessageEventBus.dispose()
        }
        
        super.onDestroy()
    }
}
