package com.sap.cdc.android.sdk.example.ui.route

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.example.ui.view.AuthenticationTabView
import com.sap.cdc.android.sdk.example.ui.view.ResolvePendingRegistrationWithMissingFields
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


@Composable
fun HomeNavHost() {
    val homeNavController = rememberNavController()
    NavHost(homeNavController, startDestination = "home1") {
        composable("home1") {
            ViewHome()
        }
    }
}

@Composable
fun SearchNavHost() {
    val searchNavController = rememberNavController()
    NavHost(searchNavController, startDestination = "search1") {
        composable("search1") {
            Text(MainScreenRoute.Search.route)
        }
    }
}

@Composable
fun CartNavHost() {
    val cartNavController = rememberNavController()
    NavHost(cartNavController, startDestination = "cart1") {
        composable("cart1") {
            Text(MainScreenRoute.Cart.route)
        }
    }
}

@Composable
fun FavoritesNavHost() {
    val favoritesNavController = rememberNavController()
    NavHost(favoritesNavController, startDestination = "favorites1") {
        composable("favorites1") {
            Text(MainScreenRoute.Favorites.route)
        }
    }
}

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
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{missingFields}") { backStackEntry ->
            val missingFieldsJson = backStackEntry.arguments?.getString("missingFields")
            val missingFields = Json.decodeFromString<List<String>>(missingFieldsJson!!)
            ResolvePendingRegistrationWithMissingFields(
                viewModel = ViewModelAuthentication(context),
                missingFields
            )
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            ViewMyProfile(viewModel = ViewModelProfile(context))
        }
        composable(ProfileScreenRoute.AboutMe.route) {
            ViewAboutMe(viewModel = ViewModelProfile(context))
        }
        composable(ScreenSetsRoute.ResistrationLogin_Login.route) {
            ViewScreenSet(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }
        composable(ScreenSetsRoute.RegistrationLogin_Register.route) {
            ViewScreenSet(
                ViewModelScreenSet(LocalContext.current),
                "Default-RegistrationLogin",
                "gigya-register-screen"
            )
        }
    }
}

