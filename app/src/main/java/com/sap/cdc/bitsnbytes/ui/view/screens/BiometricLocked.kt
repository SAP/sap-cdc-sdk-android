package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.BiometricLockedNavigationEvent
import com.sap.cdc.bitsnbytes.ui.view.composables.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer


@Composable
fun BiometricLockedView(viewModel: IBiometricLockedViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is BiometricLockedNavigationEvent.NavigateToRoute -> {
                    // Pop the BiometricLocked screen - this will either:
                    // 1. Reveal the previous screen if there's a backstack (normal case)
                    // 2. Do nothing if BiometricLocked is the only screen, so we need to navigate explicitly
                    val didNavigateUp = NavigationCoordinator.INSTANCE.navigateUp()
                    
                    // If navigateUp didn't work (no backstack), navigate to the target route
                    if (!didNavigateUp) {
                        NavigationCoordinator.INSTANCE.navigate(event.route) {
                            popUpTo(ProfileScreenRoute.BiometricLocked.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // UI elements.

        // Title & Subtitle
        LargeVerticalSpacer()
        Text("Session is locked", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text("Use biometric authentication to unlock", style = AppTheme.typography.body)
        MediumVerticalSpacer()

        // Unlock button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Unlock",
            onClick = { viewModel.onUnlockClick(context as ComponentActivity) },
            iconResourceId = R.drawable.ic_faceid,
        )

        MediumVerticalSpacer()

        // Error message
        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(text = error)
            }
        }
    }

    // Loading indicator on top of all views.
    Box(Modifier.fillMaxWidth()) {
        IndeterminateLinearIndicator(state.isLoading)
    }
}

@Preview
@Composable
fun BiometricLockedViewPreview() {
    AppTheme {
        BiometricLockedView(BiometricLockedViewModelPreview())
    }
}
