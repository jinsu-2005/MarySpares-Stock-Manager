package com.marytwowheelers.spares.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.marytwowheelers.spares.ui.screens.*
import com.marytwowheelers.spares.ui.theme.ThemeMode
import com.marytwowheelers.spares.ui.viewmodels.AppViewModelFactory

sealed class Screen(val route: String) {
    object Auth        : Screen("auth")
    object Dashboard   : Screen("dashboard")
    object Inventory   : Screen("inventory?focusSearch={focusSearch}") {
        fun createRoute(focusSearch: Boolean = false) = "inventory?focusSearch=$focusSearch"
    }
    object History     : Screen("history")
    object Settings    : Screen("settings")
    object PartDetails : Screen("part_details/{partId}") {
        fun createRoute(partId: String) = "part_details/$partId"
    }
}

@Composable
fun MarySparesNavGraph(
    navController   : NavHostController,
    modifier        : Modifier = Modifier,
    viewModelFactory: AppViewModelFactory,
    currentTheme    : ThemeMode,
    onThemeChange   : (ThemeMode) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) Screen.Dashboard.route else Screen.Auth.route

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel             = viewModel(factory = viewModelFactory),
                onNavigateToInventory = { navController.navigate(Screen.Inventory.createRoute(focusSearch = false)) },
                onNavigateToInventorySearch = { navController.navigate(Screen.Inventory.createRoute(focusSearch = true)) },
                onNavigateToPartDetails = { partId ->
                    navController.navigate(Screen.PartDetails.createRoute(partId))
                }
            )
        }

        composable(
            route     = Screen.Inventory.route,
            arguments = listOf(navArgument("focusSearch") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val focusSearch = backStackEntry.arguments?.getBoolean("focusSearch") ?: false
            InventoryScreen(
                viewModel = viewModel(factory = viewModelFactory),
                autoFocusSearch = focusSearch,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToPartDetails = { partId ->
                    navController.navigate(Screen.PartDetails.createRoute(partId))
                }
            )
        }

        composable(
            route     = Screen.PartDetails.route,
            arguments = listOf(navArgument("partId") { type = NavType.StringType })
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            PartDetailsScreen(
                partId          = partId,
                viewModel       = viewModel(factory = viewModelFactory),
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onNavigateToPartDetails = { partId ->
                    navController.navigate(Screen.PartDetails.createRoute(partId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel     = viewModel(factory = viewModelFactory),
                currentTheme  = currentTheme,
                onThemeChange = onThemeChange,
                onLogout      = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
