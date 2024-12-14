package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
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
import com.sap.cdc.android.sdk.example.ui.view.custom.SimpleErrorMessages
import com.sap.cdc.android.sdk.example.ui.view.custom.SmallSpacer
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview


/**
 * Created by Tal Mirmelshtein on 19/06/2024
 * Copyright: SAP LTD.
 *
 * Credentials sign in basic view.
 */

@Composable
fun EmailSignInView(viewModel: IViewModelAuthentication) {

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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {

        LargeSpacer()
        Text("Sign In with Email", style = AppTheme.typography.titleLarge)
        SmallSpacer()
        Text(
            "Please enter your email and password",
            style = AppTheme.typography.body
        )
        MediumSpacer()

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                placeholderText = "Password placeholder",
                passwordVisible = passwordVisible,
                onValueChange = {
                    password = it
                },
                onEyeClick = { passwordVisible  = it },
                focusManager = focusManager
            )


            MediumSpacer()
            Box(modifier = Modifier.align(Alignment.End)) {
                Surface(onClick = {
                    // Forgot password route.
                }) {
                    Text("Forgot password?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            SmallSpacer()

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
        EmailSignInView(ViewModelAuthenticationPreview())
    }
}