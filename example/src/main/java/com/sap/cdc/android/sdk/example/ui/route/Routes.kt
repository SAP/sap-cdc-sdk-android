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
    object Home : MainScreenRoute("Home", R.string.home, R.drawable.ic_home)
    object Search : MainScreenRoute("Search", R.string.search, R.drawable.ic_search)
    object Cart : MainScreenRoute("Cart", R.string.cart, R.drawable.ic_cart)
    object Favorites : MainScreenRoute("Favorites", R.string.favorites, R.drawable.ic_favorites)
    object Profile : MainScreenRoute("Profile", R.string.profile, R.drawable.ic_profile)
    object Configuration : MainScreenRoute("App Configuration", R.string.app_configuration, -1)
}

sealed class ProfileScreenRoute(
    val route: String,
    @StringRes val resourceId: Int
) {
    object Welcome : ProfileScreenRoute("Welcome", R.string.welcome)
    object AuthTabView: ProfileScreenRoute("AuthTabView", -1)
    object Register : ProfileScreenRoute("Register", R.string.register)
    object Login: ProfileScreenRoute("Login", R.string.login)
    object MyProfile : ProfileScreenRoute("MyProfile", R.string.my_profile)
}


