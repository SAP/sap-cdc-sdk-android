package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.OutlineTitleAndEditPasswordTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.OutlineTitleAndEditTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.PasswordNotMatchingError
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Credentials registration basic view (email & password).
 */

@Composable
fun EmailRegisterView(viewModel: IEmailRegistrationViewModel) {
    val context = LocalContext.current
    
    // Use ViewModel state instead of local remember state
    // This ensures field values persist across navigation
    val isNotMatching = remember {
        derivedStateOf {
            viewModel.password != viewModel.confirmPassword
        }
    }
    val focusManager = LocalFocusManager.current

    // UI elements.

    LoadingStateColumn(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(48.dp),
        loading = viewModel.loading
    ) {

        LargeVerticalSpacer()
        Text("Create your account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        SmallVerticalSpacer()
        Text("Please fill out the listed inputs", fontSize = 16.sp, fontWeight = FontWeight.Light)
        MediumVerticalSpacer()


        // Name Input.
        SmallVerticalSpacer()
        OutlineTitleAndEditTextField(
            modifier = Modifier,
            titleText = "Name: *",
            inputText = viewModel.name,
            placeholderText = "Name placeholder",
            onValueChange = {
                viewModel.name = it
            },
            focusManager = focusManager
        )

        // Email input.
        SmallVerticalSpacer()
        OutlineTitleAndEditTextField(
            modifier = Modifier,
            titleText = "Email: *",
            inputText = viewModel.email,
            placeholderText = "Email placeholder",
            onValueChange = {
                viewModel.email = it
            },
            focusManager = focusManager
        )

        // Password input.
        SmallVerticalSpacer()
        OutlineTitleAndEditPasswordTextField(
            titleText = "Password: *",
            inputText = viewModel.password,
            placeholderText = "",
            passwordVisible = viewModel.passwordVisible,
            onValueChange = {
                viewModel.password = it
            },
            onEyeClick = { viewModel.passwordVisible = it },
            focusManager = focusManager
        )

        // Confirm password input.
        SmallVerticalSpacer()
        OutlineTitleAndEditPasswordTextField(
            titleText = "Confirm password: *",
            inputText = viewModel.confirmPassword,
            placeholderText = "",
            passwordVisible = viewModel.passwordVisible,
            onValueChange = {
                viewModel.confirmPassword = it
            },
            onEyeClick = { viewModel.passwordVisible = it },
            focusManager = focusManager
        )

        if (isNotMatching.value) {
            PasswordNotMatchingError()
        }

        MediumVerticalSpacer()
        Text(
            "By clicking on \"register\", you conform that you have read and agree to the privacy policy and terms of use.",
        )

        MediumVerticalSpacer()

        ActionOutlineButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = "Register",
            onClick = {
                viewModel.registerError = ""
                viewModel.loading = true
                // Credentials registration.
                viewModel.register(
                    Credentials(email = viewModel.email, password = viewModel.password),
                    name = viewModel.name,
                ) {
                    onSuccess = {
                        viewModel.loading = false
                        viewModel.registerError = ""
                        NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                    }
                    onError = { error ->
                        viewModel.loading = false
                        viewModel.registerError = error.message
                    }
                    onTwoFactorRequired = { twoFactorContext ->
                        viewModel.loading = false
                        viewModel.registerError = ""
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
                    onPendingRegistration = { registrationContext ->
                        viewModel.loading = false
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

        if (viewModel.registerError.isNotEmpty()) {
            SimpleErrorMessages(
                text = viewModel.registerError
            )
        }
    }

}


@Preview
@Composable
fun EmailRegisterViewPreview() {
    AppTheme {
        EmailRegisterView(EmailRegistrationViewModelPreview())
    }
}
