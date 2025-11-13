@file:OptIn(ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.AuthError
import com.sap.cdc.android.sdk.feature.RegistrationContext
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.PendingRegistrationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Custom view for dynamically creating input fields for resolving "Account Pending Registration"
 * interruption flow.
 */


@Composable
fun PendingRegistrationView(
    viewModel: IPendingRegistrationViewModel,
    registrationContext: RegistrationContext
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    // Initialize fields on first composition
    LaunchedEffect(registrationContext) {
        viewModel.initializeMissingFields(registrationContext)
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is PendingRegistrationNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                        toRoute = ProfileScreenRoute.MyProfile.route,
                        rootRoute = ProfileScreenRoute.Welcome.route
                    )
                }
                is PendingRegistrationNavigationEvent.NavigateToLinkAccount -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.ResolveLinkAccount.route}/${event.linkingContext}"
                    )
                }
            }
        }
    }

    // UI elements

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.size(80.dp))
        Text("Account Pending Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Missing required fields for registration",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(24.dp))

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            state.missingFields.forEach { field ->
                val fieldValue = state.fieldValues[field] ?: ""
                val autoFillHandler =
                    autoFillRequestHandler(
                        contentTypes = listOf(ContentType.EmailAddress, ContentType.Password),
                        onFill = {
                            viewModel.updateFieldValue(field, it)
                        }
                    )
                Text(
                    "${field.replaceFirstChar { it.uppercase() }}: *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                )
                TextField(
                    value = fieldValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .connectNode(handler = autoFillHandler)
                        .defaultFocusChangeAutoFill(handler = autoFillHandler),
                    placeholder = {
                        Text(
                            "${field.replaceFirstChar { it.uppercase() }} Placeholder",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    onValueChange = {
                        viewModel.updateFieldValue(field, it)
                    },
                    keyboardActions = KeyboardActions {
                        focusManager.moveFocus(FocusDirection.Next)
                    },
                )
                Spacer(modifier = Modifier.size(12.dp))
            }

            Spacer(modifier = Modifier.size(24.dp))
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    viewModel.onResolve(registrationContext.regToken!!)
                }) {
                Text("Resolve")
            }
        }

        LargeVerticalSpacer()

        // Error message
        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(text = error)
            }
        }
    }

    // Loading indicator on top of all views.
    Box(Modifier.fillMaxWidth()) {
        IndeterminateLinearIndicator(state.isLoading)
    }
}

@Composable
@Preview
fun PendingRegistrationViewPreview() {
    PendingRegistrationView(
        PendingRegistrationViewModelPreview(),
        RegistrationContext(originatingError = AuthError(
            message = "Pending registration error",
            code = "400",
            details = "Missing required fields for registration: nickname",
            ""
        ))
    )
}
