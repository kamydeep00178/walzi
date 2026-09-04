package com.yunok.walzi.domain.model

/**
 * One page of a paginated wallpaper query.
 */
data class WallpaperPage(
    val items: List<Wallpaper>,
    val nextCursor: WallpaperCursor?,
    val endReached: Boolean
)