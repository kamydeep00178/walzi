package com.yunok.walzi.presentation.wallpaperdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.util.ImageDownloader
import com.yunok.walzi.util.WallpaperSetter
import com.yunok.walzi.util.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperDetailViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val listRepository: WallpaperListRepository,
    private val wallpaperSetter: WallpaperSetter,
    private val imageDownloader: ImageDownloader,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<WallpaperDetailIntent, WallpaperDetailState, WallpaperDetailEffect>(WallpaperDetailState()) {

    private val initialWallpaperId: String = savedStateHandle.get<String>("wallpaperId").orEmpty()
    private val sourceParam: String = savedStateHandle.get<String>("source") ?: "feed"
    private val categoryIdParam: String? = savedStateHandle.get<String>("categoryId")
    private val listIdParam: String? = savedStateHandle.get<String>("listId")
    private val queryParam: String? = savedStateHandle.get<String>("query")

    private var cursor: WallpaperCursor? = null

    init {
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { favoriteIds ->
                setState { copy(wallpapers = wallpapers.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }) }
            }
        }
        viewModelScope.launch {
            listRepository.observeLists().collect { lists -> setState { copy(availableLists = lists) } }
        }
        loadInitial()
    }

    override suspend fun handleIntent(intent: WallpaperDetailIntent) {
        when (intent) {
            WallpaperDetailIntent.LoadNextPage -> loadNextPage()
            is WallpaperDetailIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
            is WallpaperDetailIntent.OpenSetWallpaperSheet ->
                setState { copy(showTargetSheet = true, targetWallpaperId = intent.wallpaperId) }
            WallpaperDetailIntent.DismissSetWallpaperSheet -> setState { copy(showTargetSheet = false) }
            is WallpaperDetailIntent.ConfirmSetWallpaper -> confirmSetWallpaper(intent.wallpaperId, intent.target)
            is WallpaperDetailIntent.Download -> download(intent.wallpaperId)
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
        viewModelScope.launch {
            setState { copy(isLoading = true) }

            if (sourceParam == "favorites") {
                val ids = repository.observeFavoriteIds().first().toList()
                val wallpapers = repository.getWallpapersByIds(ids)
                applyLoaded(wallpapers, endReached = true)
                return@launch
            }

            if (sourceParam == "list") {
                val listId = listIdParam.orEmpty()
                val ids = listRepository.observeLists().first().firstOrNull { it.id == listId }?.wallpaperIds.orEmpty()
                val wallpapers = repository.getWallpapersByIds(ids)
                applyLoaded(wallpapers, endReached = true)
                return@launch
            }

            if (sourceParam == "search") {
                val wallpapers = repository.searchWallpapersByTag(queryParam.orEmpty())
                applyLoaded(wallpapers, endReached = true)
                return@launch
            }

            val source = sourceParam.toWallpaperSource(categoryIdParam)
            val page = repository.loadWallpaperPage(source, cursor = null)
            cursor = page.nextCursor
            applyLoaded(page.items, endReached = page.endReached)
        }
    }

    private suspend fun applyLoaded(wallpapers: List<Wallpaper>, endReached: Boolean) {
        var index = wallpapers.indexOfFirst { it.id == initialWallpaperId }
        val finalList: List<Wallpaper>
        if (index == -1) {
            val direct = repository.getWallpaperById(initialWallpaperId)
            finalList = if (direct != null) listOf(direct) + wallpapers else wallpapers
            index = 0
        } else {
            finalList = wallpapers
        }
        setState { copy(wallpapers = finalList, initialIndex = index, isLoading = false, endReached = endReached) }
    }

    private fun loadNextPage() {
        if (sourceParam == "favorites" || sourceParam == "list" || sourceParam == "search") return
        if (currentState.isLoadingMore || currentState.endReached) return
        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val source = sourceParam.toWallpaperSource(categoryIdParam)
            val page = repository.loadWallpaperPage(source, cursor)
            cursor = page.nextCursor
            setState {
                copy(
                    wallpapers = wallpapers + page.items,
                    isLoadingMore = false,
                    endReached = page.endReached
                )
            }
        }
    }

    private fun confirmSetWallpaper(wallpaperId: String, target: WallpaperTarget) {
        val wallpaper = currentState.wallpapers.firstOrNull { it.id == wallpaperId } ?: return
        viewModelScope.launch {
            setState { copy(isApplyingWallpaper = true, showTargetSheet = false) }
            val result = wallpaperSetter.setWallpaper(wallpaper.imageUrl, target)
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
        }
    }

    private fun download(wallpaperId: String) {
        val wallpaper = currentState.wallpapers.firstOrNull { it.id == wallpaperId } ?: return
        viewModelScope.launch {
            setState { copy(isDownloading = true) }
            val result = imageDownloader.downloadToGallery(wallpaper.imageUrl, wallpaper.title)
            setState { copy(isDownloading = false) }
            setEffect(
                WallpaperDetailEffect.ShowMessage(
                    if (result.isSuccess) "Saved to gallery" else "Download failed. Try again."
                )
            )
        }
    }

    private fun String.toWallpaperSource(categoryId: String?): WallpaperSource = when (this) {
        "recent" -> WallpaperSource.Recent
        "popular" -> WallpaperSource.Popular
        "category" -> WallpaperSource.CategoryWallpapers(categoryId.orEmpty())
        else -> WallpaperSource.Feed
    }
}