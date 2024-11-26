package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

/**
 * Created by Tal Mirmelshtein on 25/11/2024
 * Copyright: SAP LTD.
 */

@Composable
fun OtpVerifyView(
    viewModel: IViewModelAuthentication,
    authResolvable: AuthResolvable,
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
                OtpTextField(otpText = otpValue, onOtpTextChange = { value, _ ->
                    otpValue = value
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

            OutlinedButton(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    loading = true
                    viewModel.resolveLoginWithCode(code = otpValue,
                        resolvable = authResolvable,
                        onLogin = {
                            loading = false
                            signInError = ""
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = { error ->
                            signInError = error?.errorDescription!!
                            loading = false
                        })
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
fun ViewPhoneSignInCodeVerificationPreview() {
    OtpVerifyView(
        viewModel = ViewModelAuthenticationPreview(),
        authResolvable = AuthResolvable(),
        otpType = OTPType.PHONE,
        inputField = ""
    )
}

@Composable
fun OtpTextField(
    modifier: Modifier = Modifier,
    otpText: String,
    otpCount: Int = 6,
    onOtpTextChange: (String, Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        if (otpText.length > otpCount) {
            throw IllegalArgumentException("Otp text value must not have more than otpCount: $otpCount characters")
        }
    }

    BasicTextField(modifier = modifier,
        value = TextFieldValue(otpText, selection = TextRange(otpText.length)),
        onValueChange = {
            if (it.text.length <= otpCount) {
                onOtpTextChange.invoke(it.text, it.text.length == otpCount)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(otpCount) { index ->
                    CharView(
                        index = index, text = otpText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        })
}

@Composable
private fun CharView(
    index: Int, text: String
) {
    val isFocused = text.length == index
    val char = when {
        index == text.length -> ""
        index > text.length -> ""
        else -> text[index].toString()
    }
    Text(
        modifier = Modifier
            .width(40.dp)
            .border(
                1.dp, when {
                    isFocused -> Color.Black
                    else -> Color.Gray
                }, RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        text = char,
        style = MaterialTheme.typography.headlineLarge,
        color = if (isFocused) {
            Color.Gray
        } else {
            Color.Black
        },
        textAlign = TextAlign.Center
    )
}