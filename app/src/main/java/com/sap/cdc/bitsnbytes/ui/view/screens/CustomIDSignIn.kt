package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
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
fun CustomIDSignInView(viewModel: ICustomIDSignInViewModel) {
    var loading by remember { mutableStateOf(false) }
    var identifier by remember {
        mutableStateOf("")
    }
    var identifierType by remember {
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
        Text("Sign In with National ID", style = AppTheme.typography.titleLarge)
        SmallVerticalSpacer()
        Text(
            "Please enter your custom credentials",
            style = AppTheme.typography.body
        )
        MediumVerticalSpacer()

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SmallVerticalSpacer()

            // Identifier input.
            OutlineTitleAndEditTextField(
                modifier = Modifier,
                titleText = "National ID: *",
                inputText = identifier,
                placeholderText = "Identifier placeholder",
                onValueChange = {
                    identifier = it
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

            LargeVerticalSpacer()

            ActionOutlineButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Login",
                onClick = {
                    loading = true
                    viewModel.login(
                            identifier,
                            password
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
                    }
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
fun CustomIDSignInViewPreview() {
    AppTheme {
        CustomIDSignInView(CustomIDSignInViewModelPreview())
    }
}