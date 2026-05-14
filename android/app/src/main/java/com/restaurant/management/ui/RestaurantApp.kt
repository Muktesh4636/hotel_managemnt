package com.restaurant.management.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableRestaurant
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.restaurant.management.ui.screens.AdminScreens
import com.restaurant.management.ui.screens.CoreScreens
import com.restaurant.management.ui.screens.QrMenuScreen
import com.restaurant.management.ui.screens.TablesReservationsScreen
import com.restaurant.management.ui.util.parseModulesJson

object Destinations {
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val KITCHEN = "kitchen"
    const val MORE = "more"
    const val MENU_ADMIN = "menu_admin"
    const val INVENTORY = "inventory"
    const val STAFF = "staff"
    const val REPORTS = "reports"
    const val ORDERS = "orders"
    const val EXPENSES = "expenses"
    const val SETTINGS = "settings"
    const val QR_MENU = "qr_menu"
    const val TABLES = "tables"
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
        Destinations.STAFF,
        Destinations.REPORTS,
        Destinations.ORDERS,
        Destinations.EXPENSES,
        Destinations.SETTINGS,
        Destinations.QR_MENU,
    )

/**
 * When opening Operations from the bottom bar, always land on the hub module list if it is
 * already under the current stack (e.g. user was on Order history or QR menu).
 */
private fun NavController.navigateToMoreHubFromBottomBar(): Boolean {
    val popped = popBackStack(Destinations.MORE, inclusive = false)
    if (!popped) {
        navigateToBottomTab(Destinations.MORE)
    }
    return popped
}

/**
 * Bottom tabs share one pattern so the back stack never mixes Operations deep-links with
 * Home/POS/Kitchen in a way that drops taps (see Navigation "Bottom navigation" guidance).
 */
private fun NavController.navigateToBottomTab(route: String) {
    navigate(route) {
        // Pop to Home route (string) so back stack matches top-level tabs; graph id can desync selection on some devices.
        popUpTo(Destinations.DASHBOARD) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

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
                if (moduleFlags.tables) {
                    add(BottomTab(Destinations.TABLES, "Tables", Icons.Default.TableRestaurant))
                }
                add(BottomTab(Destinations.MORE, "More", Icons.Default.Apps))
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Avoid stacking bottom system-bar inset on top of the nav bar (extra gap above bottom nav).
        contentWindowInsets =
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                bottomTabs.forEach { tab ->
                    // Use leaf route (not hierarchy) — Navigation 2.8+ can leave parent routes on hierarchy so POS never "matches".
                    val selected =
                        if (tab.route == Destinations.MORE) {
                            currentRoute != null && currentRoute in moreRoutes
                        } else {
                            currentRoute == tab.route
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
                            if (tab.route == Destinations.MORE) {
                                if (currentRoute == Destinations.MORE) {
                                    return@NavigationBarItem
                                }
                                navController.navigateToMoreHubFromBottomBar()
                            } else {
                                navController.navigateToBottomTab(tab.route)
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Destinations.DASHBOARD,
                modifier = Modifier.fillMaxSize(),
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
            composable(Destinations.STAFF) {
                AdminScreens.Staff(vm)
            }
            composable(Destinations.REPORTS) {
                AdminScreens.Reports(vm)
            }
            composable(Destinations.ORDERS) {
                AdminScreens.Orders(vm)
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
            composable(Destinations.TABLES) {
                TablesReservationsScreen(vm)
            }
        }
            AdminScreens.GlobalOrderDialogs(vm)
        }
    }
}
