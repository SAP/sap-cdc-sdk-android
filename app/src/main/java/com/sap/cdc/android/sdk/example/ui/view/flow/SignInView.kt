@file:OptIn(ExperimentalMaterial3Api::class)

package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme
import com.sap.cdc.android.sdk.example.ui.view.custom.IconAndTextOutlineButton
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.view.custom.LargeSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.MediumSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.SimpleErrorMessages
import com.sap.cdc.android.sdk.example.ui.view.custom.SmallSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.ViewDynamicSocialSelection
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Sign in flows initiator selection view.
 */

@Composable
fun SignInView(viewModel: IViewModelAuthentication) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // UI elements.

        // Title & Subtitle
        LargeSpacer()
        Text("Sign In", style = AppTheme.typography.titleLarge)
        SmallSpacer()
        Text("Use your preferred method",  style = AppTheme.typography.body)
        MediumSpacer()

        // Social selection view
        ViewDynamicSocialSelection(
            listOf("facebook", "google", "apple", "line")
        ) { provider ->
            viewModel.socialSignInWith(
                context as ComponentActivity,
                viewModel.getAuthenticationProvider(provider),
                onLogin = {
                    loading = false
                    signInError = ""
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = { error ->
                    loading = false
                    signInError = error?.errorDetails!!
                },
                onPendingRegistration = { authResponse ->
                    loading = false
                    NavigationCoordinator.INSTANCE
                        .navigate(
                            "${ProfileScreenRoute.ResolvePendingRegistration.route}/${
                                authResponse?.resolvable()?.toJson()
                            }"
                        )
                },
                onLoginIdentifierExists = { authResponse ->
                    loading = false
                    NavigationCoordinator.INSTANCE
                        .navigate(
                            "${ProfileScreenRoute.ResolveLinkAccount.route}/${
                                authResponse?.resolvable()?.toJson()
                            }"
                        )
                }
            )
        }

        // Divider
        MediumSpacer()
        HorizontalDivider(
            modifier = Modifier.size(
                240.dp, 1.dp
            ), thickness = 1.dp, color = Color.LightGray
        )
        MediumSpacer()

        // Passwordless button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Passwordless",
            onClick = {
                NavigationCoordinator.INSTANCE.navigate("${ProfileScreenRoute.AuthTabView.route}/1")
            },
            iconResourceId = R.drawable.ic_faceid,

            )
        Spacer(modifier = Modifier.size(10.dp))

        // Email sign in button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Email",
            onClick = {
//                Optional email otp flow.
//                NavigationCoordinator.INSTANCE.navigate(
//                    "${ProfileScreenRoute.OTPSignIn.route}/${OTPType.Email.value}"
//                )
                NavigationCoordinator.INSTANCE
                    .navigate("${ProfileScreenRoute.AuthTabView.route}/1")
            },
            iconResourceId = R.drawable.ic_email,
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Phone sign in button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Phone",
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(
                    "${ProfileScreenRoute.OTPSignIn.route}/${OTPType.PHONE.value}"
                )
            },
            iconResourceId = R.drawable.ic_device
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Error message
        if (signInError.isNotEmpty()) {
            SimpleErrorMessages(
                text = signInError
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
fun SignInViewPreview() {
    AppTheme {
        SignInView(viewModel = ViewModelAuthenticationPreview())
    }
}