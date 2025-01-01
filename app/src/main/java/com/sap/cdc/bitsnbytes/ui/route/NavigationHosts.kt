package com.sap.cdc.bitsnbytes.ui.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.cdc.IdentityServiceRepository
import com.sap.cdc.bitsnbytes.ui.view.composables.AuthenticationTabView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailRegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.HomeView
import com.sap.cdc.bitsnbytes.ui.view.screens.LinkAccountView
import com.sap.cdc.bitsnbytes.ui.view.screens.LoginOptionsView
import com.sap.cdc.bitsnbytes.ui.view.screens.MyProfileView
import com.sap.cdc.bitsnbytes.ui.view.screens.OTPType
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpVerifyView
import com.sap.cdc.bitsnbytes.ui.view.screens.PendingRegistrationView
import com.sap.cdc.bitsnbytes.ui.view.screens.RegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.ScreenSetView
import com.sap.cdc.bitsnbytes.ui.view.screens.SignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.WelcomeView
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailRegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LoginOptionsViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpVerifyViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.PendingRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.ScreenSetViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.factory.CustomViewModelFactory
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

    val context = LocalContext.current.applicationContext
    val identityServiceRepository = IdentityServiceRepository.getInstance(context)

    NavHost(
        profileNavController, startDestination =
        when (identityServiceRepository.availableSession()) {
            true -> ProfileScreenRoute.MyProfile.route
            false -> ProfileScreenRoute.Welcome.route
        }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            val viewModel: WelcomeViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            WelcomeView(viewModel)
        }
        composable(ProfileScreenRoute.SignIn.route) {
            val viewModel: SignInViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            SignInView(
                viewModel
            )
        }
        composable(ProfileScreenRoute.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            RegisterView(viewModel)
        }
        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(selected = selected!!.toInt())
        }
        composable(ProfileScreenRoute.EmailSignIn.route) {
            val viewModel: EmailSignInViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            EmailSignInView(viewModel)
        }
        composable(ProfileScreenRoute.EmailRegister.route) {
            val viewModel: EmailRegisterViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            EmailRegisterView(viewModel)
        }
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: PendingRegistrationViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            PendingRegistrationView(viewModel, resolvable)
        }
        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: LinkAccountViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            LinkAccountView(viewModel, resolvable)
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            val viewModel: AccountViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            MyProfileView(viewModel)
        }
        composable(ProfileScreenRoute.AboutMe.route) {
            val viewModel: AccountViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            AboutMeView(viewModel)
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route) {
            val viewModel: ScreenSetViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            ScreenSetView(
                viewModel,
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
            val viewModel: ScreenSetViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            ScreenSetView(
                viewModel,
                "Default-RegistrationLogin",
                "gigya-register-screen"
            )
        }
        composable("${ProfileScreenRoute.OTPSignIn.route}/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val viewModel: OtpSignInViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            OtpSignInView(viewModel, otpType = otpType!!)
        }
        composable("${ProfileScreenRoute.OTPVerify.route}/{resolvableContext}/{type}/{inputField}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val input = backStackEntry.arguments?.getString("inputField")
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: OtpVerifyViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            OtpVerifyView(
                viewModel,
                resolvable,
                otpType = otpType!!,
                inputField = input!!
            )
        }
        composable(ProfileScreenRoute.LoginOptions.route) {
            val viewModel: LoginOptionsViewModel = viewModel(
                factory = CustomViewModelFactory(context)
            )
            LoginOptionsView(viewModel)
        }
    }
}

