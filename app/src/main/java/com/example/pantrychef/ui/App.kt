package com.example.pantrychef.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("pantry", "Pantry", Icons.Default.Home),
    Tab("results", "Recipes", Icons.Default.Search),
    Tab("ai", "AI Chef", Icons.Default.Star),
    Tab("favorites", "Favorites", Icons.Default.Favorite)
)

@Composable
fun PantryChefApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "pantry", modifier = Modifier.padding(padding)) {
            composable("pantry") {
                PantryScreen(vm, onFind = {
                    vm.searchRecipes()
                    nav.navigate("results") {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                })
            }
            composable("results") {
                ResultsScreen(vm, onOpen = { nav.navigate("detail/$it") })
            }
            composable("detail/{id}") { entry ->
                DetailScreen(vm, entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("ai") { AiChefScreen(vm) }
            composable("favorites") {
                FavoritesScreen(vm, onOpen = { nav.navigate("detail/$it") })
            }
        }
    }
}
