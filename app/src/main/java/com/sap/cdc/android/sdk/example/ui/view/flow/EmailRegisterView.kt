package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme
import com.sap.cdc.android.sdk.example.ui.view.custom.ActionOutlineButton
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.view.custom.LargeSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.MediumSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.OutlineTitleAndEditPasswordTextField
import com.sap.cdc.android.sdk.example.ui.view.custom.OutlineTitleAndEditTextField
import com.sap.cdc.android.sdk.example.ui.view.custom.PasswordNotMatchingError
import com.sap.cdc.android.sdk.example.ui.view.custom.SimpleErrorMessages
import com.sap.cdc.android.sdk.example.ui.view.custom.SmallSpacer
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Credentials registration basic view.
 */

@Composable
fun EmailRegisterView(viewModel: IViewModelAuthentication) {

    // Editable variables.
    var name by remember {
        mutableStateOf("")
    }
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var confirmPassword by remember {
        mutableStateOf("")
    }
    // State modifiers.
    var passwordVisible: Boolean by remember { mutableStateOf(false) }
    val isNotMatching = remember {
        derivedStateOf {
            password != confirmPassword
        }
    }
    var registerError by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var loading by remember { mutableStateOf(false) }

    // UI elements.

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {

        LargeSpacer()
        Text("Create your account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        SmallSpacer()
        Text("Please fill out the listed inputs", fontSize = 16.sp, fontWeight = FontWeight.Light)
        MediumSpacer()

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Name Input.
            SmallSpacer()
            OutlineTitleAndEditTextField(
                titleText = "Name: *",
                inputText = name,
                placeholderText = "Name placeholder",
                onValueChange = {
                    name = it
                },
                focusManager = focusManager
            )

            // Email input.
            SmallSpacer()
            OutlineTitleAndEditTextField(
                titleText = "Email: *",
                inputText = email,
                placeholderText = "Email placeholder",
                onValueChange = {
                    email = it
                },
                focusManager = focusManager
            )

            // Password input.
            SmallSpacer()
            OutlineTitleAndEditPasswordTextField(
                titleText = "Password: *",
                inputText = password,
                placeholderText = "",
                passwordVisible = passwordVisible,
                onValueChange = {
                    password = it
                },
                onEyeClick = { passwordVisible = it },
                focusManager = focusManager
            )

            // Confirm password input.
            SmallSpacer()
            OutlineTitleAndEditPasswordTextField(
                titleText = "Confirm password: *",
                inputText = confirmPassword,
                placeholderText = "",
                passwordVisible = passwordVisible,
                onValueChange = {
                    confirmPassword = it
                },
                onEyeClick = { passwordVisible = it },
                focusManager = focusManager
            )

            if (isNotMatching.value) {
                PasswordNotMatchingError()
            }

            MediumSpacer()
            Text(
                "By clicking on \"register\", you conform that you have read and agree to the privacy policy and terms of use.",
            )

            MediumSpacer()
            ActionOutlineButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Register",
                onClick = {
                    registerError = ""
                    loading = true
                    // Credentials registration.
                    viewModel.register(
                        email = email,
                        password = password,
                        name = name,
                        onLogin = {
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = { error ->
                            if (error != null) {
                                // Need to display error information.
                                registerError = error.errorDetails!!
                            }
                        }
                    )
                }
            )

            if (registerError.isNotEmpty()) {
                SimpleErrorMessages(
                    text = registerError
                )
            }
        }
    }

    // Loading indicator on top of all views.
    Box(Modifier.fillMaxWidth()) {
        IndeterminateLinearIndicator(loading)
    }
}


@Preview
@Composable
fun EmailRegisterViewPreview() {
    AppTheme {
        EmailRegisterView(ViewModelAuthenticationPreview())
    }
}