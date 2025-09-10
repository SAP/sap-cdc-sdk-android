@file:OptIn(ExperimentalPermissionsApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineInverseButton
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeHorizontalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer


/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

@SuppressLint("InlinedApi")
@Composable
fun LoginOptionsView(viewModel: ILoginOptionsViewModel) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(false) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var optionsError: String? by remember { mutableStateOf("") }

    val view = LocalView.current
    val notificationPermission = if (view.isInEditMode) null
    else rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val isGranted = notificationPermission?.status?.isGranted

    // UI elements.

    LoadingStateColumn(
        loading = loading,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // Option cards
        OptionCard(
            title = "Passwordless Login",
            status = if (viewModel.isPasswordlessLoginActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isPasswordlessLoginActive()) "Deactivate" else "Activate",
            onClick = {
                viewModel.togglePasswordlessLogin()
            },
            inverse = !viewModel.isPasswordlessLoginActive()
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Push Authentication",
            status = if (viewModel.isPushAuthenticationActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isPushAuthenticationActive()) "Deactivate" else "Activate",
            onClick = {
                viewModel.togglePushAuthentication()
            },
            inverse = !viewModel.isPushAuthenticationActive()
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Push 2-Factor Authentication",
            status = if (viewModel.isPushTwoFactorAuthActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isPushTwoFactorAuthActive()) "Deactivate" else "Activate",
            onClick = {
                viewModel.togglePushTwoFactorAuth()
            },
            inverse = !viewModel.isPushTwoFactorAuthActive()
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Biometrics", 
            status = if (viewModel.isBiometricActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isBiometricActive()) "Deactivate" else "Activate",
            onClick = {
                viewModel.toggleBiometricAuthentication()
            }, 
            inverse = !viewModel.isBiometricActive()
        )

        // Biometrics lock toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Lock biometrics:")
            LargeHorizontalSpacer()
            Switch(checked = viewModel.isBiometricLocked(), onCheckedChange = { checked ->
                when (checked) {
                    true -> viewModel.biometricLock()
                    false -> {
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setAllowedAuthenticators(BIOMETRIC_STRONG)
                            .setTitle("Biometric Authentication")
                            .setSubtitle("Authenticate using your biometric credential")
                            .setNegativeButtonText("Use another method").build()

                        viewModel.biometricUnlock(
                            activity = context as FragmentActivity,
                            promptInfo = promptInfo,
                            executor = executor
                        )
                    }
                }
            })
        }

        LargeVerticalSpacer()

        // Error message
        if (optionsError?.isNotEmpty() ?: false) {
            SimpleErrorMessages(
                text = optionsError!!
            )
        }
    }
}

@Preview
@Composable
fun LoginOptionsViewPreview() {
    AppTheme {
        LoginOptionsView(LoginOptionsViewModelPreview())
    }
}

@Composable
private fun OptionCard(
    title: String, status: String, actionLabel: String, inverse: Boolean, onClick: () -> Unit
) {
    Card(
        //elevation = 4.dp,
        shape = RoundedCornerShape(0),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = AppTheme.typography.titleSmall)
                SmallVerticalSpacer()
                Text(text = status, style = AppTheme.typography.body)
            }
            if (inverse) {
                ActionOutlineInverseButton(
                    text = actionLabel,
                    onClick = onClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 120.dp),
                    fillMaxWidth = false
                )
            } else {
                ActionOutlineButton(
                    text = actionLabel,
                    onClick = onClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 120.dp),
                    fillMaxWidth = false
                )
            }
        }
    }
}
