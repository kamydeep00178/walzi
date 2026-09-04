package com.yunok.walzi.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Lists : Screen("lists")

    data object CategoryDetail : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }

    data object ListDetail : Screen("list/{listId}") {
        fun createRoute(listId: String) = "list/$listId"
    }

    /**
     * [source] tells WallpaperDetail which paginated query (or bounded local set, for
     * "favorites"/"list") to keep swiping through: "feed" | "recent" | "popular" |
     * "category" | "favorites" | "list". [listId] is only used when source == "list".
     */
    data object WallpaperDetail : Screen("wallpaper/{wallpaperId}?source={source}&categoryId={categoryId}&listId={listId}") {
        fun createRoute(
            wallpaperId: String,
            source: String = "feed",
            categoryId: String? = null,
            listId: String? = null
        ): String {
            var route = "wallpaper/$wallpaperId?source=$source"
            if (categoryId != null) route += "&categoryId=$categoryId"
            if (listId != null) route += "&listId=$listId"
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