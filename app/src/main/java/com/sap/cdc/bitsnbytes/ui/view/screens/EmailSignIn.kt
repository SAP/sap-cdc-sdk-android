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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
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
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailSignInViewModelPreview
import com.sap.cdc.bitsnbytes.ui.viewmodel.IEmailSignInViewModel


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
                autoFillRequestHandler(autofillTypes = listOf(AutofillType.EmailAddress),
                    onFill = {
                        email = it
                    }
                )

            OutlineTitleAndEditTextField(
                modifier = Modifier.connectNode(handler = autoFillHandler)
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
                        email = email, password = password,
                        onLogin = {
                            signInError = ""
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = { error ->
                            loading = false
                            signInError = error?.errorDescription!!
                        },
                        onLoginIdentifierExists = {
                            loading = false
                        }
                    )
                }
            )

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