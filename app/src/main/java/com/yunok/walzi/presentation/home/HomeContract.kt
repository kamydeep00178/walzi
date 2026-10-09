package com.yunok.walzi.presentation.home

import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.DuoConfig
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

enum class FeedTab { RECENT, COLLECTIONS, POPULAR, FAVORITES }

/**
 * Everything one feed tab needs to render. Each tab owns its own instance, so swiping between
 * tabs never wipes or reloads another tab's list (which used to cause a spinner flash and a
 * lost scroll position on every swipe).
 */
data class TabState(
    val wallpapers: List<Wallpaper> = emptyList(),
    /** True until this tab has something to show (cache hit) or has finished trying. */
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    /** First load failed and there is nothing cached to show - the UI offers a Retry. */
    val hasError: Boolean = false
)

data class HomeState(
    val selectedTab: FeedTab = FeedTab.RECENT,
    val categories: List<Category> = emptyList(),
    /** Daily country+category pick shown in the "Featured Wallpapers" carousel at the top of
     *  the Recent tab - see WallpaperRepository.observeFeaturedWallpapers. */
    val featuredWallpapers: List<Wallpaper> = emptyList(),
    /** Non-empty custom/default lists, shown above the Recent tab's grid, each paired with its
     *  first 3 wallpapers' image URLs for a photo-collage preview. */
    val playlists: List<PlaylistPreview> = emptyList(),
    val recent: TabState = TabState(),
    val popular: TabState = TabState(),
    val favorites: TabState = TabState(),
    /** Duo banner at the top of the Collections tab; null / not visible = hidden. */
    val duoConfig: DuoConfig? = null
) : MviState {
    /** Collections renders straight from [categories], so it has no list state of its own. */
    fun tab(tab: FeedTab): TabState = when (tab) {
        FeedTab.RECENT -> recent
        FeedTab.POPULAR -> popular
        FeedTab.FAVORITES -> favorites
        FeedTab.COLLECTIONS -> COLLECTIONS_PLACEHOLDER
    }

    private companion object {
        val COLLECTIONS_PLACEHOLDER = TabState(isLoading = false)
    }
}

data class PlaylistPreview(val list: WallpaperList, val previewImageUrls: List<String>)

sealed interface HomeIntent : MviIntent {
    /** A tab became the settled page of the pager - loads it the first time it's seen. */
    data class SelectTab(val tab: FeedTab) : HomeIntent
    data class ToggleFavorite(val wallpaperId: String) : HomeIntent
    /** Called when a grid scrolls near its last loaded item - fetches that tab's next page. */
    data class LoadNextPage(val tab: FeedTab) : HomeIntent
    /** Retry button on a tab whose first load failed. */
    data class Retry(val tab: FeedTab) : HomeIntent
}

sealed interface HomeEffect : MviEffect
