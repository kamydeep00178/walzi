package com.yunok.walzi.presentation.wallpaperdetail

import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState
import com.yunok.walzi.util.WallpaperTarget

data class WallpaperDetailState(
    val wallpapers: List<Wallpaper> = emptyList(),
    /** Index into [wallpapers] the pager should open on - the wallpaper the user actually tapped. */
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val showTargetSheet: Boolean = false,
    /** Which wallpaper (by id) the open sheet / in-flight set-wallpaper action applies to. */
    val targetWallpaperId: String? = null,
    val isApplyingWallpaper: Boolean = false,
    val isDownloading: Boolean = false,
    // -- Add to List --
    val showAddToListSheet: Boolean = false,
    val addToListWallpaperId: String? = null,
    val availableLists: List<WallpaperList> = emptyList(),
    val newListNameDraft: String = ""
) : MviState

sealed interface WallpaperDetailIntent : MviIntent {
    /** Fired when the pager scrolls near the end of what's loaded - fetches the next page. */
    data object LoadNextPage : WallpaperDetailIntent
    data class ToggleFavorite(val wallpaperId: String) : WallpaperDetailIntent
    data class OpenSetWallpaperSheet(val wallpaperId: String) : WallpaperDetailIntent
    data object DismissSetWallpaperSheet : WallpaperDetailIntent
    data class ConfirmSetWallpaper(val wallpaperId: String, val target: WallpaperTarget) : WallpaperDetailIntent
    data class Download(val wallpaperId: String) : WallpaperDetailIntent

    // -- Add to List --
    data class OpenAddToListSheet(val wallpaperId: String) : WallpaperDetailIntent
    data object DismissAddToListSheet : WallpaperDetailIntent
    data class ToggleWallpaperInList(val wallpaperId: String, val listId: String, val currentlyIn: Boolean) : WallpaperDetailIntent
    data class UpdateNewListNameDraft(val value: String) : WallpaperDetailIntent
    data class CreateListAndAddWallpaper(val wallpaperId: String) : WallpaperDetailIntent
}

sealed interface WallpaperDetailEffect : MviEffect {
    data class ShowMessage(val text: String) : WallpaperDetailEffect
}