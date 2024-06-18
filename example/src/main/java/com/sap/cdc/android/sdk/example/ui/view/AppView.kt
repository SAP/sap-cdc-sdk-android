package com.sap.cdc.android.sdk.example.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.MainScreenRoute
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.viewmodel.ConfigurationViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.HomeViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.MyProfileViewModel

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AppView() {
    var titleText by remember { mutableStateOf("") }
    titleText = stringResource(id = MainScreenRoute.Home.resourceId)

    val rootNavController = rememberNavController()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.shadow(elevation = 4.dp),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
                title = {
                    Text(titleText.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (NavigationCoordinator.INSTANCE.backNav.collectAsState().value) {
                        IconButton(onClick = { NavigationCoordinator.INSTANCE.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            NavigationCoordinator.INSTANCE.setNavController(rootNavController)
                            NavigationCoordinator.INSTANCE.navigate(MainScreenRoute.Configuration.route) {
                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_configuration),
                            contentDescription = stringResource(id = R.string.app_configuration),
                            tint = Color.Black
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    bottomAppBarItems.forEach { item ->
                        IconButton(onClick = {
                            rootNavController.navigate(item.route) {
                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = item.iconID),
                                contentDescription = stringResource(id = item.resourceId),
                                tint = Color.Unspecified
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(rootNavController, startDestination = MainScreenRoute.Home.route) {
                composable(MainScreenRoute.Home.route) {
                    HomeNavHost()
                    titleText = stringResource(id = MainScreenRoute.Home.resourceId)
                }
                composable(MainScreenRoute.Search.route) {
                    SearchNavHost()
                    titleText = stringResource(id = MainScreenRoute.Search.resourceId)
                }
                composable(MainScreenRoute.Cart.route) {
                    CartNavHost()
                    titleText = stringResource(id = MainScreenRoute.Cart.resourceId)
                }
                composable(MainScreenRoute.Favorites.route) {
                    FavoritesNavHost()
                    titleText = stringResource(id = MainScreenRoute.Favorites.resourceId)
                }
                composable(MainScreenRoute.Profile.route) {
                    ProfileNavHost()
                    titleText = stringResource(id = MainScreenRoute.Profile.resourceId)
                }
                composable(MainScreenRoute.Configuration.route) {
                    ConfigurationView(viewModel = ConfigurationViewModel(LocalContext.current))
                    titleText = stringResource(id = MainScreenRoute.Configuration.resourceId)
                }
            }
        }
    }
}

val bottomAppBarItems = listOf(
    MainScreenRoute.Home,
    MainScreenRoute.Search,
    MainScreenRoute.Cart,
    MainScreenRoute.Favorites,
    MainScreenRoute.Profile
)

@Composable
fun HomeNavHost() {
    val homeNavController = rememberNavController()
    NavHost(homeNavController, startDestination = "home1") {
        composable("home1") {
            HomeView()
        }
    }
}

@Composable
fun SearchNavHost() {
    val searchNavController = rememberNavController()
    NavHost(searchNavController, startDestination = "search1") {
        composable("search1") {
            //TEST
            ScreenSetView()
            //Text(MainScreenRoute.Search.route)
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
    val viewModel = HomeViewModel(context)
    val isLoggedIn = viewModel.validSession()

    NavHost(
        profileNavController, startDestination =
        when (isLoggedIn) {
            true -> ProfileScreenRoute.MyProfile.route
            false -> ProfileScreenRoute.Welcome.route
        }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            WelcomeView(navController = profileNavController)
        }
        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(
                selected = selected!!.toInt()
            )
        }
        composable(ProfileScreenRoute.MyProfile.route) {
            MyProfileView(MyProfileViewModel(LocalContext.current))
        }
    }
}

