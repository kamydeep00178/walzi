package com.yunok.walzi.domain.repository

import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperPage
import com.yunok.walzi.domain.model.WallpaperSource
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {

    /** Room-cached, TTL-refreshed (see CacheConfig). Emits instantly from local storage,
     *  refreshing from Firestore in the background only once the TTL says it's time. */
    fun observeCategories(): Flow<List<Category>>

    /**
     * One page (default [DEFAULT_PAGE_SIZE]) of wallpapers for [source], starting after
     * [cursor] (pass null for the first page). Always a live Firestore call - used for
     * "load more" beyond whatever's already cached/on screen.
     */
    suspend fun loadWallpaperPage(
        source: WallpaperSource,
        cursor: WallpaperCursor?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WallpaperPage

    /** Room-cached first screen for a bucket ("recent" / "popular" / "category:<id>") -
     *  instant, no network. Paired with [ensureWallpaperBucketFresh] to decide when to refresh. */
    fun observeCachedWallpapers(bucket: String): Flow<List<Wallpaper>>

    /** Refreshes [bucket]'s cache from Firestore if its TTL has elapsed (or it's empty);
     *  a no-op otherwise. Updates flow silently via [observeCachedWallpapers]. */
    suspend fun ensureWallpaperBucketFresh(source: WallpaperSource, bucket: String, pageSize: Int = DEFAULT_PAGE_SIZE)

    /**
     * The "Featured Wallpapers" carousel's daily picks - instant from Room, refreshing via
     * [ensureFeaturedFresh]. Stored in its own bucket, independent from any tab's bucket so a
     * background refresh elsewhere never disturbs what's already showing mid-day.
     */
    fun observeFeaturedWallpapers(): Flow<List<Wallpaper>>

    /**
     * Regenerates the featured picks if they haven't been generated yet today (calendar-day
     * boundary, not a rolling TTL). Pulls a pool of wallpapers from across the whole
     * collection (no category or country filtering) and deterministically picks 10 for today -
     * same day always yields the same 10, a new day yields a different 10.
     */
    suspend fun ensureFeaturedFresh()

    /** Fetches a specific, bounded set of wallpapers - used for Favorites and custom Lists. */
    suspend fun getWallpapersByIds(ids: List<String>): List<Wallpaper>

    /** Fetches a single wallpaper by id (fallback when it isn't already in a loaded page). */
    suspend fun getWallpaperById(id: String): Wallpaper?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun toggleFavorite(wallpaperId: String)

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}