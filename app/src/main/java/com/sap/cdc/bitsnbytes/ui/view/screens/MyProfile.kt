package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomColoredSizeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.UserHead
import kotlin.math.absoluteValue

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Profile view containing basic account information available from the SDK.
 * Addition authentication flows can be initiated from here when the application contains a valid
 * session.
 */

val profileGradientSops = arrayOf(
    0.0f to Color(0xFFF57EA4),
    0.2f to Color(0xFF7C5AD0),
    1f to Color(0xFF6764E1)
)

@ColorInt
fun String.toHslColor(saturation: Float = 0.5f, lightness: Float = 0.4f): Int {
    val hue = fold(0) { acc, char -> char.code + acc * 37 } % 360
    return ColorUtils.HSLToColor(floatArrayOf(hue.absoluteValue.toFloat(), saturation, lightness))
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

// Constants for better maintainability
private object ProfileConstants {
    val HEADER_HEIGHT = 180.dp
    val GRADIENT_HEIGHT = 80.dp
    val WELCOME_BOX_HEIGHT = 44.dp
    val WELCOME_TEXT_SIZE = 34.sp
    val SPACER_HEIGHT = 40.dp
    val SECTION_SPACER_HEIGHT = 24.dp
}

/**
 * Profile view representation with StateFlow observation and pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileView(viewModel: IMyProfileViewModel) {
    // Observe account state from AuthenticationFlowDelegate
    val accountInfo by viewModel.flowDelegate?.userAccount?.collectAsState() ?: remember { mutableStateOf(null) }

    var loading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Derived state for better performance
    val fullName by remember {
        derivedStateOf {
            val firstName = accountInfo?.profile?.firstName ?: ""
            val lastName = accountInfo?.profile?.lastName ?: ""
            "$firstName $lastName".trim()
        }
    }

    // Load account info after the view is fully rendered to avoid navigation cancellation
    LaunchedEffect(viewModel) {
        if (accountInfo == null) {
            // Add a small delay to ensure navigation has completed and view is fully rendered
            kotlinx.coroutines.delay(100) // 100ms delay to allow navigation to settle
            
            loading = true
            viewModel.getAccountInfo(mutableMapOf()) {
                onSuccess = {
                    loading = false
                    isRefreshing = false
                }
                onError = { error ->
                    loading = false
                    isRefreshing = false
                }
            }
        } else {
            // Account info already available, no need to load
            loading = false
            isRefreshing = false
        }
    }

    LoadingStateColumn(
        loading = loading,
        modifier = Modifier
            .background(AppTheme.colorScheme.background)
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                try {
                    viewModel.getAccountInfo {
                        onSuccess = {
                            isRefreshing = false
                            loading = false
                        }
                        onError = {
                            isRefreshing = false
                            loading = false
                        }
                    }
                } catch (e: Exception) {
                    // Ensure refresh state is always dismissed even if getAccountInfo throws
                    isRefreshing = false
                    loading = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(
                    firstName = accountInfo?.profile?.firstName ?: "",
                    lastName = accountInfo?.profile?.lastName ?: ""
                )

                WelcomeSection(fullName = fullName)

                CustomColoredSizeVerticalSpacer(ProfileConstants.SPACER_HEIGHT, Color.White)

                ProfileMenuSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    firstName: String,
    lastName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileConstants.HEADER_HEIGHT)
    ) {
        Column {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileConstants.GRADIENT_HEIGHT)
                    .background(
                        brush = Brush.verticalGradient(colorStops = profileGradientSops)
                    )
            )
            // White background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileConstants.GRADIENT_HEIGHT)
                    .background(color = Color.White)
            )
        }

        UserHead(
            id = "",
            firstName = firstName,
            lastName = lastName,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun WelcomeSection(fullName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        if (fullName.isNotBlank()) {
            // Show "Welcome," and name when names are available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileConstants.WELCOME_BOX_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Welcome,",
                    style = AppTheme.typography.body
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileConstants.WELCOME_BOX_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName,
                    fontSize = ProfileConstants.WELCOME_TEXT_SIZE,
                    fontWeight = FontWeight.Bold,
                    style = AppTheme.typography.titleLarge
                )
            }
        } else {
            // Show just "Welcome" without comma when names are not available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileConstants.WELCOME_BOX_HEIGHT * 2), // Use double height to center properly
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Welcome",
                    style = AppTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuSection(viewModel: IMyProfileViewModel) {
    Column {
        // Profile menu items
        SelectionRow(
            title = "My Orders",
            leadingIcon = R.drawable.ic_cart_row
        )

        SelectionRow(
            title = "About Me",
            leadingIcon = R.drawable.ic_profile_row,
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.AboutMe.route)
            }
        )

        SelectionRow(
            title = "Change Password",
            leadingIcon = R.drawable.ic_change_password_row
        )

        SelectionRow(
            title = "Payment Methods",
            leadingIcon = R.drawable.ic_payment_methods_row
        )

        SelectionRow(
            title = "Support",
            leadingIcon = R.drawable.ic_support_row
        )

        SelectionRow(
            title = "Login Options",
            leadingIcon = R.drawable.ic_login_options_row,
            onClick = {
                NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.LoginOptions.route)
            }
        )

        MediumVerticalSpacer()

        SelectionRow(
            title = "Logout",
            leadingIcon = R.drawable.ic_logout_row,
            onClick = {
                handleLogout(viewModel)
            }
        )
    }
}

private fun handleLogout(viewModel: IMyProfileViewModel) {
    viewModel.logOut {
        onSuccess = {
            // Navigate to Welcome screen and clear the profile navigation stack
            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.Welcome.route) {
                // Clear the entire profile navigation stack
                popUpTo(ProfileScreenRoute.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        }
        onError = { error ->
            // Even on error, navigate to Welcome screen and clear the profile navigation stack
            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.Welcome.route) {
                // Clear the entire profile navigation stack
                popUpTo(ProfileScreenRoute.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

// Constants for SelectionRow
private object SelectionRowConstants {
    val ROW_HEIGHT = 48.dp
    val HORIZONTAL_PADDING = 22.dp
    val ICON_SIZE = 24.dp
    val ARROW_ICON_SIZE = 14.dp
    val ICON_SPACING = 20.dp
    val SHADOW_HEIGHT = 4.dp
    val SHADOW_ALPHA = 0.15f
    val CORNER_RADIUS = 8.dp
    val PRESSED_ALPHA = 0.08f
}

@Composable
fun SelectionRow(
    title: String,
    @DrawableRes leadingIcon: Int,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SelectionRowConstants.CORNER_RADIUS))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        color = AppTheme.colorScheme.primary
                    ),
                    onClick = onClick
                ),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(SelectionRowConstants.ROW_HEIGHT)
                    .padding(horizontal = SelectionRowConstants.HORIZONTAL_PADDING)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(SelectionRowConstants.ICON_SIZE),
                        painter = painterResource(id = leadingIcon),
                        contentDescription = null,
                        tint = Color.Black
                    )

                    Spacer(modifier = Modifier.width(SelectionRowConstants.ICON_SPACING))

                    Text(
                        text = title,
                        style = AppTheme.typography.body,
                        color = Color.Black
                    )
                }

                Icon(
                    modifier = Modifier
                        .size(SelectionRowConstants.ARROW_ICON_SIZE)
                        .align(Alignment.CenterEnd),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Navigate",
                    tint = Color.Black.copy(alpha = 0.6f)
                )
            }
        }

        BottomShadow(
            alpha = SelectionRowConstants.SHADOW_ALPHA,
            height = SelectionRowConstants.SHADOW_HEIGHT
        )
    }
}

@Preview
@Composable
fun MyProfileViewPreview() {
    AppTheme {
        MyProfileView(MyProfileViewModelPreview())
    }
}

@Preview
@Composable
fun SelectionRowPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .padding(16.dp)
        ) {
            SelectionRow(
                title = "About Me",
                leadingIcon = R.drawable.ic_profile_row,
                onClick = { /* Preview action */ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SelectionRow(
                title = "Login Options",
                leadingIcon = R.drawable.ic_login_options_row,
                onClick = { /* Preview action */ }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SelectionRow(
                title = "Logout",
                leadingIcon = R.drawable.ic_logout_row,
                onClick = { /* Preview action */ }
            )
        }
    }
}
