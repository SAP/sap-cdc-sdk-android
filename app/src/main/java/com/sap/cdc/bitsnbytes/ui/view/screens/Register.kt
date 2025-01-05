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
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.ViewDynamicSocialSelection
import com.sap.cdc.bitsnbytes.ui.viewmodel.IRegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModelPreview

@Composable
fun RegisterView(viewModel: IRegisterViewModel) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var registerError by remember { mutableStateOf("") }

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
        Text("Register", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text("Use your preferred method", style = AppTheme.typography.body)
        MediumVerticalSpacer()

        // Social selection view
        ViewDynamicSocialSelection(
            listOf("facebook", "google", "apple", "line")
        ) { provider ->
            viewModel.socialSignInWith(
                context as ComponentActivity,
                viewModel.getAuthenticationProvider(provider),
                onLogin = {
                    loading = false
                    registerError = ""
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = { error ->
                    loading = false
                    registerError = error?.errorDetails!!
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

        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Register with Email",
            iconResourceId = R.drawable.ic_email,
        ) {
            NavigationCoordinator.INSTANCE
                .navigate("${ProfileScreenRoute.AuthTabView.route}/0")
        }
        Spacer(modifier = Modifier.size(10.dp))
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Register with Phone",
            iconResourceId = R.drawable.ic_device,
        ) {
            NavigationCoordinator.INSTANCE.navigate(
                "${ProfileScreenRoute.OTPSignIn.route}/${OTPType.PHONE.value}"
            )
        }

        LargeVerticalSpacer()

        // Error message
        if (registerError.isNotEmpty()) {
            SimpleErrorMessages(
                text = registerError
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
fun RegisterViewPreview() {
    AppTheme {
        RegisterView(RegisterViewModelPreview())
    }
}