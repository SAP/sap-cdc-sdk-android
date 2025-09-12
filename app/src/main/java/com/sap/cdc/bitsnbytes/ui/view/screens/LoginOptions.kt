@file:OptIn(ExperimentalPermissionsApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.sap.cdc.bitsnbytes.ui.view.composables.SuccessBanner
import kotlinx.coroutines.delay


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
    var showBanner by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf("") }

    val view = LocalView.current
    val notificationPermission = if (view.isInEditMode) null
    else rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val isGranted = notificationPermission?.status?.isGranted

    // Load passkeys when the view is first loaded
    LaunchedEffect(Unit) {
        viewModel.loadPasskeys()
    }
    
    // UI elements.

    if (showBanner) {
        SuccessBanner(
            message = bannerText,
            onDismiss = { showBanner = false }
        )
    }

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
            status = when {
                viewModel.isLoadingPasskeys -> "Loading..."
                viewModel.isPasswordlessLoginActive() -> "Activated"
                else -> "Deactivated"
            },
            actionLabel = when {
                viewModel.isLoadingPasskeys -> "Loading..."
                viewModel.isPasswordlessLoginActive() -> "Deactivate"
                else -> "Activate"
            },
            onClick = {
                if (!viewModel.isLoadingPasskeys) {
                    if (viewModel.isPasswordlessLoginActive()) {
                        // Should start revoke flow
                        //TODO: Transition to a new view displaying the credentials list.
                    } else {
                        loading = true
                        viewModel.createPasskey(
                            activity = context as ComponentActivity
                        ) {
                            onSuccess = {
                                loading = false
                                bannerText = "Passkey added"
                                showBanner = true
                            }

                            onError = { error ->
                                loading = false
                                optionsError = error.message
                            }
                        }
                    }
                }
            },
            inverse = !viewModel.isPasswordlessLoginActive(),
            isEnabled = !viewModel.isLoadingPasskeys
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Push Authentication",
            status
            = if (viewModel.isPushAuthenticationActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isPushAuthenticationActive()) "Deactivate" else "Activate",
            onClick = {
                loading = false
                viewModel.optOnForAuthenticationNotifications {
                    onSuccess = {
                        loading = false
                        viewModel.togglePushAuthentication()
                    }

                    onError = { error ->
                        loading = false
                        optionsError = error.message
                    }
                }
            },
            inverse = !viewModel.isPushAuthenticationActive()
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Push 2-Factor Authentication",
            status = if (viewModel.isPushTwoFactorAuthActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isPushTwoFactorAuthActive()) "Deactivate" else "Activate",
            onClick = {
                loading = true
                viewModel.optInForTwoFactorNotifications {
                    onSuccess = {
                        loading = false
                        viewModel.togglePushTwoFactorAuth()
                    }

                    onError = { error ->
                        loading = false
                        optionsError = error.message
                    }
                }
            },
            inverse = !viewModel.isPushTwoFactorAuthActive()
        )
        SmallVerticalSpacer()
        OptionCard(
            title = "Biometrics",
            status = if (viewModel.isBiometricActive()) "Activated" else "Deactivated",
            actionLabel = if (viewModel.isBiometricActive()) "Deactivate" else "Activate",
            onClick = {
                if (!viewModel.isBiometricActive()) {
                    viewModel.biometricOptIn(
                        activity = context as FragmentActivity,
                        promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setAllowedAuthenticators(BIOMETRIC_STRONG)
                            .setTitle("Opt in for biometric authentication")
                            .setSubtitle("Authenticate using your biometric credential")
                            .setNegativeButtonText("Use another method").build(),
                        executor = executor,
                    ) {
                        onSuccess = {
                            viewModel.toggleBiometricAuthentication()
                        }
                        onError = { error ->
                            optionsError = error.message
                        }
                    }
                } else  {
                    viewModel.biometricOptOut(
                        activity = context as FragmentActivity,
                        promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setAllowedAuthenticators(BIOMETRIC_STRONG)
                            .setTitle("Opt out of biometric authentication")
                            .setSubtitle("Authenticate using your biometric credential")
                            .setNegativeButtonText("Use another method").build(),
                        executor = executor,
                    ) {
                        onSuccess = {
                            viewModel.toggleBiometricAuthentication()
                        }
                        onError = { error ->
                            optionsError = error.message
                        }
                    }
                }
            },
            inverse = !viewModel.isBiometricActive()
        )

        // Biometrics lock toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .alpha(if (viewModel.isBiometricActive()) 1f else 0.5f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Lock biometrics:")
            LargeHorizontalSpacer()
            Switch(
                checked = viewModel.isBiometricLocked(),
                enabled = viewModel.isBiometricActive(),
                onCheckedChange = { checked ->
                    if (viewModel.isBiometricActive()) {
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
                                ) {
                                    onSuccess = {
                                        // Successfully unlocked, now disable the lock

                                    }
                                    onError = { error ->
                                        optionsError = error.message
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        LargeVerticalSpacer()

        // Error message
        if (optionsError?.isNotEmpty() ?: false) {
            SimpleErrorMessages(
                text = optionsError!!
            )
        }

        LargeVerticalSpacer()

        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SuccessBanner(
                message = "Account updated successfully",
                onDismiss = { showBanner = false }
            )
        }

        // Auto-hide after 2 seconds
        if (showBanner) {
            LaunchedEffect(Unit) {
                delay(2000)
                showBanner = false
            }
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
    title: String, 
    status: String, 
    actionLabel: String, 
    inverse: Boolean, 
    onClick: () -> Unit,
    isEnabled: Boolean = true
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
                    onClick = if (isEnabled) onClick else {
                        ->
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp, minWidth = 120.dp)
                        .alpha(if (isEnabled) 1f else 0.5f),
                    fillMaxWidth = false
                )
            } else {
                ActionOutlineButton(
                    text = actionLabel,
                    onClick = if (isEnabled) onClick else {
                        ->
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp, minWidth = 120.dp)
                        .alpha(if (isEnabled) 1f else 0.5f),
                    fillMaxWidth = false
                )
            }
        }
    }
}
