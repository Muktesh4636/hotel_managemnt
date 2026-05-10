package com.restaurant.management.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.restaurant.management.ui.screens.AdminScreens
import com.restaurant.management.ui.screens.CoreScreens
import com.restaurant.management.ui.screens.QrMenuScreen
import com.restaurant.management.ui.util.parseModulesJson

object Destinations {
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val KITCHEN = "kitchen"
    const val MORE = "more"
    const val MENU_ADMIN = "menu_admin"
    const val INVENTORY = "inventory"
    const val RESERVATIONS = "reservations"
    const val STAFF = "staff"
    const val REPORTS = "reports"
    const val EXPENSES = "expenses"
    const val SETTINGS = "settings"
    const val QR_MENU = "qr_menu"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val moreRoutes =
    setOf(
        Destinations.MORE,
        Destinations.MENU_ADMIN,
        Destinations.INVENTORY,
        Destinations.RESERVATIONS,
        Destinations.STAFF,
        Destinations.REPORTS,
        Destinations.EXPENSES,
        Destinations.SETTINGS,
        Destinations.QR_MENU,
    )

@Composable
fun RestaurantRoot(vm: RestaurantViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val settings by vm.settings.collectAsState()
    val moduleFlags =
        remember(settings?.modulesJson) {
            parseModulesJson(settings?.modulesJson)
        }
    val bottomTabs =
        remember(moduleFlags) {
            buildList {
                add(BottomTab(Destinations.DASHBOARD, "Home", Icons.Default.Home))
                add(BottomTab(Destinations.POS, "POS", Icons.Default.ShoppingCart))
                if (moduleFlags.kitchen) {
                    add(BottomTab(Destinations.KITCHEN, "Kitchen", Icons.Default.LocalDining))
                }
                add(BottomTab(Destinations.MORE, "More", Icons.Default.Apps))
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                bottomTabs.forEach { tab ->
                    val selected =
                        if (tab.route == Destinations.MORE) {
                            currentRoute in moreRoutes
                        } else {
                            navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        }
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selected,
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surface,
                                unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            ),
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destinations.DASHBOARD) {
                CoreScreens.Dashboard(vm)
            }
            composable(Destinations.POS) {
                CoreScreens.Pos(vm)
            }
            composable(Destinations.KITCHEN) {
                CoreScreens.Kitchen(vm)
            }
            composable(Destinations.MORE) {
                AdminScreens.Hub(
                    vm = vm,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destinations.MENU_ADMIN) {
                AdminScreens.MenuAdmin(vm)
            }
            composable(Destinations.INVENTORY) {
                AdminScreens.Inventory(vm)
            }
            composable(Destinations.RESERVATIONS) {
                AdminScreens.Reservations(vm)
            }
            composable(Destinations.STAFF) {
                AdminScreens.Staff(vm)
            }
            composable(Destinations.REPORTS) {
                AdminScreens.Reports(vm)
            }
            composable(Destinations.EXPENSES) {
                AdminScreens.Expenses(vm)
            }
            composable(Destinations.SETTINGS) {
                AdminScreens.Settings(vm)
            }
            composable(Destinations.QR_MENU) {
                QrMenuScreen(vm)
            }
        }
    }
}
