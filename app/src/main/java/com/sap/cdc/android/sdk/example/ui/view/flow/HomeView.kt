package com.sap.cdc.android.sdk.example.ui.view.flow

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.example.R
import com.sap.cdc.android.sdk.example.ui.route.CartNavHost
import com.sap.cdc.android.sdk.example.ui.route.FavoritesNavHost
import com.sap.cdc.android.sdk.example.ui.route.HomeNavHost
import com.sap.cdc.android.sdk.example.ui.route.MainScreenRoute
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileNavHost
import com.sap.cdc.android.sdk.example.ui.route.SearchNavHost
import com.sap.cdc.android.sdk.example.ui.view.custom.ViewCustomBottomBar
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelConfiguration

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Static scrollbar navigation home view.
 */

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Main landing page view containing bottom bar navigation flows.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Preview
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
            ViewCustomBottomBar(
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
                    ConfigurationView(viewModel = ViewModelConfiguration(LocalContext.current))
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
fun HomeView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.size(20.dp))
        Text("The all-new", fontSize = 20.sp, fontWeight = FontWeight.Light)
        Text("MacBook Pro", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("with Retina display", fontSize = 16.sp, fontWeight = FontWeight.Normal)
        Spacer(modifier = Modifier.size(20.dp))
        OutlinedButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                //TODO: Click buy now...
            }) {
            Text("Buy Now")
        }
        Image(
            modifier = Modifier.size(width = 200.dp, height = 200.dp),
            painter = painterResource(id = R.drawable.img_home_macbook),
            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
        )
        Row(
            Modifier.padding(10.dp)
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
                            painter = painterResource(id = R.drawable.img_home_great_sounds),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Great Sounds", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Listening Experiences",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
            Spacer(modifier = Modifier.size(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier
                    .size(width = 150.dp, height = 200.dp)
                    .background(color = Color.White)
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
                            painter = painterResource(id = R.drawable.img_home_essentials),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Essentials", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "All you need",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
        }
        Row(
            Modifier.padding(10.dp)
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
                            painter = painterResource(id = R.drawable.img_home_vr),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Virtual Reality", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Dive into the details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
            Spacer(modifier = Modifier.size(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier
                    .size(width = 150.dp, height = 200.dp)
                    .background(color = Color.White)
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
                            painter = painterResource(id = R.drawable.img_home_performance),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Performance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Powerful devices",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
        }
    }
}