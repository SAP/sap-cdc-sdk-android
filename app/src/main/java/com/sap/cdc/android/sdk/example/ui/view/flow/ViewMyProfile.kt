package com.sap.cdc.android.sdk.example.ui.view.flow

import android.util.Log
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview
import kotlin.math.absoluteValue

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Profile view containing basic account information available from the SDK.
 * Addition authentication flows can be initiated from here when the application contains a valid
 * session.
 */

@Composable
fun ViewMyProfile(viewModel: IViewModelAuthentication) {
    val loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .fillMaxHeight(),
    ) {
        // UI elements.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                ) {
                    IndeterminateLinearIndicator(loading)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = Color.White)
                )
            }
            UserHead(
                id = "",
                firstName = viewModel.accountInfo()?.profile?.firstName ?: "",
                lastName = viewModel.accountInfo()?.profile?.lastName ?: "",
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(44.dp), contentAlignment = Alignment.Center
        ) {
            Text("Welcome,")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(44.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                "${viewModel.accountInfo()?.profile?.firstName ?: ""} ${viewModel.accountInfo()?.profile?.lastName ?: ""}",
                fontSize = 34.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .background(Color.White)
        )
        SelectionRow(title = "My Orders", leadingIcon = R.drawable.ic_cart_row)
        Spacer(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
        )
        SelectionRow(
            title = "About Me", leadingIcon = R.drawable.ic_profile_row,
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.AboutMe.route)
            },
        )
        SelectionRow(title = "Change Password", leadingIcon = R.drawable.ic_change_password_row)
        SelectionRow(title = "Payment Methods", leadingIcon = R.drawable.ic_payment_methods_row)
        SelectionRow(title = "Support", leadingIcon = R.drawable.ic_support_row)
        SelectionRow(title = "Login Options", leadingIcon = R.drawable.ic_login_options_row)
        Spacer(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
        )
        SelectionRow(
            title = "Logout",
            leadingIcon = R.drawable.ic_logout_row,
            onClick = {
                viewModel.logOut(
                    success = {
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            route = ProfileScreenRoute.Welcome.route,
                            rootRoute = ProfileScreenRoute.Welcome.route
                        )
                    },
                    onFailed = {
                        // Stub.
                        Log.e("ViewMyProfile", "Logout failed")
                    }
                )
            },
        )
    }

    viewModel.getAccountInfo(
        success = {
            Log.d("ViewMyProfile", "Get account info success")
        },
        onFailed = {
            Log.e("ViewMyProfile", "Get account info failed")
        }
    )
}

@Composable
fun UserHead(
    id: String,
    firstName: String,
    lastName: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    textStyle: TextStyle = MaterialTheme.typography.headlineLarge,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val color = remember(id, firstName, lastName) {
            val name = listOf(firstName, lastName)
                .joinToString(separator = "")
                .uppercase()
            Color("$id / $name".toHslColor())
        }
        val initials = (firstName.take(1) + lastName.take(1)).uppercase()
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(SolidColor(color))
        }
        Text(text = initials, style = textStyle, color = Color.White)
    }
}

@Composable
fun SelectionRow(title: String, leadingIcon: Int, onClick: () -> Unit = {}) {
    Column(Modifier.clickable {
        onClick()
    }
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .background(Color.White)
                .height(48.dp)
                .padding(start = 22.dp, end = 22.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = leadingIcon),
                    contentDescription = stringResource(id = R.string.app_configuration),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(title)
            }
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd),
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "",
                tint = Color.Black
            )
        }
        BottomShadow(alpha = .15f, height = 4.dp)
    }
}

@Composable
fun BottomShadow(alpha: Float = 0.1f, height: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = alpha),
                        Color.Transparent,
                    )
                )
            )
    )
}

@ColorInt
fun String.toHslColor(saturation: Float = 0.5f, lightness: Float = 0.4f): Int {
    val hue = fold(0) { acc, char -> char.code + acc * 37 } % 360
    return ColorUtils.HSLToColor(floatArrayOf(hue.absoluteValue.toFloat(), saturation, lightness))
}

@Preview
@Composable
fun ViewMyProfilePreview() {
    ViewMyProfile(ViewModelAuthenticationPreview())
}