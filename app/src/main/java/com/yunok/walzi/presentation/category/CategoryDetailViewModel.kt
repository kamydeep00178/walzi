package com.yunok.walzi.presentation.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<CategoryDetailIntent, CategoryDetailState, CategoryDetailEffect>(CategoryDetailState()) {

    private val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()
    private var cursor: WallpaperCursor? = null
    private var hasStartedLivePagination = false
    private var cacheCollectionJob: Job? = null

    init {
        setState { copy(categoryId = categoryId) }
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                categories.firstOrNull { it.id == categoryId }?.let { match ->
                    setState { copy(categoryName = match.name) }
                }
            }
        }
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { favoriteIds ->
                setState { copy(wallpapers = wallpapers.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }) }
            }
        }
        loadFirstPage()
    }

    override suspend fun handleIntent(intent: CategoryDetailIntent) {
        when (intent) {
            is CategoryDetailIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
            CategoryDetailIntent.LoadNextPage -> loadNextPage()
        }
    }

    private fun loadFirstPage() {
        cacheCollectionJob?.cancel()
        cursor = null
        hasStartedLivePagination = false
        setState { copy(isLoading = true, wallpapers = emptyList(), endReached = false) }

        val source = WallpaperSource.CategoryWallpapers(categoryId)
        val bucket = source.cacheBucket()

        cacheCollectionJob = viewModelScope.launch {
            repository.observeCachedWallpapers(bucket).collect { cached ->
                if (hasStartedLivePagination) return@collect
                setState { copy(wallpapers = cached, isLoading = false) }
                cached.lastOrNull()?.let { last ->
                    cursor = WallpaperCursor(last.priority.toLong(), last.createdAt)
                }
            }
        }
        viewModelScope.launch {
            repository.ensureWallpaperBucketFresh(source, bucket)
        }
    }

    private fun loadNextPage() {
        if (currentState.isLoadingMore || currentState.endReached) return
        hasStartedLivePagination = true
        cacheCollectionJob?.cancel()

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val page = repository.loadWallpaperPage(WallpaperSource.CategoryWallpapers(categoryId), cursor)
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
}