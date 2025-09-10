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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.android.sdk.feature.TwoFactorInitiator
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
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
    var loading by remember { mutableStateOf(false) }
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var signInError by remember { mutableStateOf("") }
    var passwordVisible: Boolean by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    var captchaRequired by remember { mutableStateOf(false) }
    var isSwitchChecked by remember { mutableStateOf(false) }

    //UI elements
    LoadingStateColumn(
        loading
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
                    autofillTypes = listOf(AutofillType.EmailAddress),
                    onFill = {
                        email = it
                    }
                )

            OutlineTitleAndEditTextField(
                modifier = Modifier
                    .connectNode(handler = autoFillHandler)
                    .defaultFocusChangeAutoFill(handler = autoFillHandler),
                titleText = "Email: *",
                inputText = email,
                placeholderText = "Email placeholder",
                onValueChange = {
                    email = it
                },
                focusManager = focusManager
            )

            // Password input.
            SmallVerticalSpacer()
            OutlineTitleAndEditPasswordTextField(
                titleText = "Password: *",
                inputText = password,
                placeholderText = "Password placeholder",
                passwordVisible = passwordVisible,
                onValueChange = {
                    password = it
                },
                onEyeClick = { passwordVisible = it },
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
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Login",
                onClick = {
                    loading = true
                    viewModel.login(
                        Credentials(loginId = email, password = password)
                    ) {
                        onSuccess = {
                            signInError = ""
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        }
                        onError = { error ->
                            loading = false
                            signInError = error.message
                        }
                        onLinkingRequired = {
                            loading = false
                        }
                        onTwoFactorRequired = { twoFactorContext ->
                            loading = false
                            signInError = ""
                            when (twoFactorContext.initiator) {
                                TwoFactorInitiator.REGISTRATION -> {
                                    NavigationCoordinator.INSTANCE
                                        .navigate(
                                            "${ProfileScreenRoute.AuthMethods.route}/${
                                                twoFactorContext.toJson()
                                            }"
                                        )
                                }

                                TwoFactorInitiator.VERIFICATION -> {
                                    NavigationCoordinator.INSTANCE
                                        .navigate(
                                            "${ProfileScreenRoute.AuthMethods.route}/${
                                                twoFactorContext.toJson()
                                            }"
                                        )
                                }

                                null -> { /* no-op */
                                }
                            }
                        }
                        onCaptchaRequired = { ->
                            loading = false
                            captchaRequired = true
                            signInError = "Captcha required"
                        }
                        onPendingRegistration = { registrationContext ->
                            loading = false
                            NavigationCoordinator.INSTANCE
                                .navigate(
                                    "${ProfileScreenRoute.ResolvePendingRegistration.route}/${
                                        registrationContext.toJson()
                                    }"
                                )
                        }
                    }
                }
            )

            if (captchaRequired) {
                SmallVerticalSpacer()
                androidx.compose.material3.Switch(
                    checked = isSwitchChecked,
                    onCheckedChange = { isChecked ->
                        isSwitchChecked = isChecked
                        if (isChecked) {
                            viewModel.getSaptchaToken {
                                onSuccess = {
                                    loading = false
                                }
                                onError = { error ->
                                    loading = false
                                    signInError = error.message
                                }
                            }
                        }
                    }
                )
            }

            if (signInError.isNotEmpty()) {
                SimpleErrorMessages(
                    text = signInError
                )
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