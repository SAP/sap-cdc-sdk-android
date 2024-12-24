package com.sap.cdc.bitsnbytes.ui.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.cdc.IdentityServiceRepository
import com.sap.cdc.bitsnbytes.ui.view.custom.AuthenticationTabView
import com.sap.cdc.bitsnbytes.ui.view.flow.AboutMeView
import com.sap.cdc.bitsnbytes.ui.view.flow.EmailRegisterView
import com.sap.cdc.bitsnbytes.ui.view.flow.EmailSignInView
import com.sap.cdc.bitsnbytes.ui.view.flow.HomeView
import com.sap.cdc.bitsnbytes.ui.view.flow.LinkAccountView
import com.sap.cdc.bitsnbytes.ui.view.flow.LoginOptionsView
import com.sap.cdc.bitsnbytes.ui.view.flow.MyProfileView
import com.sap.cdc.bitsnbytes.ui.view.flow.OTPType
import com.sap.cdc.bitsnbytes.ui.view.flow.OtpSignInView
import com.sap.cdc.bitsnbytes.ui.view.flow.OtpVerifyView
import com.sap.cdc.bitsnbytes.ui.view.flow.PendingRegistrationView
import com.sap.cdc.bitsnbytes.ui.view.flow.RegisterView
import com.sap.cdc.bitsnbytes.ui.view.flow.ScreenSetView
import com.sap.cdc.bitsnbytes.ui.view.flow.SignInView
import com.sap.cdc.bitsnbytes.ui.view.flow.WelcomeView
import com.sap.cdc.bitsnbytes.ui.viewmodel.AboutMeViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailRegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LoginOptionsViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.MyProfileViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpVerifyViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.PendingRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.ScreenSetViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel
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

    NavHost(
        profileNavController, startDestination =
        when (IdentityServiceRepository.getInstance(LocalContext.current).availableSession()) {
            true -> ProfileScreenRoute.MyProfile.route
            false -> ProfileScreenRoute.Welcome.route
        }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            WelcomeView(viewModel = WelcomeViewModel(LocalContext.current))
        }
        composable(ProfileScreenRoute.SignIn.route) {
            SignInView(viewModel = SignInViewModel(LocalContext.current))
        }
        composable(ProfileScreenRoute.Register.route) {
            RegisterView(viewModel = RegisterViewModel(LocalContext.current))
        }
        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(selected = selected!!.toInt(),)
        }
        composable(ProfileScreenRoute.EmailSignIn.route) {
            EmailSignInView(viewModel = EmailSignInViewModel(LocalContext.current))
        }
        composable(ProfileScreenRoute.EmailRegister.route) {
            EmailRegisterView(viewModel = EmailRegisterViewModel(LocalContext.current))
        }
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            PendingRegistrationView(
                viewModel = PendingRegistrationViewModel(LocalContext.current),
                resolvable,
            )
        }
        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            LinkAccountView(
                viewModel = LinkAccountViewModel(LocalContext.current),
                resolvable,
            )
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            MyProfileView(viewModel = MyProfileViewModel(LocalContext.current))
        }
        composable(ProfileScreenRoute.AboutMe.route) {
            AboutMeView(viewModel = AboutMeViewModel(LocalContext.current))
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route) {
            ScreenSetView(
                ScreenSetViewModel(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
            ScreenSetView(
                ScreenSetViewModel(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-register-screen"
            )
        }
        composable("${ProfileScreenRoute.OTPSignIn.route}/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            OtpSignInView(viewModel = OtpSignInViewModel(LocalContext.current), otpType = otpType!!)
        }
        composable("${ProfileScreenRoute.OTPVerify.route}/{resolvableContext}/{type}/{inputField}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val input = backStackEntry.arguments?.getString("inputField")
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            OtpVerifyView(
                viewModel = OtpVerifyViewModel(LocalContext.current),
                resolvable,
                otpType = otpType!!,
                inputField = input!!
            )
        }
        composable(ProfileScreenRoute.LoginOptions.route) {
            LoginOptionsView(viewModel = LoginOptionsViewModel(LocalContext.current))
        }
    }
}

