package com.sap.cdc.android.sdk.example.ui.route

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sap.cdc.android.sdk.example.R

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

sealed class MainScreenRoute(
    val route: String,
    @StringRes val resourceId: Int,
    @DrawableRes val iconID: Int
) {
    data object Home : MainScreenRoute("Home", R.string.home, R.drawable.ic_home)
    data object Search : MainScreenRoute("Search", R.string.search, R.drawable.ic_search)
    data object Cart : MainScreenRoute("Cart", R.string.cart, R.drawable.ic_cart)
    data object Favorites : MainScreenRoute("Favorites", R.string.favorites, R.drawable.ic_favorites)
    data object Profile : MainScreenRoute("Profile", R.string.profile, R.drawable.ic_profile)
    data object Configuration : MainScreenRoute("App Configuration", R.string.app_configuration, -1)
}

sealed class ProfileScreenRoute(
    val route: String,
) {
    data object Welcome : ProfileScreenRoute("Welcome")
    data object AuthTabView : ProfileScreenRoute("AuthTabView")
    data object Register : ProfileScreenRoute("Register")
    data object EmailSignIn : ProfileScreenRoute("EmailSignIn")
    data object MyProfile : ProfileScreenRoute("MyProfile")
    data object AboutMe : ProfileScreenRoute("AboutMe")
    data object ResolvePendingRegistration : ProfileScreenRoute("ResolvePendingRegistration",)
    data object ResolveLinkAccount : ProfileScreenRoute("ResolveLinkAccount")
    data object OTPSignIn : ProfileScreenRoute("OTPSignIn")
    data object OTPVerify : ProfileScreenRoute("OTPVerify")
}

sealed class ScreenSetsRoute(
    val route: String
) {
    data object ScreenSetRegistrationLoginRegister : ScreenSetsRoute("Register")
    data object ScreenSetRegistrationLoginLogin : ScreenSetsRoute("Login")
}


