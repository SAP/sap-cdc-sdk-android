package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.view.composables.IconAndTextOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallActionTextButton
import com.sap.cdc.bitsnbytes.ui.viewmodel.ITFAAuthenticationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.TFAAuthenticationViewModelPreview

@Composable
fun AuthMethodsView(
    viewModel: ITFAAuthenticationViewModel,
    resolvableContext: ResolvableContext
) {
    LaunchedEffect(Unit) {
        // Update resolvable context in shared view model.
        viewModel.updateResolvableContext(resolvableContext)
    }

    //TODO: Dynamically show the auth methods based on the resolvable context available/unavailable providers
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(28.dp)
    ) {
        Spacer(modifier = Modifier.size(60.dp))
        Text("Auth Methods", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Use your preferred method",
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

        // Send Code to email button
//        IconAndTextOutlineButton(
//            modifier = Modifier.size(width = 240.dp, height = 44.dp),
//            text = "Send Code to Email",
//            onClick = {
//            },
//            iconResourceId = R.drawable.ic_email,
//
//            )
//        Spacer(modifier = Modifier.size(10.dp))

        // Send Code to Phone button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Send Code to Phone",
            onClick = {
                NavigationCoordinator.INSTANCE
                    .navigate(ProfileScreenRoute.PhoneSelection.route)
            },
            iconResourceId = R.drawable.ic_device,

            )
        Spacer(modifier = Modifier.size(10.dp))

        // Use a TOTP App button
        IconAndTextOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Use a TOTP App",
            onClick = {
                NavigationCoordinator.INSTANCE
                    .navigate(ProfileScreenRoute.TOTPVerification.route)
            },
            iconResourceId = R.drawable.ic_lock,

            )
        Spacer(modifier = Modifier.size(10.dp))

        SmallActionTextButton(
            "Back to Login Screen"
        ) {
            //TODO: Navigate to login screen
        }
    }
}

@Preview
@Composable
fun AuthMethodsViewPreview() {
    AppTheme {
        AuthMethodsView(
            viewModel = TFAAuthenticationViewModelPreview(),
            resolvableContext = ResolvableContext(
                regToken = ""
            )
        )
    }
}