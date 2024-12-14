package com.sap.cdc.android.sdk.example.ui.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.example.ui.view.custom.AuthenticationTabView
import com.sap.cdc.android.sdk.example.ui.view.flow.OTPType
import com.sap.cdc.android.sdk.example.ui.view.flow.LinkAccountResolvableView
import com.sap.cdc.android.sdk.example.ui.view.flow.PendingRegistrationResolvableView
import com.sap.cdc.android.sdk.example.ui.view.flow.EmailSignInView
import com.sap.cdc.android.sdk.example.ui.view.flow.AboutMeView
import com.sap.cdc.android.sdk.example.ui.view.flow.EmailRegisterView
import com.sap.cdc.android.sdk.example.ui.view.flow.HomeView
import com.sap.cdc.android.sdk.example.ui.view.flow.MyProfileView
import com.sap.cdc.android.sdk.example.ui.view.flow.OtpSignInView
import com.sap.cdc.android.sdk.example.ui.view.flow.OtpVerifyView
import com.sap.cdc.android.sdk.example.ui.view.flow.RegisterView
import com.sap.cdc.android.sdk.example.ui.view.flow.ScreenSetView
import com.sap.cdc.android.sdk.example.ui.view.flow.SignInView
import com.sap.cdc.android.sdk.example.ui.view.flow.WelcomeView
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelScreenSet
import kotlinx.serialization.json.Json

/**
 * Home tab navigation host.
 */
@Composable
fun HomeNavHost() {
    val homeNavController = rememberNavController()
    NavHost(homeNavController, startDestination = "home1") {
        composable("home1") {
            HomeView()
        }
    }
}

/**
 * Search tab navigation host.
 */
@Composable
fun SearchNavHost() {
    val searchNavController = rememberNavController()
    NavHost(searchNavController, startDestination = "search1") {
        composable("search1") {
            Text(MainScreenRoute.Search.route)
        }
    }
}

/**
 * Cart tab navigation host.
 */
@Composable
fun CartNavHost() {
    val cartNavController = rememberNavController()
    NavHost(cartNavController, startDestination = "cart1") {
        composable("cart1") {
            Text(MainScreenRoute.Cart.route)
        }
    }
}

/**
 * Favorites tab navigation host.
 */
@Composable
fun FavoritesNavHost() {
    val favoritesNavController = rememberNavController()
    NavHost(favoritesNavController, startDestination = "favorites1") {
        composable("favorites1") {
            Text(MainScreenRoute.Favorites.route)
        }
    }
}

/**
 * Profile tab navigation host.
 */
@Composable
fun ProfileNavHost() {
    val profileNavController = rememberNavController()
    NavigationCoordinator.INSTANCE.setNavController(profileNavController)

    val authenticationViewModel = ViewModelAuthentication(LocalContext.current)
    val isLoggedIn = authenticationViewModel.validSession()

    NavHost(
        profileNavController, startDestination =
        when (isLoggedIn) {
            true -> ProfileScreenRoute.MyProfile.route
            false -> ProfileScreenRoute.Welcome.route
        }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            WelcomeView(
                viewModel = authenticationViewModel,
            )
        }
        composable(ProfileScreenRoute.SignIn.route) {
            SignInView(
                viewModel = authenticationViewModel
            )
        }
        composable(ProfileScreenRoute.Register.route) {
            RegisterView(
                viewModel = authenticationViewModel
            )
        }
        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(
                viewModel = authenticationViewModel,
                selected = selected!!.toInt(),
            )
        }
        composable(ProfileScreenRoute.EmailSignIn.route) {
            EmailSignInView(viewModel = authenticationViewModel)
        }
        composable(ProfileScreenRoute.EmailRegister.route) {
            EmailRegisterView(viewModel = authenticationViewModel)
        }
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            PendingRegistrationResolvableView(
                viewModel = authenticationViewModel,
                resolvable,
            )
        }
        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            LinkAccountResolvableView(
                viewModel = authenticationViewModel,
                resolvable,
            )
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            MyProfileView(viewModel = authenticationViewModel)
        }
        composable(ProfileScreenRoute.AboutMe.route) {
            AboutMeView(viewModel = authenticationViewModel)
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route) {
            ScreenSetView(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
            ScreenSetView(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-register-screen"
            )
        }
        composable("${ProfileScreenRoute.OTPSignIn.route}/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            OtpSignInView(viewModel = authenticationViewModel, otpType = otpType!!)
        }
        composable("${ProfileScreenRoute.OTPVerify.route}/{resolvableContext}/{type}/{inputField}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val input = backStackEntry.arguments?.getString("inputField")
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            OtpVerifyView(
                viewModel = authenticationViewModel,
                resolvable,
                otpType = otpType!!,
                inputField = input!!
            )
        }
    }
}

