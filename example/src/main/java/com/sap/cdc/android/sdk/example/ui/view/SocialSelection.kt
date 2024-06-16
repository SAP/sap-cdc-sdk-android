package com.sap.cdc.android.sdk.example.ui.view

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.social.FacebookAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.GoogleAuthenticationProvider
import com.sap.cdc.android.sdk.example.social.LineAuthenticationProvider
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelCoordinator

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Composable
fun SocialSelectionView(onMutableValueChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 74.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            modifier = Modifier.size(52.dp),
            onClick = {
                onMutableValueChange(true)
                // Facebook social login.
                ViewModelCoordinator.authentication(context).socialSignInWith(
                    context as ComponentActivity, FacebookAuthenticationProvider(),
                    onLogin = {
                        onMutableValueChange(false)
                        NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                    },
                    onFailedWith = {
                        onMutableValueChange(false)
                        // Handle error here.
                    }
                )
            }) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.facebook_v),
                contentDescription = "Localized description",
                tint = Color.Unspecified
            )
        }
        IconButton(
            modifier = Modifier.size(52.dp),
            onClick = {
            // Google social login.
                ViewModelCoordinator.authentication(context).socialSignInWith(
                    context as ComponentActivity, GoogleAuthenticationProvider(),
                    onLogin = {
                        onMutableValueChange(false)
                        NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                    },
                    onFailedWith = {
                        onMutableValueChange(false)
                        // Handle error here.
                    }
                )
        }) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.google_v),
                contentDescription = "Localized description",
                tint = Color.Unspecified
            )

        }
        IconButton(
            modifier = Modifier.size(52.dp),
            onClick = {
            // Apple social login.
            onMutableValueChange(true)
            ViewModelCoordinator.authentication(context).socialWebSignInWith(
                context as ComponentActivity, "apple",
                onLogin = {
                    onMutableValueChange(false)
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = {
                    onMutableValueChange(false)
                    // Handle error here.
                }
            )
        }) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.apple_v),
                contentDescription = "Localized description",
                tint = Color.Unspecified
            )
        }
        IconButton(
            modifier = Modifier.size(52.dp),
            onClick = {
            // Line social login.
            onMutableValueChange(true)
            // Facebook social login.
            ViewModelCoordinator.authentication(context).socialSignInWith(
                context as ComponentActivity, LineAuthenticationProvider(),
                onLogin = {
                    onMutableValueChange(false)
                    NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                },
                onFailedWith = {
                    onMutableValueChange(false)
                    // Handle error here.
                }
            )
        }) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.ic_line),
                contentDescription = "Localized description",
                tint = Color.Unspecified
            )
        }
    }
}

@Preview
@Composable
fun SocialSelectionViewPreview() {
    SocialSelectionView({})
}