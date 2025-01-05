package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.ui.route.CartNavHost
import com.sap.cdc.bitsnbytes.ui.route.FavoritesNavHost
import com.sap.cdc.bitsnbytes.ui.route.HomeNavHost
import com.sap.cdc.bitsnbytes.ui.route.MainScreenRoute
import com.sap.cdc.bitsnbytes.ui.route.NavigationCoordinator
import com.sap.cdc.bitsnbytes.ui.route.ProfileNavHost
import com.sap.cdc.bitsnbytes.ui.route.SearchNavHost
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomBottomBar
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.viewmodel.ConfigurationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.factory.CustomViewModelFactory

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Main landing page view containing bottom bar navigation flows.
 */

@Preview
@Composable
fun HomeScaffoldViewPreview() {
    AppTheme {
        HomeScaffoldView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffoldView() {
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
                    Text(
                        titleText.uppercase(),
                        style = AppTheme.typography.topBar
                    )
                },
                navigationIcon = {
                    if (NavigationCoordinator.INSTANCE.backNav.collectAsState().value) {
                        IconButton(onClick = {
                            NavigationCoordinator.INSTANCE.navigateUp()
                        }) {
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
            CustomBottomBar(
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
                    val viewModel: ConfigurationViewModel = viewModel(
                        factory = CustomViewModelFactory(LocalContext.current)
                    )
                    ConfigurationView(viewModel)
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

@Preview
@Composable
fun HomeViewPreview() {
    AppTheme {
        HomeView()
    }
}

@Composable
fun HomeView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        MediumVerticalSpacer()

        Text("The all-new", style = AppTheme.typography.titleNormalLight)
        Text("MacBook Pro", style = AppTheme.typography.titleLarge)
        Text("with Retina display", style = AppTheme.typography.body)

        MediumVerticalSpacer()

        ActionOutlineButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            text = "Buy Now",
            onClick = {}
        )

        Image(
            modifier = Modifier.size(width = 200.dp, height = 200.dp),
            painter = painterResource(id = R.drawable.img_home_macbook),
            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
        )

        Row(
            Modifier.padding(10.dp)
        ) {
            HomeCard(
                largeLabel = "Great Sounds",
                smallLabel = "Listening Experiences",
                imageResourceId = R.drawable.img_home_great_sounds,
                imageContentDescriptionId = R.string.home_img_macbook_content_description
            )
            Spacer(modifier = Modifier.size(10.dp))
            HomeCard(
                largeLabel = "Essentials",
                smallLabel = "All you need",
                imageResourceId = R.drawable.img_home_essentials,
                imageContentDescriptionId = R.string.home_img_macbook_content_description
            )
        }
        Row(
            Modifier.padding(10.dp)
        ) {
            HomeCard(
                largeLabel = "Virtual Reality",
                smallLabel = "Dive into the details",
                imageResourceId = R.drawable.img_home_vr,
                imageContentDescriptionId = R.string.home_img_macbook_content_description
            )

            SmallVerticalSpacer()
            HomeCard(
                largeLabel = "Performance",
                smallLabel = "Powerful devices",
                imageResourceId = R.drawable.img_home_performance,
                imageContentDescriptionId = R.string.home_img_macbook_content_description
            )
        }
    }
}

@Composable
fun HomeCard(
    largeLabel: String,
    smallLabel: String,
    imageResourceId: Int,
    imageContentDescriptionId: Int,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        modifier = Modifier.size(width = 150.dp, height = 200.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(width = 150.dp, height = 120.dp),
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    painter = painterResource(id = imageResourceId),
                    contentDescription = stringResource(id = imageContentDescriptionId)
                )
            }

            SmallVerticalSpacer()

            Text(largeLabel, style = AppTheme.typography.labelLarge)

            SmallVerticalSpacer()

            Text(
                smallLabel, style = AppTheme.typography.labelSmall
            )
        }

    }
}

@Preview
@Composable
fun HomeCardPreview() {
    AppTheme {
        HomeCard(
            largeLabel = "Great Sounds",
            smallLabel = "Listening Experiences",
            imageResourceId = R.drawable.img_home_great_sounds,
            imageContentDescriptionId = R.string.home_img_macbook_content_description
        )
    }
}