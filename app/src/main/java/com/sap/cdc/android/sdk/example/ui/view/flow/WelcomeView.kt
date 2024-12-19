package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sap.cdc.android.sdk.example.ApplicationConfig
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.route.ScreenSetsRoute
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme
import com.sap.cdc.android.sdk.example.ui.view.custom.ActionOutlineButton
import com.sap.cdc.android.sdk.example.ui.view.custom.ActionTextButton
import com.sap.cdc.android.sdk.example.ui.view.custom.CustomSizeVerticalSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.view.custom.LargeVerticalSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.LoadingStateColumn
import com.sap.cdc.android.sdk.example.ui.view.custom.MediumVerticalSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.SimpleErrorMessages
import com.sap.cdc.android.sdk.example.ui.viewmodel.IWelcomeViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.WelcomeViewModelPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Welcome view.
 */

@Composable
fun WelcomeView(viewModel: IWelcomeViewModel) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(false) }
    var ssoError by remember { mutableStateOf("") }

    LoadingStateColumn(
        loading = loading,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        CustomSizeVerticalSpacer(140.dp)

        Text("Welcome!", style = AppTheme.typography.titleLarge)
        Text("Manage your profile", style = AppTheme.typography.body)

        LargeVerticalSpacer()

        ActionOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in",
        ) {
            if (ApplicationConfig.useWebViews) {
                NavigationCoordinator.INSTANCE
                    .navigate(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route)
            } else {
                NavigationCoordinator.INSTANCE
                    .navigate(ProfileScreenRoute.SignIn.route)
            }
        }

        MediumVerticalSpacer()

        ActionOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Register"
        ) {
            if (ApplicationConfig.useWebViews) {
                NavigationCoordinator.INSTANCE
                    .navigate(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route)
            } else {
                NavigationCoordinator.INSTANCE
                    .navigate(ProfileScreenRoute.Register.route)
            }
        }

        MediumVerticalSpacer()

        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp,
            modifier = Modifier.size(240.dp, 1.dp)
        )

        MediumVerticalSpacer()

        ActionTextButton(
            "Sign in with SSO"
        ) {
            loading = true
            viewModel.singleSignOn(
                context as ComponentActivity,
                mutableMapOf(),
                onLogin = {
                    loading = false
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = { error ->
                    loading = false
                    ssoError = error?.errorDescription!!
                }
            )
        }

        LargeVerticalSpacer()

        // Error message
        if (ssoError.isNotEmpty()) {
            SimpleErrorMessages(
                text = ssoError
            )
        }
    }
}

@Preview
@Composable
fun WelcomeViewPreview() {
    AppTheme {
        WelcomeView(WelcomeViewModelPreview())
    }
}
