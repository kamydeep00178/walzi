package com.yunok.walzi.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Lists : Screen("lists")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")

    data object CategoryDetail : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }

    data object ListDetail : Screen("list/{listId}") {
        fun createRoute(listId: String) = "list/$listId"
    }

    /**
     * [source] tells WallpaperDetail which paginated query (or bounded local set, for
     * "favorites"/"list"/"search") to keep swiping through: "feed" | "recent" | "popular" |
     * "category" | "favorites" | "list" | "search". [listId] only used when source == "list";
     * [query] only used when source == "search" (URL-encoded, since search text can contain
     * spaces/special characters).
     */
    data object WallpaperDetail : Screen("wallpaper/{wallpaperId}?source={source}&categoryId={categoryId}&listId={listId}&query={query}") {
        fun createRoute(
            wallpaperId: String,
            source: String = "feed",
            categoryId: String? = null,
            listId: String? = null,
            query: String? = null
        ): String {
            var route = "wallpaper/$wallpaperId?source=$source"
            if (categoryId != null) route += "&categoryId=$categoryId"
            if (listId != null) route += "&listId=$listId"
            if (query != null) route += "&query=${android.net.Uri.encode(query)}"
            return route
        }
    }
}

/**
 * Parsed from a notification's data payload (see WalziFirebaseMessagingService and
 * MainActivity). Mirrors the "screen" values the admin portal can send.
 */
data class DeepLinkTarget(
    val screen: String,
    val wallpaperId: String? = null,
    val categoryId: String? = null
) {
    companion object {
        const val HOME = "home"
        const val COLLECTIONS = "collections"
        const val FAVORITES = "favorites"
        const val CATEGORY = "category"
        const val WALLPAPER = "wallpaper"
    }
}

/**
 * Shared by AppRoot (cold-start push tap) and the Notifications screen (in-app tap on a
 * saved notification) so both use identical routing logic - one place to update if a new
 * screen type is ever added to the payload.
 */
fun androidx.navigation.NavController.navigateToDeepLinkTarget(target: DeepLinkTarget) {
    when (target.screen) {
        DeepLinkTarget.WALLPAPER -> target.wallpaperId?.let {
            navigate(Screen.WallpaperDetail.createRoute(it))
        }
        DeepLinkTarget.CATEGORY -> target.categoryId?.let {
            navigate(Screen.CategoryDetail.createRoute(it))
        }
        DeepLinkTarget.FAVORITES -> navigate(Screen.Favorites.route)
        DeepLinkTarget.COLLECTIONS, DeepLinkTarget.HOME -> navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    }
}