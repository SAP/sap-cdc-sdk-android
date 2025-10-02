package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.OTPContext
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.OtpTextField

/**
 * Created by Tal Mirmelshtein on 25/11/2024
 * Copyright: SAP LTD.
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpVerifyView(
    viewModel: IOtpVerifyViewModel,
    otpContext: OTPContext,
    otpType: OTPType,
    inputField: String? = null,
) {
    var loading by remember { mutableStateOf(false) }

    var signInError by remember { mutableStateOf("") }

    var codeSent by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // UI elements.
        IndeterminateLinearIndicator(loading)

        var otpValue by remember {
            mutableStateOf("")
        }

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
                    "Please enter the code sent to your phone"
                }

                OTPType.Email -> {
                    "Please enter the code sent to your email"
                }
            }, fontSize = 16.sp, fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(32.dp))

        if (codeSent) {
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info, contentDescription = "", tint = Color.Blue
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "A new code was sent to $inputField",
                    color = Color.Blue,
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Enter verification code:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(modifier = Modifier.size(10.dp))

            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                val autoFillHandler =
                    autoFillRequestHandler(
                        contentTypes = listOf(ContentType.SmsOtpCode, ContentType.EmailAddress),
                        onFill = {
                            otpValue = it
                        }
                    )
                OtpTextField(
                    modifier = Modifier
                        .connectNode(handler = autoFillHandler)
                        .defaultFocusChangeAutoFill(handler = autoFillHandler),
                    otpText = otpValue, onOtpTextChange = { value, _ ->
                        otpValue = value
                        if (value.isEmpty()) autoFillHandler.requestVerifyManual()
                    })
            }

            Spacer(modifier = Modifier.size(6.dp))

            if (signInError.isNotEmpty()) {
                Spacer(modifier = Modifier.size(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Cancel, contentDescription = "", tint = Color.Red
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = signInError,
                        color = Color.Red,
                    )
                }
            }

            Spacer(modifier = Modifier.size(48.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    loading = true
                    viewModel.verifyCode(
                        code = otpValue,
                        vToken = otpContext.vToken ?: "",
                    ) {
                        onSuccess = {
                            loading = false
                            signInError = ""
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        }

                        onError = { error ->
                            signInError = error.message
                            loading = false
                        }

                        onPendingRegistration = { registrationContext ->
                            loading = false
                            NavigationCoordinator.INSTANCE
                                .navigate(
                                    "${ProfileScreenRoute.ResolvePendingRegistration.route}/${
                                        registrationContext.toJson()
                                    }"
                                )
                        }
                    }
                }) {
                Text("Verify")
            }

        }

        Spacer(modifier = Modifier.size(24.dp))

        Text(
            when (codeSent) {
                true -> "Sent code again"
                false -> "Didn't get code"
            },
            color = when (codeSent) {
                true -> Color.LightGray
                false -> Color.Black
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(enabled = !codeSent) {
                    codeSent = true
                    //TODO: cancel timer when composable is not active.
                    viewModel.startOtpTimer {
                        codeSent = false
                    }
                }
                .padding(start = 16.dp, end = 16.dp),
        )

        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
@Preview
fun PhoneOtpVerifyView() {
    OtpVerifyView(
        viewModel = OtpVerifyViewModelPreview(),
        otpContext = OTPContext(""),
        otpType = OTPType.PHONE,
        inputField = ""
    )
}
