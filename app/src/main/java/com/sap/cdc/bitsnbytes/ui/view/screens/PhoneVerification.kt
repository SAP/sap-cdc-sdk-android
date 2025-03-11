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
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.OtpTextField
import com.sap.cdc.bitsnbytes.ui.viewmodel.IPhoneVerificationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.PhoneVerificationViewModelPreview

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhoneVerificationView(
    viewModel: IPhoneVerificationViewModel,
    resolvableContext: ResolvableContext,
) {
    var loading by remember { mutableStateOf(false) }

    var verificationError by remember { mutableStateOf("") }

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
            "Sign In with Phone", fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Please enter the code sent to your phone",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
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
                        autofillTypes = listOf(AutofillType.SmsOtpCode, AutofillType.EmailAddress),
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

            if (verificationError.isNotEmpty()) {
                Spacer(modifier = Modifier.size(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Cancel, contentDescription = "", tint = Color.Red
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = verificationError,
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
                    viewModel.verifyTFACode(
                        otpValue,
                        resolvableContext,
                        rememberDevice = false,
                        onVerified = {
                            loading = false
                            verificationError = ""
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = { error ->
                            verificationError = error?.errorDescription!!
                            loading = false

                        }
                    )
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

@Preview
@Composable
fun PhoneVerificationViewPreview() {
    AppTheme {
        PhoneVerificationView(
            viewModel = PhoneVerificationViewModelPreview(),
            resolvableContext = ResolvableContext(regToken = "")
        )
    }
}