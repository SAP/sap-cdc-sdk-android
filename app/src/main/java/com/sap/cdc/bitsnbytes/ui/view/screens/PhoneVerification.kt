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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.PhoneVerificationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.utils.autoFillRequestHandler
import com.sap.cdc.bitsnbytes.ui.utils.connectNode
import com.sap.cdc.bitsnbytes.ui.utils.defaultFocusChangeAutoFill
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.OtpTextField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhoneVerificationView(
    viewModel: IPhoneVerificationViewModel,
    twoFactorContext: TwoFactorContext
) {
    val state by viewModel.state.collectAsState()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is PhoneVerificationNavigationEvent.NavigateToMyProfile -> {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
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
    ) {
        // UI elements.
        IndeterminateLinearIndicator(state.isLoading)

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

        if (state.codeSent) {
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
                        contentTypes = listOf(ContentType.SmsOtpCode, ContentType.EmailAddress),
                        onFill = {
                            viewModel.updateOtpValue(it)
                        }
                    )
                OtpTextField(
                    modifier = Modifier
                        .connectNode(handler = autoFillHandler)
                        .defaultFocusChangeAutoFill(handler = autoFillHandler),
                    otpText = state.otpValue, onOtpTextChange = { value, _ ->
                        viewModel.updateOtpValue(value)
                        if (value.isEmpty()) autoFillHandler.requestVerifyManual()
                    })
            }

            Spacer(modifier = Modifier.size(6.dp))

            state.error?.let { error ->
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Cancel, contentDescription = "", tint = Color.Red
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = error,
                            color = Color.Red,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(48.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    viewModel.onVerifyCode(twoFactorContext)
                }) {
                Text("Verify")
            }

        }

        Spacer(modifier = Modifier.size(24.dp))

        Text(
            when (state.codeSent) {
                true -> "Sent code again"
                false -> "Didn't get code"
            },
            color = when (state.codeSent) {
                true -> Color.LightGray
                false -> Color.Black
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(enabled = !state.codeSent) {
                    viewModel.onCodeSent()
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
            twoFactorContext = TwoFactorContext()
        )
    }
}
