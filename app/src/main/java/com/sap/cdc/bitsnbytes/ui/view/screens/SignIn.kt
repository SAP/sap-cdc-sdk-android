package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.sap.cdc.bitsnbytes.ui.view.composables.ViewDynamicSocialSelection
import com.sap.cdc.bitsnbytes.ui.viewmodel.ISignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModelPreview

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
                provider,
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
                viewModel.passkeyLogin(context as ComponentActivity) {
                    onSuccess = {
                        loading = false
                        signInError = ""
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            toRoute = ProfileScreenRoute.MyProfile.route,
                            rootRoute = ProfileScreenRoute.Welcome.route
                        )
                    }

                    onError = { error ->
                        loading = false
                        signInError = error.message
                    }
                }
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

        // Custom identifier sign in
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Custom ID",
            onClick = {
                NavigationCoordinator.INSTANCE
                    .navigate(ProfileScreenRoute.CustomIdSignIn.route)
            },
            iconResourceId = R.drawable.ic_profile_row,
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
        SignInView(SignInViewModelPreview())
    }
}