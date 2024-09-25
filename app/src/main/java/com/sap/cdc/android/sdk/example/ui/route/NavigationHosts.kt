package com.sap.cdc.android.sdk.example.ui.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
import com.sap.cdc.android.sdk.example.ui.view.AuthenticationTabView
import com.sap.cdc.android.sdk.example.ui.view.ResolveLinkAccount
import com.sap.cdc.android.sdk.example.ui.view.ResolvePendingRegistrationWithMissingFields
import com.sap.cdc.android.sdk.example.ui.view.SignInWithEmailView
import com.sap.cdc.android.sdk.example.ui.view.ViewAboutMe
import com.sap.cdc.android.sdk.example.ui.view.ViewHome
import com.sap.cdc.android.sdk.example.ui.view.ViewMyProfile
import com.sap.cdc.android.sdk.example.ui.view.ViewScreenSet
import com.sap.cdc.android.sdk.example.ui.view.ViewWelcome
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelHome
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelProfile
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
            ViewHome()
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

    val context = LocalContext.current
    val viewModel = ViewModelHome(context)
    val isLoggedIn = viewModel.validSession()

    NavHost(
        profileNavController, startDestination =
        when (isLoggedIn) {
            true -> ProfileScreenRoute.MyProfile.route
            false -> ProfileScreenRoute.Welcome.route
        }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            ViewWelcome()
        }
        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(
                viewModel = ViewModelAuthentication(context),
                selected = selected!!.toInt(),
            )
        }
        composable(ProfileScreenRoute.EmailSignIn.route) {
            SignInWithEmailView(viewModel = ViewModelAuthentication(context))
        }
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{missingFields}/{regToken}") { backStackEntry ->
            val missingFieldsJson = backStackEntry.arguments?.getString("missingFields")
            val missingFields = Json.decodeFromString<List<String>>(missingFieldsJson!!)
            val regToken = backStackEntry.arguments?.getString("regToken")
            ResolvePendingRegistrationWithMissingFields(
                viewModel = ViewModelAuthentication(context),
                missingFields,
                regToken!!
            )
        }
        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{conflictingAccounts}/{regToken}") { backStackEntry ->
            val conflictingAccountsJson = backStackEntry.arguments?.getString("conflictingAccounts")
            val conflictingAccounts =
                Json.decodeFromString<ConflictingAccountsEntity>(conflictingAccountsJson!!)
            val regToken = backStackEntry.arguments?.getString("regToken")
            ResolveLinkAccount(
                viewModel = ViewModelAuthentication(context),
                conflictingAccounts,
                regToken!!
            )
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            ViewMyProfile(viewModel = ViewModelProfile(context))
        }
        composable(ProfileScreenRoute.AboutMe.route) {
            ViewAboutMe(viewModel = ViewModelProfile(context))
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route) {
            ViewScreenSet(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }
        composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
            ViewScreenSet(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-register-screen"
            )
        }
    }
}

