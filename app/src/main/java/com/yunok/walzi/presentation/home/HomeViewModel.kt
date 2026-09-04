package com.yunok.walzi.presentation.home

import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recent and Popular each show a Room-cached first page instantly on open (see
 * WallpaperRepository.observeCachedWallpapers), while a background check refreshes that cache
 * if its TTL has elapsed (CacheConfig). Scrolling further ("load more") always calls Firestore
 * live, continuing from a cursor derived from whichever wallpaper is currently last in the
 * list - whether that list came from cache or a previous live page.
 *
 * Favourites is a separate, always-bounded case: it reacts directly to the local favorites
 * id set (no Firestore pagination, no cache bucket) and re-resolves to full Wallpaper objects
 * every time that set changes, so un-favoriting something removes it from the tab immediately.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val listRepository: WallpaperListRepository
) : BaseViewModel<HomeIntent, HomeState, HomeEffect>(HomeState()) {

    private var cursor: WallpaperCursor? = null
    private var hasStartedLivePagination = false
    private var cacheCollectionJob: Job? = null
    private var favoritesJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                setState { copy(categories = categories) }
            }
        }
        // "Featured Wallpapers" carousel: kick off the daily pick if it hasn't been generated
        // yet today (no-op otherwise - see WallpaperRepositoryImpl.ensureFeaturedFresh), and
        // observe whatever's in the Room-cached bucket. Instant paint from cache; the ensure
        // call only ever touches network/Firestore once per calendar day.
        viewModelScope.launch { repository.ensureFeaturedFresh() }
        viewModelScope.launch {
            repository.observeFeaturedWallpapers().collect { featured ->
                setState { copy(featuredWallpapers = featured) }
            }
        }
        viewModelScope.launch {
            listRepository.observeLists().collect { lists ->
                val nonEmpty = lists.filter { it.wallpaperIds.isNotEmpty() }
                val previews = nonEmpty.map { list ->
                    val previewUrls = repository.getWallpapersByIds(list.wallpaperIds.take(3)).map { it.imageUrl }
                    PlaylistPreview(list, previewUrls)
                }
                setState { copy(playlists = previews) }
            }
        }
        // Backs the "Favourites" folder card in Your Collections - separate from the
        // playlists collector above since Favourites is a tab/bucket, not a WallpaperList.
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { ids ->
                val previewUrls = repository.getWallpapersByIds(ids.take(3).toList()).map { it.imageUrl }
                setState { copy(favoritesCount = ids.size, favoritesPreviewUrls = previewUrls) }
            }
        }
        // Keeps favorite hearts correct regardless of whether the current list came from the
        // Room cache (already favorite-aware) or a live page (favorite-aware only as of fetch time).
        // (Favourites tab itself is handled by its own dedicated collector below, not this one.)
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { favoriteIds ->
                if (currentState.selectedTab == FeedTab.FAVORITES) return@collect
                setState { copy(wallpapers = wallpapers.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }) }
            }
        }
        loadTab(FeedTab.RECENT)
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectTab -> loadTab(intent.tab)
            is HomeIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
            HomeIntent.LoadNextPage -> loadNextPage()
        }
    }

    private fun loadTab(tab: FeedTab) {
        cacheCollectionJob?.cancel()
        favoritesJob?.cancel()

        if (tab == FeedTab.COLLECTIONS) {
            setState { copy(selectedTab = tab, isLoading = false) }
            return
        }

        if (tab == FeedTab.FAVORITES) {
            setState { copy(selectedTab = tab, wallpapers = emptyList(), isLoading = true, endReached = true) }
            favoritesJob = viewModelScope.launch {
                repository.observeFavoriteIds().collect { ids ->
                    val wallpapers = repository.getWallpapersByIds(ids.toList())
                    setState { copy(wallpapers = wallpapers, isLoading = false) }
                }
            }
            return
        }

        cursor = null
        hasStartedLivePagination = false
        setState { copy(selectedTab = tab, wallpapers = emptyList(), isLoading = true, endReached = false) }

        val source = tab.toSource()
        val bucket = source.cacheBucket()

        // Instant paint from Room; stops updating state once live "load more" has begun so a
        // background refresh mid-scroll can't clobber items the user has already paged into.
        cacheCollectionJob = viewModelScope.launch {
            repository.observeCachedWallpapers(bucket).collect { cached ->
                if (hasStartedLivePagination) return@collect
                setState { copy(wallpapers = cached, isLoading = false) }
                cached.lastOrNull()?.let { last ->
                    cursor = WallpaperCursor(last.priority.toLong(), last.createdAt)
                }
            }
        }

        // Fire-and-forget: refreshes the Room cache in the background only if its TTL elapsed.
        viewModelScope.launch {
            repository.ensureWallpaperBucketFresh(source, bucket)
        }
    }

    private fun loadNextPage() {
        if (currentState.selectedTab == FeedTab.COLLECTIONS) return
        if (currentState.selectedTab == FeedTab.FAVORITES) return // bounded set - never paginates
        if (currentState.isLoadingMore || currentState.endReached) return

        hasStartedLivePagination = true
        cacheCollectionJob?.cancel()

        viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val source = currentState.selectedTab.toSource()
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

    private fun FeedTab.toSource(): WallpaperSource = when (this) {
        FeedTab.RECENT -> WallpaperSource.Recent
        FeedTab.POPULAR -> WallpaperSource.Popular
        FeedTab.COLLECTIONS -> WallpaperSource.Recent // unreachable - Collections never loads wallpapers directly
        FeedTab.FAVORITES -> WallpaperSource.Recent // unreachable - Favourites is handled separately above
    }
}