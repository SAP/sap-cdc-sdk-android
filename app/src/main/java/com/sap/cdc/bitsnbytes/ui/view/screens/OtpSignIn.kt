@file:OptIn(ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomSizeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.viewmodel.IOtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpSignInViewModelPreview

/**
 * Created by Tal Mirmelshtein on 25/11/2024
 * Copyright: SAP LTD.
 */

enum class OTPType(val value: Int) {
    PHONE(0), Email(1);

    companion object {
        private val VALUES = entries.toTypedArray()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.value == value }
    }
}

@Composable
fun OtpSignInView(
    viewModel: IOtpSignInViewModel,
    otpType: OTPType,
) {
    var loading by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf("") }

    var inputField by remember {
        mutableStateOf("")
    }

    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // UI elements.
        IndeterminateLinearIndicator(loading)

        Spacer(modifier = Modifier.size(80.dp))
        Text(
            when (otpType) {
                OTPType.PHONE -> {
                    "Sign In with Phone"
                }

                OTPType.Email -> {
                    "Sign In with Email"
                }
            }, fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            when (otpType) {
                OTPType.PHONE -> {
                    "Please enter your phone number"
                }

                OTPType.Email -> {
                    "Please enter your email address"
                }
            }, fontSize = 16.sp, fontWeight = FontWeight.Light
        )
        LargeVerticalSpacer()

        val autoFillHandler =
            autoFillRequestHandler(autofillTypes = listOf(
                AutofillType.EmailAddress,
                AutofillType.PhoneNumber
            ),
                onFill = {
                    inputField = it
                }
            )

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (otpType) {
                OTPType.PHONE -> {
                    Text(
                        "Phone Number:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                    )


                    TextField(
                        inputField,
                        modifier = Modifier.fillMaxWidth().connectNode(handler = autoFillHandler)
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
                }

                OTPType.Email -> {
                    Text(
                        "Email:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                    )
                    TextField(
                        inputField,
                        modifier = Modifier.fillMaxWidth().connectNode(handler = autoFillHandler)
                            .defaultFocusChangeAutoFill(handler = autoFillHandler),
                        placeholder = {
                            Text(
                                "Enter email address",
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        keyboardActions = KeyboardActions {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                    )
                }
            }

            SmallVerticalSpacer()

            if (signInError.isNotEmpty()) {
                SimpleErrorMessages(signInError)
            }

            CustomSizeVerticalSpacer(48.dp)

            OutlinedButton(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    loading = true
                    signInError = ""
                    viewModel.otpSignIn(
                        otpType = otpType,
                        inputField = inputField,
                        success = { resolvable ->
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(
                                "${ProfileScreenRoute.OTPVerify.route}/${
                                    resolvable.toJson()
                                }/${otpType.value}/${inputField}"
                            )
                        },
                        onFailed = { error ->
                            signInError = error.errorDescription!!
                            loading = false
                        })

                }) {
                Text("Send code")
            }
        }

    }
}

@Composable
@Preview
fun PhoneOtpSignInViewPreview() {
    OtpSignInView(
        viewModel = OtpSignInViewModelPreview(),
        otpType = OTPType.PHONE
    )
}