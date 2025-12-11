@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.PhoneSelectionNavigationEvent
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.CountryCodeSelector
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomSizeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallActionTextButton
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer

@Composable
fun PhoneSelectionView(
    viewModel: IPhoneSelectionViewModel
) {
    val state by viewModel.state.collectAsState()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is PhoneSelectionNavigationEvent.NavigateToPhoneVerification -> {
                    NavigationCoordinator.INSTANCE.navigate(
                        "${ProfileScreenRoute.PhoneVerification.route}/${event.twoFactorContext}"
                    )
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(28.dp)
    ) {
        Spacer(modifier = Modifier.size(60.dp))
        Text("Phone Verification", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Select your phone number",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(24.dp))

        // Divider
        HorizontalDivider(
            modifier = Modifier.size(
                240.dp, 1.dp
            ), thickness = 1.dp, color = Color.LightGray
        )

        Spacer(modifier = Modifier.size(24.dp))

        // Vary provider state to show different phone numbers
        // or to register a new one if none available.
        if (viewModel.twoFactorContext.collectAsState().value?.tfaProviders?.activeProviders?.isEmpty() == true) {
            // Need to register a new phone number
            RegisterNewPhoneNumber(viewModel = viewModel)
        } else {
            // Show available phone numbers
            RegisteredPhoneNumbers(viewModel = viewModel)
        }

        LargeVerticalSpacer()

        state.error?.let { error ->
            if (error.isNotEmpty()) {
                SimpleErrorMessages(text = error)
            }
        }
    }
}


@Composable
fun RegisterNewPhoneNumber(
    viewModel: IPhoneSelectionViewModel
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    Text("Please enter your phone number")
    LargeVerticalSpacer()

    val autoFillHandler =
        autoFillRequestHandler(
            contentTypes = listOf(
                ContentType.EmailAddress,
                ContentType.PhoneNumber
            ),
            onFill = {
                viewModel.updateInputField(it)
            }
        )

    Text(
        "Phone Number:",
        fontSize = 16.sp,
        fontWeight = FontWeight.Light,
    )
    SmallVerticalSpacer()

    // Row containing country selector and phone number input
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country code selector
        CountryCodeSelector(
            selectedCountry = state.selectedCountry,
            onCountrySelected = { country ->
                viewModel.updateSelectedCountry(country)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Phone number input field
        TextField(
            value = state.inputField,
            modifier = Modifier
                .weight(1f)
                .connectNode(handler = autoFillHandler)
                .defaultFocusChangeAutoFill(handler = autoFillHandler),
            placeholder = {
                Text(
                    "Enter phone number",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            },
            textStyle = TextStyle(
                color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Normal
            ),
            onValueChange = {
                viewModel.updateInputField(it)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Next)
            },
        )
    }

    CustomSizeVerticalSpacer(48.dp)

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp),
        shape = RoundedCornerShape(6.dp),
        onClick = {
            viewModel.onRegisterPhoneNumber()
        }) {
        Text("Send code")
    }
}

@Composable
fun RegisteredPhoneNumbers(
    viewModel: IPhoneSelectionViewModel
) {
    val phoneList by viewModel.phoneList.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadRegisteredPhoneNumbers()
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(phoneList) { tfaPhoneEntity ->
            ActionOutlineButton(
                modifier = Modifier,
                text = tfaPhoneEntity.obfuscated ?: ""
            ) {
                viewModel.onSendCode(tfaPhoneEntity.id ?: "")
            }
        }
    }

    LargeVerticalSpacer()

    SmallActionTextButton(
        "Back to Login Screen"
    ) {
        //TODO: Navigate to login screen
    }
}

@Preview
@Composable
fun PhoneSelectionViewPreview() {
    AppTheme {
        PhoneSelectionView(
            viewModel = PhoneSelectionViewModelPreview()
        )
    }
}
