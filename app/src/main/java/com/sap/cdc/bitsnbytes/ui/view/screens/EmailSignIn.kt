@file:OptIn(ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.EmailSignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.OutlineTitleAndEditPasswordTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.OutlineTitleAndEditTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer


/**
 * Created by Tal Mirmelshtein on 19/06/2024
 * Copyright: SAP LTD.
 *
 * Credentials sign in basic view.
 */

@Composable
fun EmailSignInView(viewModel: IEmailSignInViewModel) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var isSwitchChecked by remember { mutableStateOf(false) }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is EmailSignInNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                }
                is EmailSignInNavigationEvent.NavigateToAuthMethods -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.AuthMethods.route}/${event.twoFactorContextJson}"
                    )
                }
                is EmailSignInNavigationEvent.NavigateToPendingRegistration -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.ResolvePendingRegistration.route}/${event.registrationContextJson}"
                    )
                }
            }
        }
    }

    //UI elements
    LoadingStateColumn(
        state.isLoading
    ) {

        LargeVerticalSpacer()
        Text("Sign In with Email", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text(
            "Please enter your email and password",
            style = AppTheme.typography.body
        )
        MediumVerticalSpacer()

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SmallVerticalSpacer()
            val autoFillHandler =
                autoFillRequestHandler(
                    contentTypes = listOf(ContentType.EmailAddress),
                    onFill = { viewModel.onEmailChanged(it) }
                )

            OutlineTitleAndEditTextField(
                modifier = Modifier
                    .connectNode(handler = autoFillHandler)
                    .defaultFocusChangeAutoFill(handler = autoFillHandler),
                titleText = "Email: *",
                inputText = state.email,
                placeholderText = "Email placeholder",
                onValueChange = { viewModel.onEmailChanged(it) },
                focusManager = focusManager
            )

            // Password input.
            SmallVerticalSpacer()
            OutlineTitleAndEditPasswordTextField(
                titleText = "Password: *",
                inputText = state.password,
                placeholderText = "Password placeholder",
                passwordVisible = state.passwordVisible,
                onValueChange = { viewModel.onPasswordChanged(it) },
                onEyeClick = { viewModel.onPasswordVisibilityToggled() },
                focusManager = focusManager
            )


            MediumVerticalSpacer()
            Box(modifier = Modifier.align(Alignment.End)) {
                Surface(onClick = {
                    // Forgot password route.
                }) {
                    Text("Forgot password?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            SmallVerticalSpacer()

            ActionOutlineButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Login",
                onClick = { viewModel.onLoginClick() }
            )

            if (state.captchaRequired) {
                SmallVerticalSpacer()
                androidx.compose.material3.Switch(
                    checked = isSwitchChecked,
                    onCheckedChange = { isChecked ->
                        isSwitchChecked = isChecked
                        if (isChecked) {
                            viewModel.onGetCaptchaToken()
                        }
                    }
                )
            }

            state.error?.let { error ->
                if (error.isNotEmpty()) {
                    SimpleErrorMessages(text = error)
                }
            }
        }
    }
}


@Preview
@Composable
fun EmailSignInViewPreview() {
    AppTheme {
        EmailSignInView(EmailSignInViewModelPreview())
    }
}
