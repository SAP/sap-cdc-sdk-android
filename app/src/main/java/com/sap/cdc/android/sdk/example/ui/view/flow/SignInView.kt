@file:OptIn(ExperimentalMaterial3Api::class)

package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.view.custom.ViewDynamicSocialSelection
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Sign in flows initiator selection view.
 */

@Composable
fun SignInView(viewModel: IViewModelAuthentication) {
    var loading by remember { mutableStateOf(false) }

    var signInError by remember { mutableStateOf("") }

    val context = LocalContext.current

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
        Text("Sign In", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text("Use your preferred method", fontSize = 16.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.size(24.dp))


        ViewDynamicSocialSelection(
            listOf("facebook", "google", "apple", "line")
        ) { provider ->
            viewModel.socialSignInWith(
                context as ComponentActivity,
                viewModel.getAuthenticationProvider(provider),
                onLogin = {
                    loading = false
                    signInError = ""
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = { error ->
                    loading = false
                    signInError = error?.errorDetails!!
                },
                onPendingRegistration = { authResponse ->
                    loading = false
                    NavigationCoordinator.INSTANCE
                        .navigate(
                            "${ProfileScreenRoute.ResolvePendingRegistration.route}/${
                                authResponse?.resolvable()?.toJson()
                            }"
                        )
                },
                onLoginIdentifierExists = { authResponse ->
                    loading = false
                    NavigationCoordinator.INSTANCE
                        .navigate(
                            "${ProfileScreenRoute.ResolveLinkAccount.route}/${
                                authResponse?.resolvable()?.toJson()
                            }"
                        )
                }
            )
        }
        
        Spacer(modifier = Modifier.size(24.dp))
        HorizontalDivider(
            modifier = Modifier.size(
                240.dp, 1.dp
            ), thickness = 1.dp, color = Color.LightGray
        )

        Spacer(modifier = Modifier.size(24.dp))

        OutlinedButton(modifier = Modifier.size(width = 260.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                NavigationCoordinator.INSTANCE.navigate("${ProfileScreenRoute.AuthTabView.route}/1")
            }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_faceid),
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text("Passwordless")
            }

        }

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedButton(modifier = Modifier.size(width = 260.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.EmailSignIn.route)
            }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Password,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text("Sign in with Credentials")
            }

        }

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedButton(modifier = Modifier.size(width = 260.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(
                    "${ProfileScreenRoute.OTPSignIn.route}/${OTPType.Email.value}"
                )
            }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_email),
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text("Sign in with Email")
            }

        }

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedButton(modifier = Modifier.size(width = 260.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(
                    "${ProfileScreenRoute.OTPSignIn.route}/${OTPType.PHONE.value}"
                )
            }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_device),
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text("Sign in with Phone")
            }
        }

        if (signInError.isNotEmpty()) {
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "",
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = signInError,
                    color = Color.Red,
                )
            }
        }
    }
}

@Preview
@Composable
fun SignInViewPreview() {
    SignInView(viewModel = ViewModelAuthenticationPreview())
}