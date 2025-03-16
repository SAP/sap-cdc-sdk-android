@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomSizeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallActionTextButton
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.viewmodel.ITFAAuthenticationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.TFAAuthenticationViewModelPreview

@Composable
fun PhoneSelectionView(
    viewModel: ITFAAuthenticationViewModel,
) {
    var loading by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf("") }

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
        if (viewModel.resolvableContext.collectAsState().value?.tfa?.tfaProviders?.activeProviders?.isEmpty() == true) {
            // Need to register a new phone number
            RegisterNewPhoneNumber(
                viewModel = viewModel,
                onLoadChanged = { loading = it },
                onVerificationErrorChanged = { verificationError = it }
            )
        } else {
            // Show available phone numbers
            RegisteredPhoneNumbers(
                viewModel = viewModel,
                onLoadChanged = { loading = it },
                onVerificationErrorChanged = { verificationError = it }
            )
        }

        LargeVerticalSpacer()

        if (verificationError.isNotEmpty()) {
            SimpleErrorMessages(
                text = verificationError
            )
        }
    }
}


@Composable
fun RegisterNewPhoneNumber(
    viewModel: ITFAAuthenticationViewModel,
    onLoadChanged: (Boolean) -> Unit,
    onVerificationErrorChanged: (String) -> Unit,
) {
    var inputField by remember {
        mutableStateOf("")
    }

    val focusManager = LocalFocusManager.current

    Text("Please enter your phone number")
    LargeVerticalSpacer()

    val autoFillHandler =
        autoFillRequestHandler(
            autofillTypes = listOf(
                AutofillType.EmailAddress,
                AutofillType.PhoneNumber
            ),
            onFill = {
                inputField = it
            }
        )

    Text(
        "Phone Number:",
        fontSize = 16.sp,
        fontWeight = FontWeight.Light,
    )
    SmallVerticalSpacer()

    TextField(
        inputField,
        modifier = Modifier
            .fillMaxWidth()
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
            inputField = it
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
    )

    CustomSizeVerticalSpacer(48.dp)

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp),
        shape = RoundedCornerShape(6.dp),
        onClick = {
            onLoadChanged(true)
            onVerificationErrorChanged("")
            viewModel.registerTFAPhoneNumber(
                inputField,
                "en",
                onVerificationCodeSent = { authResponse ->
                    onLoadChanged(false)
                    NavigationCoordinator.INSTANCE
                        .navigate(
                            "${ProfileScreenRoute.PhoneVerification.route}/${
                                authResponse?.resolvable()!!.toJson()
                            }"
                        )
                },
                onFailedWith = { error ->
                    onLoadChanged(false)
                    onVerificationErrorChanged(
                        error?.errorDescription!!
                    )
                }
            )

        }) {
        Text("Send code")
    }
}

@Composable
fun RegisteredPhoneNumbers(
    viewModel: ITFAAuthenticationViewModel,
    onLoadChanged: (Boolean) -> Unit,
    onVerificationErrorChanged: (String) -> Unit,
) {
    // Show registered phone numbers
    val phoneList by viewModel.phoneList.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            phoneList
        ) { tfaPhoneEntity ->
            ActionOutlineButton(
                modifier = Modifier,
                text = tfaPhoneEntity.obfuscated ?: ""
            ) {
                onLoadChanged(true)
                viewModel.sendRegisteredPhoneCode(
                    tfaPhoneEntity.id ?: "",
                    "en",
                    onVerificationCodeSent = { authResponse ->
                        onLoadChanged(false)
                        val resolvableJson = authResponse?.resolvable()!!.toJson()
                        NavigationCoordinator.INSTANCE
                            .navigate(
                                "${ProfileScreenRoute.PhoneVerification.route}/${
                                    Base64.encodeToString(
                                        resolvableJson.toByteArray(Charsets.UTF_8),
                                        Base64.DEFAULT
                                    )
                                }"
                            )
                    },
                    onFailedWith = { error ->
                        onLoadChanged(false)
                        onVerificationErrorChanged(
                            error?.errorDescription!!
                        )
                    }
                )
            }
        }
    }

    LargeVerticalSpacer()

    SmallActionTextButton(
        "Back to Login Screen"
    ) {
        //TODO: Navigate to login screen
    }

    onLoadChanged(true)
    viewModel.getRegisteredPhoneNumbers(
        onRegisteredPhoneNumbers = {
            onLoadChanged(false)
            onVerificationErrorChanged("")
        },
        onFailedWith = { error ->
            onLoadChanged(false)
            onVerificationErrorChanged(
                error?.errorDescription!!
            )
        }
    )

}

@Preview
@Composable
fun PhoneSelectionViewPreview() {
    AppTheme {
        PhoneSelectionView(
            viewModel = TFAAuthenticationViewModelPreview(),
        )
    }
}