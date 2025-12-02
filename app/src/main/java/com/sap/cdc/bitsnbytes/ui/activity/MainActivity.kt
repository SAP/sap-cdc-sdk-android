package com.sap.cdc.bitsnbytes.ui.activity

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sap.cdc.bitsnbytes.BuildConfig
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.AppStateManager
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.view.screens.HomeScaffoldView
import kotlinx.coroutines.launch

/**
 * Main Activity with proper lifecycle management and MVVM architecture.
 *
 * Responsibilities:
 * - Conditional WebView debugging (debug builds only)
 * - Lifecycle-aware splash screen handling
 * - UI composition with Jetpack Compose
 * - Navigation coordinator setup
 *
 * Session event handling is delegated to MainActivityViewModel for proper MVVM architecture.
 */
class MainActivity : FragmentActivity() {

    // ViewModel integration for proper MVVM architecture
    // ViewModel handles all session events and business logic
    private val viewModel: MainActivityViewModel by viewModels()

    // AppStateManager for navigation
    private val appStateManager: AppStateManager by viewModels()

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

        // Connect NavigationCoordinator with AppStateManager
        NavigationCoordinator.INSTANCE.setAppStateManager(appStateManager)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colorScheme.background
                ) {
                    // Pass the activity-scoped AppStateManager
                    HomeScaffoldView(appStateManager = appStateManager)
                }
            }
        }
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
}
