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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sap.cdc.bitsnbytes.ui.view.composables.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer


/**
 * Created by Cline on 12/09/2025
 * Copyright: SAP LTD.
 *
 * Biometric locked session screen.
 */

@Composable
fun BiometricLockedView(viewModel: IBiometricLockedViewModel) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var unlockError by remember { mutableStateOf("") }

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
            onClick = {
                loading = true
                unlockError = ""
                viewModel.unlockWithBiometrics(context as ComponentActivity) {
                    onSuccess = {
                        loading = false
                        // Navigate to MyProfile and remove BiometricLocked from backstack
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            toRoute = ProfileScreenRoute.MyProfile.route,
                            rootRoute = ProfileScreenRoute.BiometricLocked.route
                        )
                    }
                    onError = { error ->
                        loading = false
                        unlockError = error.message
                    }
                }
            },
            iconResourceId = R.drawable.ic_faceid,
        )

        MediumVerticalSpacer()

        // Error message
        if (unlockError.isNotEmpty()) {
            SimpleErrorMessages(
                text = unlockError
            )
        }
    }

    // Loading indicator on top of all views.
    Box(Modifier.fillMaxWidth()) {
        IndeterminateLinearIndicator(loading)
    }
}

@Preview
@Composable
fun BiometricLockedViewPreview() {
    AppTheme {
        BiometricLockedView(BiometricLockedViewModelPreview())
    }
}
