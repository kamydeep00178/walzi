package com.yunok.walzi.domain.model

/** Identifies which Firestore query a paginated wallpaper list is backed by. */
sealed class WallpaperSource {
    data object Feed : WallpaperSource()
    data object Recent : WallpaperSource()
    data object Popular : WallpaperSource()
    data class CategoryWallpapers(val categoryId: String) : WallpaperSource()
}

/** Room cache bucket key for a given source - keeps the "which cache row belongs to which
 *  query" mapping in one place instead of duplicated string literals scattered around. */
fun WallpaperSource.cacheBucket(): String = when (this) {
    is WallpaperSource.Feed -> "feed"
    is WallpaperSource.Recent -> "recent"
    is WallpaperSource.Popular -> "popular"
    is WallpaperSource.CategoryWallpapers -> "category:$categoryId"
}