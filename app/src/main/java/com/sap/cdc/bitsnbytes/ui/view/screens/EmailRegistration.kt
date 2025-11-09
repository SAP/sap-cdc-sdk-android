package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.EmailRegistrationNavigationEvent
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
    val state by viewModel.state.collectAsState()
    
    val isNotMatching = remember {
        derivedStateOf {
            state.password != state.confirmPassword
        }
    }
    val focusManager = LocalFocusManager.current

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is EmailRegistrationNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                }
                is EmailRegistrationNavigationEvent.NavigateToAuthMethods -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.AuthMethods.route}/${event.twoFactorContextJson}"
                    )
                }
                is EmailRegistrationNavigationEvent.NavigateToPendingRegistration -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.ResolvePendingRegistration.route}/${event.registrationContextJson}"
                    )
                }
            }
        }
    }

    // UI elements.

    LoadingStateColumn(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(48.dp),
        loading = state.isLoading
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
            inputText = state.name,
            placeholderText = "Name placeholder",
            onValueChange = { viewModel.onNameChanged(it) },
            focusManager = focusManager
        )

        // Email input.
        SmallVerticalSpacer()
        OutlineTitleAndEditTextField(
            modifier = Modifier,
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
            placeholderText = "",
            passwordVisible = state.passwordVisible,
            onValueChange = { viewModel.onPasswordChanged(it) },
            onEyeClick = { viewModel.onPasswordVisibilityToggled() },
            focusManager = focusManager
        )

        // Confirm password input.
        SmallVerticalSpacer()
        OutlineTitleAndEditPasswordTextField(
            titleText = "Confirm password: *",
            inputText = state.confirmPassword,
            placeholderText = "",
            passwordVisible = state.passwordVisible,
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            onEyeClick = { viewModel.onPasswordVisibilityToggled() },
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
            modifier = Modifier.fillMaxWidth(),
            text = "Register",
            onClick = { viewModel.onRegisterClick() }
        )

        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(text = error)
            }
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
