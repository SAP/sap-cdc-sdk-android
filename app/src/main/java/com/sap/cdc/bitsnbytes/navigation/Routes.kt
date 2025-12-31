package com.sap.cdc.bitsnbytes.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sap.cdc.bitsnbytes.R

/**
 * Type-safe navigation route definitions for the application.
 * 
 * Provides sealed classes for compile-time checked navigation routes, preventing
 * typos and making route refactoring safer.
 * 
 * ## Usage
 * ```kotlin
 * // Navigate to a main screen
 * navCoordinator.navigate(MainScreenRoute.Home.route)
 * 
 * // Navigate to authentication flow
 * navCoordinator.navigate(ProfileScreenRoute.SignIn.route)
 * 
 * // Navigate to ScreenSet
 * navCoordinator.navigate(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route)
 * ```
 * 
 * ## Bottom Navigation
 * ```kotlin
 * BottomNavigationBar(
 *     items = listOf(
 *         MainScreenRoute.Home,
 *         MainScreenRoute.Search,
 *         MainScreenRoute.Cart
 *     ),
 *     onItemClick = { route -> 
 *         navCoordinator.navigate(route.route)
 *     }
 * )
 * ```
 * 
 * @see NavigationCoordinator
 */

/**
 * Main application screen routes for bottom navigation.
 * 
 * @property route Navigation route string
 * @property resourceId String resource ID for the screen title
 * @property iconID Drawable resource ID for the navigation icon
 */
sealed class MainScreenRoute(
    val route: String,
    @param:StringRes val resourceId: Int,
    @param:DrawableRes val iconID: Int
) {
    data object Home : MainScreenRoute("Home", R.string.home, R.drawable.ic_home)
    data object Search : MainScreenRoute("Search", R.string.search, R.drawable.ic_search)
    data object Cart : MainScreenRoute("Cart", R.string.cart, R.drawable.ic_cart)
    data object Favorites : MainScreenRoute("Favorites", R.string.favorites, R.drawable.ic_favorites)
    data object Profile : MainScreenRoute("Profile", R.string.profile, R.drawable.ic_profile)
    data object Configuration : MainScreenRoute("App Configuration", R.string.app_configuration, -1)
}

/**
 * Profile and authentication screen routes.
 * 
 * Contains all routes for the authentication flow including sign-in,
 * registration, profile management, and multi-factor authentication.
 * 
 * @property route Navigation route string
 */
sealed class ProfileScreenRoute(
    val route: String,
) {
    data object Welcome : ProfileScreenRoute("Welcome")
    data object SignIn : ProfileScreenRoute("SignIn")
    data object Register : ProfileScreenRoute("Register")
    data object AuthTabView : ProfileScreenRoute("AuthTabView")
    data object EmailSignIn : ProfileScreenRoute("EmailSignIn")
    data object CustomIdSignIn : ProfileScreenRoute("CustomIdSignIn")
    data object EmailRegister : ProfileScreenRoute("EmailRegister")
    data object MyProfile : ProfileScreenRoute("MyProfile")
    data object AboutMe : ProfileScreenRoute("AboutMe")
    data object ResolvePendingRegistration : ProfileScreenRoute("ResolvePendingRegistration")
    data object ResolveLinkAccount : ProfileScreenRoute("ResolveLinkAccount")
    data object OTPSignIn : ProfileScreenRoute("OTPSignIn")
    data object OTPVerify : ProfileScreenRoute("OTPVerify")
    data object LoginOptions : ProfileScreenRoute("LoginOptions")
    data object AuthMethods : ProfileScreenRoute("AuthMethods")
    data object PhoneSelection : ProfileScreenRoute("PhoneSelection")
    data object PhoneVerification : ProfileScreenRoute("PhoneVerification")
    data object TOTPVerification : ProfileScreenRoute("TOTPVerification")
    data object BiometricLocked : ProfileScreenRoute("BiometricLocked")
    data object PasskeysCredentials : ProfileScreenRoute("PasskeysCredentials")
}

/**
 * Web-based ScreenSet routes.
 * 
 * Routes for CIAM ScreenSets rendered in WebView for authentication UI.
 * 
 * @property route Navigation route string
 */
sealed class ScreenSetsRoute(
    val route: String
) {
    data object ScreenSetRegistrationLoginRegister : ScreenSetsRoute("ScreenSet_Register")
    data object ScreenSetRegistrationLoginLogin : ScreenSetsRoute("ScreenSet_Login")
}
