package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.ViewDynamicSocialSelection
import com.sap.cdc.bitsnbytes.ui.viewmodel.ILinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LinkAccountViewModelPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Custom view for dynamically creating the correct fields to continue linking the users account
 * in the event of an interrupted flow.
 */

@Composable
fun LinkAccountView(
    viewModel: ILinkAccountViewModel,
    resolvable: ResolvableContext,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    var linkError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    var password by remember {
        mutableStateOf("")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(28.dp)
    ) {
        Spacer(modifier = Modifier.size(60.dp))
        Text("Link Your Accounts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "An account already exists with this identifier. Link both accounts.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(24.dp))

        // Vary login providers list to display the correct link path (social or site).
        if (resolvable.linking?.conflictingAccounts!!.loginProviders.contains("site")) {
            // Login to site.
            Text("Link with account password")
            Spacer(modifier = Modifier.size(12.dp))
            TextField(
                value = password,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                onValueChange = {
                    password = it
                },
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
            )

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    loading = true
                    // Link to site account using password.
                    viewModel.resolveLinkToSiteAccount(
                        loginId = resolvable.linking?.conflictingAccounts?.loginID!!,
                        password = password,
                        resolvableContext = resolvable,
                        onLogin = {
                            loading = false
                            NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                                toRoute = ProfileScreenRoute.MyProfile.route,
                                rootRoute = ProfileScreenRoute.Welcome.route
                            )
                        },
                        onFailedWith = { error ->
                            loading = false
                        }
                    )
                }) {
                Text("Link Account")
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        val socialProvidersOnly = resolvable.linking?.conflictingAccounts!!.loginProviders.toMutableList()
        socialProvidersOnly.remove("site")
        if (socialProvidersOnly.size > 0) {
            Text("Link with existing social accounts")
            Spacer(modifier = Modifier.size(12.dp))
            // Login to social
            ViewDynamicSocialSelection(
                socialProviders = socialProvidersOnly,
            ) { provider ->
                loading = true
                viewModel.resolveLinkToSocialAccount(
                    hostActivity = context as ComponentActivity,
                    provider = provider,
                    resolvableContext = resolvable,
                    onLogin = {
                        loading = false
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            toRoute = ProfileScreenRoute.MyProfile.route,
                            rootRoute = ProfileScreenRoute.Welcome.route
                        )
                    },
                    onFailedWith = { error ->
                        loading = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.size(24.dp))
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp,
            modifier = Modifier.width(200.dp)
        )
        Spacer(modifier = Modifier.size(24.dp))

        OutlinedButton(modifier = Modifier.size(width = 240.dp, height = 44.dp),
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

        OutlinedButton(modifier = Modifier.size(width = 240.dp, height = 44.dp),
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
                    painter = painterResource(id = R.drawable.ic_email),
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text("Sign in with Email")
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.width(100.dp)
            )
            Text("Not you?")
            HorizontalDivider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.width(100.dp)
            )
        }

        Spacer(modifier = Modifier.size(44.dp))
        Text("Sign into a different account", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(24.dp))
        Text("Create a new one", fontSize = 14.sp, fontWeight = FontWeight.Bold)

    }

    // Loading indicator on top of all views.
    Box(Modifier.fillMaxWidth()) {
        IndeterminateLinearIndicator(loading)
    }
}

@Preview
@Composable
fun LinkAccountViewPreview() {
    LinkAccountView(
        viewModel = LinkAccountViewModelPreview(),
        resolvable = ResolvableContext(
            "",
        )
    )
}