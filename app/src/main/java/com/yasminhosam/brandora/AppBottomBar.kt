package com.yasminhosam.brandora

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yasminhosam.brandora.core.navigation.Routes

private data class TopLevelTab(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TopLevelTab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevelTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.DateRange),
    TopLevelTab(Routes.ORDERS, R.string.nav_orders, Icons.Filled.ShoppingCart),
    TopLevelTab(Routes.PRODUCTS, R.string.nav_products, Icons.AutoMirrored.Filled.List),
)

/** The bar is visible only when the current screen is one of the tabs. */
fun NavDestination?.isTopLevel(): Boolean =
    this?.hierarchy?.any { destination -> tabs.any { it.route == destination.route } } == true

@Composable
fun AppBottomBar(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current?.hierarchy?.any { it.route == tab.route } == true,
                onClick = {
                    nav.navigate(tab.route) {
                        // Keep one copy of each tab and remember its state.
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}