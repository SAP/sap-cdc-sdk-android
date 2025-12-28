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
import com.sap.cdc.bitsnbytes.ui.state.SignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.view.composables.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.ViewDynamicSocialSelection

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Sign in flows initiator selection view.
 */

@Composable
fun SignInView(viewModel: ISignInViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Handle navigation events using SharedFlow
    // LaunchedEffect with Unit key runs once when composable enters composition
    // and keeps collecting events without restarting on recompositions
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SignInNavigationEvent.NavigateToProfile -> {
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = event.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
                }
                is SignInNavigationEvent.NavigateToPendingRegistration -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.ResolvePendingRegistration.route}/${event.context}"
                    )
                }
                is SignInNavigationEvent.NavigateToLinkAccount -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.ResolveLinkAccount.route}/${event.context}"
                    )
                }
                is SignInNavigationEvent.NavigateToAuthTab -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.AuthTabView.route}/${event.tabIndex}"
                    )
                }
                is SignInNavigationEvent.NavigateToOTPSignIn -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.OTPSignIn.route}/${event.otpType}"
                    )
                }
                is SignInNavigationEvent.NavigateToCustomIdSignIn -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        ProfileScreenRoute.CustomIdSignIn.route
                    )
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
        Text("Sign In", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text("Use your preferred method", style = AppTheme.typography.body)
        MediumVerticalSpacer()

        // Social selection view
        ViewDynamicSocialSelection(
            listOf("facebook", "google", "apple", "linkedIn")
        ) { provider ->
            viewModel.socialSignInWith(
                hostActivity = context as ComponentActivity,
                provider = provider
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
                viewModel.passkeyLogin(context as ComponentActivity)
            },
            iconResourceId = R.drawable.ic_faceid,
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Email sign in button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Email",
            onClick = {
                viewModel.onEmailSignInClick()
            },
            iconResourceId = R.drawable.ic_email,
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Custom identifier sign in
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Custom ID",
            onClick = {
                viewModel.onCustomIdSignInClick()
            },
            iconResourceId = R.drawable.ic_profile_row,
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Phone sign in button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Sign in with Phone",
            onClick = {
                viewModel.onPhoneSignInClick()
            },
            iconResourceId = R.drawable.ic_device
        )
        Spacer(modifier = Modifier.size(10.dp))

        // Error message
        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(
                    text = error
                )
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
fun SignInViewPreview() {
    AppTheme {
        SignInView(SignInViewModelPreview())
    }
}
