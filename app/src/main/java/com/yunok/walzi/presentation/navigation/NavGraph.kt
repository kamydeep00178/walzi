package com.yunok.walzi.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yunok.walzi.presentation.category.CategoryDetailScreen
import com.yunok.walzi.presentation.favorites.FavoritesScreen
import com.yunok.walzi.presentation.home.HomeScreen
import com.yunok.walzi.presentation.lists.ListDetailScreen
import com.yunok.walzi.presentation.lists.ListsScreen
import com.yunok.walzi.presentation.settings.SettingsScreen
import com.yunok.walzi.presentation.wallpaperdetail.WallpaperDetailScreen

private const val ANIM_DURATION_MS = 320
private const val ZOOM_ANIM_DURATION_MS = 280

@Composable
fun NavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        // Default motion for every destination (List/Category detail, Favorites, Settings,
        // Lists): a subtle horizontal slide + fade, the standard "push forward / pop back"
        // feel. WallpaperDetail overrides this below with its own zoom-style transition.
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(ANIM_DURATION_MS)
            ) + fadeIn(animationSpec = tween(ANIM_DURATION_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(ANIM_DURATION_MS)
            ) + fadeOut(animationSpec = tween(ANIM_DURATION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(ANIM_DURATION_MS)
            ) + fadeIn(animationSpec = tween(ANIM_DURATION_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(ANIM_DURATION_MS)
            ) + fadeOut(animationSpec = tween(ANIM_DURATION_MS))
        }
    ) {

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenDrawer = onOpenDrawer,
                onWallpaperClick = { id, source ->
                    navController.navigate(Screen.WallpaperDetail.createRoute(id, source = source))
                },
                onCategoryClick = { id -> navController.navigate(Screen.CategoryDetail.createRoute(id)) },
                onOpenList = { listId -> navController.navigate(Screen.ListDetail.createRoute(listId)) }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBack = { navController.popBackStackOrHome() },
                onWallpaperClick = { id ->
                    navController.navigate(Screen.WallpaperDetail.createRoute(id, source = "favorites"))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStackOrHome() })
        }

        composable(Screen.Lists.route) {
            ListsScreen(
                onBack = { navController.popBackStackOrHome() },
                onOpenList = { listId -> navController.navigate(Screen.ListDetail.createRoute(listId)) }
            )
        }

        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId").orEmpty()
            ListDetailScreen(
                listId = listId,
                onBack = { navController.popBackStackOrHome() },
                onWallpaperClick = { id ->
                    navController.navigate(
                        Screen.WallpaperDetail.createRoute(id, source = "list", listId = listId)
                    )
                }
            )
        }

        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId").orEmpty()
            CategoryDetailScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStackOrHome() },
                onWallpaperClick = { id ->
                    navController.navigate(
                        Screen.WallpaperDetail.createRoute(id, source = "category", categoryId = categoryId)
                    )
                }
            )
        }

        composable(
            route = Screen.WallpaperDetail.route,
            arguments = listOf(
                navArgument("wallpaperId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType; defaultValue = "feed" },
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("listId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            // Overrides the default slide with a "zoom into the photo" feel - scales up +
            // fades in on open, scales back down + fades out on close. Feels closer to how
            // Photos/Gallery-style apps open a full-screen image than a generic horizontal push.
            enterTransition = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(ZOOM_ANIM_DURATION_MS)
                ) + fadeIn(animationSpec = tween(ZOOM_ANIM_DURATION_MS))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(ZOOM_ANIM_DURATION_MS)
                ) + fadeOut(animationSpec = tween(ZOOM_ANIM_DURATION_MS))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 1.06f,
                    animationSpec = tween(ZOOM_ANIM_DURATION_MS)
                ) + fadeIn(animationSpec = tween(ZOOM_ANIM_DURATION_MS))
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 1.06f,
                    animationSpec = tween(ZOOM_ANIM_DURATION_MS)
                ) + fadeOut(animationSpec = tween(ZOOM_ANIM_DURATION_MS))
            }
        ) {
            // wallpaperId / source / categoryId / listId are read straight from SavedStateHandle
            // inside WallpaperDetailViewModel, so nothing further needs passing here.
            WallpaperDetailScreen(onBack = { navController.popBackStackOrHome() })
        }
    }
}

/** Pops back if possible; otherwise (e.g. arrived here directly via deep link) goes Home. */
private fun NavController.popBackStackOrHome() {
    if (!popBackStack()) {
        navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
    }
}