package com.opendesign.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.opendesign.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Create : Screen("create", "Create", Icons.Outlined.AddCircle, Icons.Filled.AddCircle)
    object Media : Screen("media", "Media", Icons.Outlined.Videocam, Icons.Filled.Videocam)
    object Gallery : Screen("gallery", "Gallery", Icons.Outlined.Image, Icons.Filled.Image)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

val screens = listOf(Screen.Home, Screen.Create, Screen.Media, Screen.Gallery, Screen.Settings)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Create.route) { CreateScreen() }
            composable(Screen.Media.route) { MediaScreen() }
            composable(Screen.Gallery.route) { GalleryScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
