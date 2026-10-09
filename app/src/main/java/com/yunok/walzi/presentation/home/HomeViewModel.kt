package com.yunok.walzi.presentation.home

import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.model.toCursor
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.presentation.common.runSuspendCatching
import com.yunok.walzi.data.repository.DuoRepository
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.util.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Recent and Popular each show a Room-cached first page instantly on open (see
 * WallpaperRepository.observeCachedWallpapers), while a background check refreshes that cache
 * if its TTL has elapsed (CacheConfig). Scrolling further ("load more") always calls Firestore
 * live, continuing from a cursor derived from whichever wallpaper is currently last in the
 * list - whether that list came from cache or a previous live page.
 *
 * Every tab keeps its own [TabState]; a tab is loaded the first time it becomes the settled
 * pager page and is never wiped afterwards, so swiping back and forth is instant and keeps
 * scroll position.
 *
 * Favourites is a separate, always-bounded case: it reacts directly to the local favorites
 * id set (no Firestore pagination, no cache bucket) and re-resolves to full Wallpaper objects
 * every time that set changes, so un-favoriting something removes it from the tab immediately.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val listRepository: WallpaperListRepository,
    private val analytics: AnalyticsTracker,
    private val duoRepository: DuoRepository
) : BaseViewModel<HomeIntent, HomeState, HomeEffect>(HomeState()) {

    private val cursors = mutableMapOf<FeedTab, WallpaperCursor>()
    private val livePagination = mutableSetOf<FeedTab>()
    private val startedTabs = mutableSetOf<FeedTab>()
    private val tabJobs = mutableMapOf<FeedTab, Job>()

    init {
        launchSafely {
            repository.observeCategories()
                .catch { /* cache read failed - the grid just stays empty */ }
                .collect { categories -> setState { copy(categories = categories) } }
        }
        // "Featured Wallpapers" carousel: kick off the daily pick if it hasn't been generated
        // yet today (no-op otherwise - see WallpaperRepositoryImpl.ensureFeaturedFresh), and
        // observe whatever's in the Room-cached bucket. Instant paint from cache; the ensure
        // call only ever touches network/Firestore once per calendar day.
        launchSafely { repository.ensureFeaturedFresh() }
        launchSafely {
            repository.observeFeaturedWallpapers()
                .catch { }
                .collect { featured -> setState { copy(featuredWallpapers = featured) } }
        }
        launchSafely {
            // collectLatest: a newer lists emission cancels a stale preview lookup still in flight.
            listRepository.observeLists().collectLatest { lists ->
                val previews = coroutineScope {
                    lists.filter { it.wallpaperIds.isNotEmpty() }.map { list ->
                        async {
                            // Non-throwing and memoised in the repository: no Firestore reads
                            // after the first time a wallpaper has been seen.
                            val urls = repository.getWallpapersByIds(list.wallpaperIds.take(3)).map { it.gridImageUrl }
                            PlaylistPreview(list, urls)
                        }
                    }.awaitAll()
                }
                setState { copy(playlists = previews) }
            }
        }
        // Duo banner for the Collections tab (one small document, cached for the session).
        launchSafely {
            val config = duoRepository.getConfig()
            setState { copy(duoConfig = config) }
        }
        startTab(FeedTab.RECENT)
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectTab -> selectTab(intent.tab)
            is HomeIntent.Retry -> startTab(intent.tab, force = true)
            is HomeIntent.LoadNextPage -> loadNextPage(intent.tab)
            is HomeIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
        }
    }

    /** Analytics: "Surprise me" spun / its pick opened. */
    fun trackSurpriseSpin() = analytics.surpriseSpin()
    fun trackSurpriseOpen(wallpaper: Wallpaper) = analytics.surpriseOpen(wallpaper)

    /** Analytics: the Duo banner was tapped. */
    fun trackDuoBannerClick() = analytics.duoBannerClick()

    /** Analytics: a category tile was tapped in the Collections tab. */
    fun trackCategoryClick(categoryId: String) {
        val name = currentState.categories.firstOrNull { it.id == categoryId }?.name.orEmpty()
        analytics.categoryClick(categoryId, name, source = "collections")
    }

    private fun selectTab(tab: FeedTab) {
        setState { copy(selectedTab = tab) }
        startTab(tab)
    }

    private fun updateTab(tab: FeedTab, reducer: TabState.() -> TabState) = setState {
        when (tab) {
            FeedTab.RECENT -> copy(recent = recent.reducer())
            FeedTab.POPULAR -> copy(popular = popular.reducer())
            FeedTab.FAVORITES -> copy(favorites = favorites.reducer())
            FeedTab.COLLECTIONS -> this
        }
    }

    /** Idempotent: a tab that has already been started is left exactly as it is (unless [force]). */
    private fun startTab(tab: FeedTab, force: Boolean = false) {
        if (tab == FeedTab.COLLECTIONS) return
        if (!force && tab in startedTabs) return
        startedTabs += tab

        tabJobs.remove(tab)?.cancel()
        cursors.remove(tab)
        livePagination -= tab
        updateTab(tab) { TabState(isLoading = true) }

        if (tab == FeedTab.FAVORITES) {
            tabJobs[tab] = launchSafely {
                repository.observeFavoriteIds().distinctUntilChanged().collectLatest { ids ->
                    val wallpapers = repository.getWallpapersByIds(ids.toList())
                    updateTab(FeedTab.FAVORITES) {
                        copy(wallpapers = wallpapers, isLoading = false, endReached = true, hasError = false)
                    }
                }
            }
            return
        }

        val source = tab.toSource()
        val bucket = source.cacheBucket()

        // Instant paint from Room; stops updating state once live "load more" has begun so a
        // background refresh mid-scroll can't clobber items the user has already paged into.
        tabJobs[tab] = launchSafely {
            repository.observeCachedWallpapers(bucket)
                .catch { }
                .collect { cached ->
                    if (tab in livePagination || cached.isEmpty()) return@collect
                    cursors[tab] = cached.last().toCursor()
                    updateTab(tab) { copy(wallpapers = cached, isLoading = false, hasError = false) }
                }
        }

        // Refreshes the Room cache in the background only if its TTL elapsed. If that fails and
        // there is nothing cached to show, surface an error + Retry instead of a blank screen.
        launchSafely {
            val refreshed = repository.ensureWallpaperBucketFresh(source, bucket)
            if (currentState.tab(tab).wallpapers.isEmpty()) {
                updateTab(tab) { copy(isLoading = false, hasError = !refreshed) }
            }
        }
    }

    private suspend fun loadNextPage(tab: FeedTab) {
        if (tab == FeedTab.COLLECTIONS || tab == FeedTab.FAVORITES) return // bounded / no pagination
        val tabState = currentState.tab(tab)
        if (tabState.isLoadingMore || tabState.endReached || tabState.wallpapers.isEmpty()) return
        val cursor = cursors[tab] ?: return

        livePagination += tab
        tabJobs[tab]?.cancel() // stop listening to the cache; live pages own the list from here
        updateTab(tab) { copy(isLoadingMore = true) }

        runSuspendCatching { repository.loadWallpaperPage(tab.toSource(), cursor) }
            .onSuccess { page ->
                page.nextCursor?.let { cursors[tab] = it }
                updateTab(tab) {
                    copy(
                        // distinctBy: a duplicate key would crash the lazy grid.
                        wallpapers = (wallpapers + page.items).distinctBy { it.id },
                        isLoadingMore = false,
                        endReached = page.endReached
                    )
                }
            }
            .onFailure {
                // Offline / transient: clear the spinner. The next scroll near the end retries.
                updateTab(tab) { copy(isLoadingMore = false) }
            }
    }

    private fun FeedTab.toSource(): WallpaperSource = when (this) {
        FeedTab.RECENT -> WallpaperSource.Recent
        FeedTab.POPULAR -> WallpaperSource.Popular
        FeedTab.COLLECTIONS -> WallpaperSource.Recent // unreachable - Collections never loads wallpapers directly
        FeedTab.FAVORITES -> WallpaperSource.Recent // unreachable - Favourites is handled separately above
    }
}
