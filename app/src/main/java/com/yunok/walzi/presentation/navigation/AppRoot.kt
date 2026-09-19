package com.yunok.walzi.presentation.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * Root composable: hosts the navigation drawer (Your Feed / Favorites / Settings)
 * around the NavHost, and applies a pending notification deep link once, on launch.
 */
@Composable
fun AppRoot(pendingDeepLink: DeepLinkTarget?) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                navController = navController,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    ) {
        NavGraph(
            navController = navController,
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }

    // Apply a notification deep link exactly once when the app cold-starts from a tap.
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { navController.navigateToDeepLinkTarget(it) }
    }
}