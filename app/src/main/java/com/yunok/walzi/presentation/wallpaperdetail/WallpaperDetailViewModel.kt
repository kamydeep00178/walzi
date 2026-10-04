package com.yunok.walzi.presentation.wallpaperdetail

import androidx.lifecycle.SavedStateHandle
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.toCursor
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.presentation.common.runSuspendCatching
import com.yunok.walzi.util.AnalyticsTracker
import com.yunok.walzi.util.ImageDownloader
import com.yunok.walzi.util.WallpaperSetter
import com.yunok.walzi.util.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class WallpaperDetailViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val listRepository: WallpaperListRepository,
    private val wallpaperSetter: WallpaperSetter,
    private val imageDownloader: ImageDownloader,
    private val analytics: AnalyticsTracker,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<WallpaperDetailIntent, WallpaperDetailState, WallpaperDetailEffect>(WallpaperDetailState()) {

    private val initialWallpaperId: String = savedStateHandle.get<String>("wallpaperId").orEmpty()
    private val sourceParam: String = savedStateHandle.get<String>("source") ?: "feed"
    private val categoryIdParam: String? = savedStateHandle.get<String>("categoryId")
    private val listIdParam: String? = savedStateHandle.get<String>("listId")
    private val queryParam: String? = savedStateHandle.get<String>("query")

    private var cursor: WallpaperCursor? = null
    private var openLogged = false
    private var lastViewedWallpaperId: String? = null

    /** What the first load produced: the pager's list, where to open it, and whether more pages exist. */
    private data class InitialLoad(val wallpapers: List<Wallpaper>, val index: Int, val endReached: Boolean)

    init {
        launchSafely {
            repository.observeFavoriteIds().distinctUntilChanged().collect { ids ->
                setState { copy(favoriteIds = ids) }
            }
        }
        launchSafely {
            listRepository.observeLists().collect { lists -> setState { copy(availableLists = lists) } }
        }
        loadInitial()
    }

    override suspend fun handleIntent(intent: WallpaperDetailIntent) {
        when (intent) {
            WallpaperDetailIntent.LoadNextPage -> loadNextPage()
            WallpaperDetailIntent.Retry -> loadInitial()
            is WallpaperDetailIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
            is WallpaperDetailIntent.OpenSetWallpaperSheet ->
                setState { copy(showTargetSheet = true, targetWallpaperId = intent.wallpaperId) }
            WallpaperDetailIntent.DismissSetWallpaperSheet -> setState { copy(showTargetSheet = false) }
            is WallpaperDetailIntent.ConfirmSetWallpaper -> confirmSetWallpaper(intent.wallpaperId, intent.target)
            is WallpaperDetailIntent.Download -> download(intent.wallpaperId, intent.viaReward)
            is WallpaperDetailIntent.OpenAddToListSheet ->
                setState { copy(showAddToListSheet = true, addToListWallpaperId = intent.wallpaperId, newListNameDraft = "") }
            WallpaperDetailIntent.DismissAddToListSheet ->
                setState { copy(showAddToListSheet = false, addToListWallpaperId = null) }
            is WallpaperDetailIntent.ToggleWallpaperInList -> {
                if (intent.currentlyIn) {
                    listRepository.removeWallpaperFromList(intent.listId, intent.wallpaperId)
                } else {
                    listRepository.addWallpaperToList(intent.listId, intent.wallpaperId)
                }
            }
            is WallpaperDetailIntent.UpdateNewListNameDraft -> setState { copy(newListNameDraft = intent.value) }
            is WallpaperDetailIntent.CreateListAndAddWallpaper -> {
                val name = currentState.newListNameDraft.trim()
                if (name.isEmpty()) return
                val newListId = listRepository.createList(name)
                listRepository.addWallpaperToList(newListId, intent.wallpaperId)
                setState { copy(newListNameDraft = "") }
            }
        }
    }

    private fun loadInitial() {
        launchSafely {
            setState { copy(isLoading = true, hasError = false) }
            runSuspendCatching { fetchInitial() }
                .onSuccess { load ->
                    // Analytics: the wallpaper the user tapped (logged once, not again on Retry).
                    if (!openLogged) {
                        load.wallpapers.getOrNull(load.index)?.let {
                            openLogged = true
                            analytics.wallpaperOpen(it, sourceParam)
                        }
                    }
                    setState {
                        copy(
                            wallpapers = load.wallpapers,
                            initialIndex = load.index,
                            isLoading = false,
                            endReached = load.endReached,
                            hasError = load.wallpapers.isEmpty()
                        )
                    }
                }
                .onFailure { setState { copy(isLoading = false, hasError = true) } }
        }
    }

    /** Analytics: called by the screen whenever the pager settles on a wallpaper (deduplicated). */
    fun onWallpaperViewed(wallpaperId: String) {
        if (wallpaperId == lastViewedWallpaperId) return
        val wallpaper = currentState.wallpapers.firstOrNull { it.id == wallpaperId } ?: return
        lastViewedWallpaperId = wallpaperId
        analytics.wallpaperView(wallpaper, sourceParam)
    }

    private suspend fun fetchInitial(): InitialLoad = when (sourceParam) {
        "favorites" -> {
            val ids = repository.observeFavoriteIds().first().toList()
            fixedList(repository.getWallpapersByIds(ids))
        }
        "list" -> {
            val listId = listIdParam.orEmpty()
            val ids = listRepository.observeLists().first().firstOrNull { it.id == listId }?.wallpaperIds.orEmpty()
            fixedList(repository.getWallpapersByIds(ids))
        }
        "search" -> fixedList(repository.searchWallpapersByTag(queryParam.orEmpty()))
        else -> pagedList()
    }

    /** Bounded sources (favorites / a list / search results): everything is loaded up front. */
    private suspend fun fixedList(wallpapers: List<Wallpaper>): InitialLoad {
        val index = wallpapers.indexOfFirst { it.id == initialWallpaperId }
        if (index != -1) return InitialLoad(wallpapers, index, endReached = true)
        val direct = runSuspendCatching { repository.getWallpaperById(initialWallpaperId) }.getOrNull()
        return InitialLoad(listOfNotNull(direct) + wallpapers, index = 0, endReached = true)
    }

    /**
     * Paged sources (recent / popular / category / feed). If the tapped wallpaper is on the first
     * page, open there. If the user had scrolled deep into the grid it won't be, so instead of
     * showing the top of the feed with the tapped item stuck in front, continue *from* the tapped
     * wallpaper: swiping then follows the same order the grid was in.
     */
    private suspend fun pagedList(): InitialLoad {
        val source = sourceParam.toWallpaperSource(categoryIdParam)
        val firstPage = repository.loadWallpaperPage(source, cursor = null)

        val index = firstPage.items.indexOfFirst { it.id == initialWallpaperId }
        if (index != -1) {
            cursor = firstPage.nextCursor
            return InitialLoad(firstPage.items, index, firstPage.endReached)
        }

        val tapped = runSuspendCatching { repository.getWallpaperById(initialWallpaperId) }.getOrNull()
        if (tapped == null) {
            cursor = firstPage.nextCursor
            return InitialLoad(firstPage.items, index = 0, endReached = firstPage.endReached)
        }

        val following = repository.loadWallpaperPage(source, tapped.toCursor())
        cursor = following.nextCursor
        return InitialLoad(listOf(tapped) + following.items, index = 0, endReached = following.endReached)
    }

    private suspend fun loadNextPage() {
        if (sourceParam == "favorites" || sourceParam == "list" || sourceParam == "search") return
        val current = currentState
        if (current.isLoadingMore || current.endReached || current.wallpapers.isEmpty()) return

        setState { copy(isLoadingMore = true) }
        val source = sourceParam.toWallpaperSource(categoryIdParam)
        runSuspendCatching { repository.loadWallpaperPage(source, cursor) }
            .onSuccess { page ->
                cursor = page.nextCursor
                setState {
                    copy(
                        wallpapers = (wallpapers + page.items).distinctBy { it.id },
                        isLoadingMore = false,
                        endReached = page.endReached
                    )
                }
            }
            // Offline / transient: clear the flag; the pager asks again on the next swipe.
            .onFailure { setState { copy(isLoadingMore = false) } }
    }

    private suspend fun confirmSetWallpaper(wallpaperId: String, target: WallpaperTarget) {
        val wallpaper = currentState.wallpapers.firstOrNull { it.id == wallpaperId } ?: return
        setState { copy(isApplyingWallpaper = true, showTargetSheet = false) }
        val result = wallpaperSetter.setWallpaper(wallpaper.imageUrl, target)
        analytics.setWallpaper(wallpaper, target, result.isSuccess)
        setState { copy(isApplyingWallpaper = false) }
        val label = when (target) {
            WallpaperTarget.HOME -> "Home Screen"
            WallpaperTarget.LOCK -> "Lock Screen"
            WallpaperTarget.BOTH -> "Home & Lock Screen"
        }
        setEffect(
            WallpaperDetailEffect.ShowMessage(
                if (result.isSuccess) "Wallpaper set to $label" else "Couldn't set wallpaper. Try again."
            )
        )
        if (result.isSuccess) setEffect(WallpaperDetailEffect.ActionCompleted)
    }

    private suspend fun download(wallpaperId: String, viaReward: Boolean) {
        val wallpaper = currentState.wallpapers.firstOrNull { it.id == wallpaperId } ?: return
        setState { copy(isDownloading = true) }
        val result = imageDownloader.downloadToGallery(wallpaper.imageUrl, wallpaper.title)
        analytics.download(wallpaper, result.isSuccess, viaReward)
        setState { copy(isDownloading = false) }
        setEffect(
            WallpaperDetailEffect.ShowMessage(
                if (result.isSuccess) "Saved to gallery" else "Download failed. Try again."
            )
        )
        // No interstitial right after a rewarded ad the user just watched for this download.
        if (result.isSuccess && !viaReward) setEffect(WallpaperDetailEffect.ActionCompleted)
    }

    private fun String.toWallpaperSource(categoryId: String?): WallpaperSource = when (this) {
        "recent" -> WallpaperSource.Recent
        "popular" -> WallpaperSource.Popular
        "category" -> WallpaperSource.CategoryWallpapers(categoryId.orEmpty())
        else -> WallpaperSource.Feed
    }
}
