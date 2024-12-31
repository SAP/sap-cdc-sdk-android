package com.sap.cdc.bitsnbytes.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.CDCMessageEventBus
import com.sap.cdc.android.sdk.SessionEvent
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.flow.HomeScaffoldView
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Main application activity class.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            keepSplashScreen = false
        }
        enableEdgeToEdge()
        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colorScheme.background
                ) {
                    HomeScaffoldView()
                }
            }
        }

        CDCMessageEventBus.subscribeToSessionEvents {
            when (it) {
                is SessionEvent.ExpiredSession -> {
                    CDCDebuggable.log("MainActivity", "Invalidate session event received from bus.")
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = ProfileScreenRoute.Welcome.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
                }

                is SessionEvent.VerifySession -> {
                    // Verify session
                }
            }
        }
    }
}







