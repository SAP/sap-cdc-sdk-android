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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.tfa.TFAProvidersEntity
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.TOTPVerificationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.utils.autofillSemantics
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.OtpTextField
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer

@Composable
fun TOTPVerificationView(
    viewModel: ITOTPVerificationViewModel,
    twoFactorContext: TwoFactorContext
) {
    LaunchedEffect(twoFactorContext) {
        viewModel.updateTwoFactorContext(twoFactorContext)
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is TOTPVerificationNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                }
            }
        }
    }

    if (viewModel.twoFactorContext.collectAsState().value?.tfaProviders?.activeProviders?.isEmpty() == true) {
        RegisterAuthenticatorAppWithQAView(viewModel = viewModel)
    } else {
        TOTPCodeVerificationView(viewModel = viewModel)
    }
}

@Composable
fun RegisterAuthenticatorAppWithQAView(
    viewModel: ITOTPVerificationViewModel
) {
    val state by viewModel.state.collectAsState()
    val bitmap = viewModel.qACode.collectAsState().value
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.onRegisterNewAuthenticatorApp()
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(10.dp)
                .background(Color.LightGray)
        ) {
            Text("Use an authenticator app (such as Google Authenticator) to scan this QR code or manually enter the secret key. ")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(4.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            BitmapImageView(bitmap = bitmap)
        }

        MediumVerticalSpacer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
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
                OtpTextField(
                    modifier = Modifier
                        .autofillSemantics(ContentType.SmsOtpCode),
                    otpText = state.otpValue, onOtpTextChange = { value, _ ->
                        viewModel.updateOtpValue(value)
                    })
            }

            LargeVerticalSpacer()

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 44.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    viewModel.onVerifyCode()
                }) {
                Text("Finish")
            }

            MediumVerticalSpacer()
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
    viewModel: ITOTPVerificationViewModel
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
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
            OtpTextField(
                modifier = Modifier
                    .autofillSemantics(ContentType.SmsOtpCode),
                otpText = state.otpValue, onOtpTextChange = { value, _ ->
                    viewModel.updateOtpValue(value)
                })
        }

        LargeVerticalSpacer()

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                viewModel.onVerifyCode()
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
            twoFactorContext = TwoFactorContext(
                tfaProviders = TFAProvidersEntity(
                    activeProviders = listOf()
                )
            )
        )
    }
}
