@file:OptIn(ExperimentalMaterial3Api::class)

package com.sap.cdc.bitsnbytes.ui.view.flow

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.custom.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.custom.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.custom.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.custom.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.custom.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.custom.ViewDynamicSocialSelection
import com.sap.cdc.bitsnbytes.ui.viewmodel.ISignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModelPreview
import com.sap.cdc.bitsnbytes.R

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Sign in flows initiator selection view.
 */

@Composable
fun SignInView(viewModel: ISignInViewModel) {
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
        LargeVerticalSpacer()
        Text("Sign In", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text("Use your preferred method", style = AppTheme.typography.body)
        MediumVerticalSpacer()

        // Social selection view
        ViewDynamicSocialSelection(
            listOf("facebook", "google", "apple", "linkedIn")
        ) { provider ->
            viewModel.socialSignInWith(
                context as ComponentActivity,
                viewModel.getAuthenticationProvider(provider),
                onLogin = {
                    loading = false
                    signInError = ""
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = ProfileScreenRoute.MyProfile.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
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
        MediumVerticalSpacer()
        HorizontalDivider(
            modifier = Modifier.size(
                240.dp, 1.dp
            ), thickness = 1.dp, color = Color.LightGray
        )
        MediumVerticalSpacer()

        // Passwordless button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Passwordless",
            onClick = {
                viewModel.passkeySignIn(
                    activity = context as ComponentActivity,
                    onLogin = {
                        loading = false
                        signInError = ""
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            toRoute = ProfileScreenRoute.MyProfile.route,
                            rootRoute = ProfileScreenRoute.Welcome.route
                        )
                    },
                    onFailedWith = { error ->
                        loading = false
                        signInError = error?.errorDetails!!
                    }
                )
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
        SignInView(viewModel = SignInViewModelPreview())
    }
}