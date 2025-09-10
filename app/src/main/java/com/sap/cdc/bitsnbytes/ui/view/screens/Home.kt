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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.navigation.MainScreenRoute
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.OptimizedProfileNavHost
import com.sap.cdc.bitsnbytes.navigation.AppStateManager
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomBottomBar
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer
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
fun HomeScaffoldView(appStateManager: AppStateManager = viewModel()) {
    // Use the provided app state manager instance (from MainActivity ViewModel or default)
    
    // Navigation state from enhanced navigation manager
    val canNavigateBack by appStateManager.canNavigateBack.collectAsState()
    val selectedTab by appStateManager.selectedTab.collectAsState()
    val hasProfileBackStack by appStateManager.hasProfileBackStack.collectAsState()
    
    // Root navigation controller for tabs
    val rootNavController = rememberNavController()
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    
    // Calculate back navigation state using AppStateManager only
    val shouldShowBackButton = remember(navBackStackEntry, canNavigateBack) {
        val currentRoute = navBackStackEntry?.destination?.route
        when (currentRoute) {
            MainScreenRoute.Configuration.route -> true // Always show back button in configuration
            MainScreenRoute.Profile.route -> canNavigateBack // Use app state manager for profile navigation
            else -> canNavigateBack // Use app state manager for other tabs
        }
    }
    
    // Update current tab based on navigation
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let { route ->
            // Update app state manager
            appStateManager.setSelectedTab(
                when (route) {
                    MainScreenRoute.Home.route -> 0
                    MainScreenRoute.Search.route -> 1
                    MainScreenRoute.Cart.route -> 2
                    MainScreenRoute.Favorites.route -> 3
                    MainScreenRoute.Profile.route -> 4
                    else -> 0
                }
            )
            
            // Update back navigation state based on current route
            when (route) {
                MainScreenRoute.Configuration.route -> {
                    // Always allow back navigation from configuration
                    appStateManager.setCanNavigateBack(true)
                }
                MainScreenRoute.Profile.route -> {
                    // Profile navigation state is handled by AppStateManager
                    // Don't override it here - let the profile nav controller manage it
                }
                else -> {
                    // For other tabs, no back navigation at root level
                    appStateManager.setCanNavigateBack(false)
                }
            }
        }
    }
    
    // Determine title text based on current route
    var titleText by remember { mutableStateOf("") }
    titleText = when (navBackStackEntry?.destination?.route) {
        MainScreenRoute.Home.route -> stringResource(id = MainScreenRoute.Home.resourceId)
        MainScreenRoute.Search.route -> stringResource(id = MainScreenRoute.Search.resourceId)
        MainScreenRoute.Cart.route -> stringResource(id = MainScreenRoute.Cart.resourceId)
        MainScreenRoute.Favorites.route -> stringResource(id = MainScreenRoute.Favorites.resourceId)
        MainScreenRoute.Profile.route -> stringResource(id = MainScreenRoute.Profile.resourceId)
        MainScreenRoute.Configuration.route -> stringResource(id = MainScreenRoute.Configuration.resourceId)
        else -> stringResource(id = MainScreenRoute.Home.resourceId)
    }

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
                    if (shouldShowBackButton) {
                        IconButton(onClick = {
                            // Handle back navigation based on current route
                            val currentRoute = navBackStackEntry?.destination?.route
                            when (currentRoute) {
                                MainScreenRoute.Configuration.route -> {
                                    // Go back from configuration to previous tab
                                    rootNavController.popBackStack()
                                }
                                MainScreenRoute.Profile.route -> {
                                    // For profile navigation, check if we have a profile back stack
                                    if (hasProfileBackStack) {
                                        // If there's a profile back stack, use NavigationCoordinator to go back within profile
                                        NavigationCoordinator.INSTANCE.navigateUp()
                                    } else {
                                        // If no profile back stack (MyProfile is root), go back to Home tab
                                        rootNavController.navigate(MainScreenRoute.Home.route) {
                                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                else -> {
                                    // For other tabs, use NavigationCoordinator for consistency
                                    NavigationCoordinator.INSTANCE.navigateUp()
                                }
                            }
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
                            // Navigate to configuration screen
                            rootNavController.navigate(MainScreenRoute.Configuration.route) {
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
            // Only show bottom bar when not in configuration
            if (navBackStackEntry?.destination?.route != MainScreenRoute.Configuration.route) {
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
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(rootNavController, startDestination = MainScreenRoute.Home.route) {
                composable(MainScreenRoute.Home.route) {
                    // Use simple HomeView for now - can be enhanced later
                    HomeView()
                }
                composable(MainScreenRoute.Search.route) {
                    Text(MainScreenRoute.Search.route)
                }
                composable(MainScreenRoute.Cart.route) {
                    Text(MainScreenRoute.Cart.route)
                }
                composable(MainScreenRoute.Favorites.route) {
                    Text(MainScreenRoute.Favorites.route)
                }
                composable(MainScreenRoute.Profile.route) {
                    // Use the existing OptimizedProfileNavHost with enhanced integration
                    OptimizedProfileNavHost(appStateManager)
                }
                composable(MainScreenRoute.Configuration.route) {
                    val viewModel: ConfigurationViewModel = viewModel(
                        factory = CustomViewModelFactory(LocalContext.current)
                    )
                    ConfigurationView(viewModel)
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
