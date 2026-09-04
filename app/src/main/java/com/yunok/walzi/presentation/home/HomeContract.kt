package com.yunok.walzi.presentation.home

import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

enum class FeedTab { RECENT, COLLECTIONS, POPULAR, FAVORITES }

data class HomeState(
    val selectedTab: FeedTab = FeedTab.RECENT,
    val categories: List<Category> = emptyList(),
    val wallpapers: List<Wallpaper> = emptyList(),
    /** Daily country+category pick shown in the "Featured Wallpapers" carousel at the top of
     *  the Recent tab - see WallpaperRepository.observeFeaturedWallpapers. */
    val featuredWallpapers: List<Wallpaper> = emptyList(),
    /** Non-empty custom/default lists, shown as a chips row above the Recent tab's grid,
     *  each paired with its first 3 wallpapers' image URLs for a photo-collage preview. */
    val playlists: List<PlaylistPreview> = emptyList(),
    /** Backs the synthetic "Favourites" card in the Your Collections row - Favourites is a
     *  tab, not a WallpaperList, so it needs its own count/preview rather than reusing playlists. */
    val favoritesCount: Int = 0,
    val favoritesPreviewUrls: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false
) : MviState

data class PlaylistPreview(val list: WallpaperList, val previewImageUrls: List<String>)

sealed interface HomeIntent : MviIntent {
    data class SelectTab(val tab: FeedTab) : HomeIntent
    data class ToggleFavorite(val wallpaperId: String) : HomeIntent
    /** Called when the grid scrolls near its last loaded item - fetches the next page. */
    data object LoadNextPage : HomeIntent
}

sealed interface HomeEffect : MviEffect