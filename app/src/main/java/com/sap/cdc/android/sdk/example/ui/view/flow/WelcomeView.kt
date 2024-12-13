package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ApplicationConfig
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.route.ScreenSetsRoute
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Welcome view.
 */

@Composable
fun WelcomeView(viewModel: IViewModelAuthentication) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.size(140.dp))
        Text("Welcome!", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("Manage your profile", fontSize = 16.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.size(20.dp))

        Spacer(modifier = Modifier.size(20.dp))

        OutlinedButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                if (ApplicationConfig.useWebViews) {
                    NavigationCoordinator.INSTANCE
                        .navigate(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route)
                } else {
                    NavigationCoordinator.INSTANCE
                        .navigate("${ProfileScreenRoute.AuthTabView.route}/1")
                }
            }) {
            Text("Sign in")
        }
        Spacer(modifier = Modifier.size(20.dp))

        OutlinedButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                if (ApplicationConfig.useWebViews) {
                    NavigationCoordinator.INSTANCE
                        .navigate(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route)
                } else {
                    NavigationCoordinator.INSTANCE
                        .navigate("${ProfileScreenRoute.AuthTabView.route}/0")
                }
            }) {
            Text("Register")
        }
        Spacer(modifier = Modifier.size(28.dp))
        HorizontalDivider(
            color = Color.LightGray, thickness = 1.dp, modifier = Modifier.size(
                240.dp, 1.dp
            )
        )
        Spacer(modifier = Modifier.size(28.dp))
        TextButton(onClick = {
            viewModel.singleSignOn(
                context as ComponentActivity,
                mutableMapOf(),
                onLogin = {
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = { error ->

                }
            )
        }) {
            Text("Sign in with SSO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

    }
}

@Preview
@Composable
fun WelcomeViewPreview() {
    WelcomeView(ViewModelAuthenticationPreview())
}
