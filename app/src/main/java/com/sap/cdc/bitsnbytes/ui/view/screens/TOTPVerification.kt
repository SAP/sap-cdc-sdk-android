@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.ResolvableTFA
import com.sap.cdc.android.sdk.auth.tfa.TFAProvidersEntity
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.OtpTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.viewmodel.ITOTPVerificationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.TOTPVerificationViewModelPreview

@Composable
fun TOTPVerificationView(
    viewModel: ITOTPVerificationViewModel,
    resolvableContext: ResolvableContext
) {
    var loading by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(28.dp)
            .verticalScroll(scrollState)
    ) {

        if (resolvableContext.tfa?.tfaProviders?.activeProviders?.isEmpty() == true) {
            // Need to register a new authenticator app
            RegisterAuthenticatorAppWithQAView(
                viewModel = viewModel,
                resolvableContext = resolvableContext,
                onLoadChanged = { loading = it },
                onVerificationErrorChanged = { verificationError = it }
            )
        } else {
            // Show verification view only.
            TOTPCodeVerificationView(
                viewModel = viewModel,
                resolvableContext = resolvableContext,
                onLoadChanged = { loading = it },
                onVerificationErrorChanged = { verificationError = it }
            )
        }
    }
}

@Composable
fun RegisterAuthenticatorAppWithQAView(
    viewModel: ITOTPVerificationViewModel,
    resolvableContext: ResolvableContext,
    onLoadChanged: (Boolean) -> Unit,
    onVerificationErrorChanged: (String) -> Unit,
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.LightGray)
        ) {
            Text("Use an authenticator app (such as Google Authenticator) to scan this QR code or manually enter the secret key. ")
        }

        var updatedResolvableContext by remember { mutableStateOf<ResolvableContext?>(null) }

        LaunchedEffect(Unit) {
            onLoadChanged(true)
            viewModel.registerNewAuthenticatorApp(
                resolvableContext = resolvableContext,
                onQACode = { iAuthResponse, newBitmap ->
                    updatedResolvableContext = iAuthResponse.resolvable()!!
                    onLoadChanged(false)
                    onVerificationErrorChanged("")
                    bitmap = newBitmap
                },
                onFailedWith = { error ->
                    onLoadChanged(false)
                    onVerificationErrorChanged(error?.errorDescription!! ?: "")
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            BitmapImageView(bitmap = bitmap)
        }

        LargeVerticalSpacer()

        var otpValue by remember {
            mutableStateOf("")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.size(80.dp))
            Text(
                "Enter the 6-Digit Code", fontSize = 28.sp, fontWeight = FontWeight.Bold
            )

            SmallVerticalSpacer()

            Text(
                "Enter the code from your authenticator app:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light
            )

            MediumVerticalSpacer()

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

            SmallVerticalSpacer()

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    onLoadChanged(true)
                    viewModel.verifyTOTPCode(
                        resolvableContext = updatedResolvableContext!!,
                        code = otpValue,
                        onVerificationSuccess = {
                            // Navigate to the next screen.
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = {
                            onLoadChanged(false)
                            onVerificationErrorChanged(it?.errorDescription ?: "")
                        }
                    )

                }) {
                Text("Finish")
            }
        }
    }
}

@Composable
fun BitmapImageView(bitmap: Bitmap?) {
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )
    }
}

@Composable
fun TOTPCodeVerificationView(
    viewModel: ITOTPVerificationViewModel,
    resolvableContext: ResolvableContext,
    onLoadChanged: (Boolean) -> Unit,
    onVerificationErrorChanged: (String) -> Unit,
) {
    var otpValue by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.size(80.dp))
        Text(
            "Enter the 6-Digit Code", fontSize = 28.sp, fontWeight = FontWeight.Bold
        )

        SmallVerticalSpacer()

        Text(
            "Enter the code from your authenticator app:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )

        MediumVerticalSpacer()

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

        SmallVerticalSpacer()

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                onLoadChanged(true)
                viewModel.verifyTOTPCode(
                    resolvableContext = resolvableContext,
                    code = otpValue,
                    onVerificationSuccess = {
                        // Navigate to the next screen.
                        NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                    },
                    onFailedWith = {
                        onLoadChanged(false)
                        onVerificationErrorChanged(it?.errorDescription ?: "")
                    }
                )
            }) {
            Text("Verify")
        }
    }
}

@Composable
@Preview
fun TOTPVerificationViewPreview() {
    AppTheme {
        TOTPVerificationView(
            viewModel = TOTPVerificationViewModelPreview(),
            resolvableContext = ResolvableContext(
                regToken = "", tfa = ResolvableTFA(
                    tfaProviders = TFAProvidersEntity(
                        activeProviders = listOf(), inactiveProviders = listOf()
                    ),
                )
            ),
        )
    }
}