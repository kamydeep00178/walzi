package com.yunok.walzi.presentation.category

import androidx.lifecycle.SavedStateHandle
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.model.toCursor
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.presentation.common.runSuspendCatching
import com.yunok.walzi.util.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val analytics: AnalyticsTracker,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<CategoryDetailIntent, CategoryDetailState, CategoryDetailEffect>(CategoryDetailState()) {

    private val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()
    private val source = WallpaperSource.CategoryWallpapers(categoryId)
    private var cursor: WallpaperCursor? = null
    private var hasStartedLivePagination = false
    private var cacheCollectionJob: Job? = null
    private var categoryViewLogged = false

    init {
        setState { copy(categoryId = categoryId) }
        launchSafely {
            repository.observeCategories()
                .catch { }
                .collect { categories ->
                    categories.firstOrNull { it.id == categoryId }?.let { match ->
                        setState { copy(categoryName = match.name) }
                        // Once per screen visit, as soon as the name is known (categories re-emit).
                        if (!categoryViewLogged) {
                            categoryViewLogged = true
                            analytics.categoryView(categoryId, match.name)
                        }
                    }
                }
        }
        loadFirstPage()
    }

    override suspend fun handleIntent(intent: CategoryDetailIntent) {
        when (intent) {
            is CategoryDetailIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
            CategoryDetailIntent.LoadNextPage -> loadNextPage()
            CategoryDetailIntent.Retry -> loadFirstPage()
        }
    }

    private fun loadFirstPage() {
        cacheCollectionJob?.cancel()
        cursor = null
        hasStartedLivePagination = false
        setState { copy(isLoading = true, hasError = false, wallpapers = emptyList(), endReached = false) }

        val bucket = source.cacheBucket()

        // Instant paint from Room, in the exact order Firestore returned it.
        cacheCollectionJob = launchSafely {
            repository.observeCachedWallpapers(bucket)
                .catch { }
                .collect { cached ->
                    if (hasStartedLivePagination || cached.isEmpty()) return@collect
                    cursor = cached.last().toCursor()
                    setState { copy(wallpapers = cached, isLoading = false, hasError = false) }
                }
        }

        // Refreshes the cache if stale; if that fails with nothing to show, offer a Retry
        // instead of leaving a blank screen.
        launchSafely {
            val refreshed = repository.ensureWallpaperBucketFresh(source, bucket)
            if (currentState.wallpapers.isEmpty()) {
                setState { copy(isLoading = false, hasError = !refreshed) }
            }
        }
    }

    private suspend fun loadNextPage() {
        val current = currentState
        if (current.isLoadingMore || current.endReached || current.wallpapers.isEmpty()) return
        val from = cursor ?: return

        hasStartedLivePagination = true
        cacheCollectionJob?.cancel()
        setState { copy(isLoadingMore = true) }

        runSuspendCatching { repository.loadWallpaperPage(source, from) }
            .onSuccess { page ->
                page.nextCursor?.let { cursor = it }
                setState {
                    copy(
                        // distinctBy: a duplicate key would crash the lazy grid.
                        wallpapers = (wallpapers + page.items).distinctBy { it.id },
                        isLoadingMore = false,
                        endReached = page.endReached
                    )
                }
            }
            .onFailure {
                // Offline / transient: clear the spinner; the next scroll near the end retries.
                setState { copy(isLoadingMore = false) }
            }
    }
}
