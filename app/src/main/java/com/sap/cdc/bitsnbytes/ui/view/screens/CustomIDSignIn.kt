package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.CustomIDSignInNavigationEvent
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
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is CustomIDSignInNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                }
            }
        }
    }

    //UI elements
    LoadingStateColumn(
        state.isLoading
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
                inputText = state.identifier,
                placeholderText = "Identifier placeholder",
                onValueChange = { viewModel.onIdentifierChanged(it) },
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

            LargeVerticalSpacer()

            ActionOutlineButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Login",
                onClick = { viewModel.onLoginClick() }
            )

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
fun CustomIDSignInViewPreview() {
    AppTheme {
        CustomIDSignInView(CustomIDSignInViewModelPreview())
    }
}
