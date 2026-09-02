package com.marytwowheelers.spares.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marytwowheelers.spares.ui.navigation.MarySparesNavGraph
import com.marytwowheelers.spares.ui.navigation.Screen
import com.marytwowheelers.spares.ui.theme.ThemeMode
import com.marytwowheelers.spares.ui.theme.ThemePreference
import com.marytwowheelers.spares.ui.viewmodels.AppViewModelFactory
import kotlinx.coroutines.launch

private data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MarySparesApp(
    viewModelFactory: AppViewModelFactory,
    currentTheme: ThemeMode
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mainBaseRoutes = setOf(
        "dashboard",
        "inventory",
        "history",
        "settings"
    )
    val currentBaseRoute = currentDestination?.route?.substringBefore("?")
    val showBottomBar = currentBaseRoute in mainBaseRoutes

    val navItems = listOf(
        NavItem(Screen.Dashboard, "Dashboard", Icons.Filled.GridView, Icons.Outlined.GridView),
        NavItem(Screen.Inventory, "Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
        NavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History),
        NavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f
    val dockBg = if (isDark) Color(0xFA14161F) else Color(0xF9FFFFFF)
    val dockBorder = if (isDark) Color(0xFF2C3246) else Color(0xFFE2E8F0)
    val activeColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)
    val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                ) {
                    Surface(
                        color = dockBg,
                        tonalElevation = if (isDark) 8.dp else 4.dp,
                        shadowElevation = if (isDark) 14.dp else 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, dockBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val itemBaseRoute = item.screen.route.substringBefore("?")
                                val isSelected = currentDestination?.hierarchy?.any { (it.route?.substringBefore("?") ?: "") == itemBaseRoute } == true

                                val tabBg = if (isSelected) {
                                    if (isDark) Color(0xFF2B2648) else Color(0xFFEEF2FF)
                                } else {
                                    Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(tabBg)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) activeColor else inactiveColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) activeColor else inactiveColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        MarySparesNavGraph(
            navController    = navController,
            modifier         = Modifier.padding(innerPadding),
            viewModelFactory = viewModelFactory,
            currentTheme     = currentTheme,
            onThemeChange    = { mode ->
                scope.launch { ThemePreference.setThemeMode(context, mode) }
            }
        )
    }
}
