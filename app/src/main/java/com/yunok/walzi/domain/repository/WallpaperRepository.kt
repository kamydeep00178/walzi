package com.yunok.walzi.domain.repository

import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Tag
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperPage
import com.yunok.walzi.domain.model.WallpaperSource
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {

    fun observeCategories(): Flow<List<Category>>

    fun observeTags(): Flow<List<Tag>>

    suspend fun loadWallpaperPage(
        source: WallpaperSource,
        cursor: WallpaperCursor?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WallpaperPage

    /**
     * Cached first page of a bucket, in the exact order Firestore returned it. Grid items are
     * emitted with `isFavorite = false` on purpose - grids never render a heart, and stamping
     * favorites onto every item would re-emit (and recompose) the whole list on each toggle.
     */
    fun observeCachedWallpapers(bucket: String): Flow<List<Wallpaper>>

    /** @return true if the bucket is fresh (or was just refreshed); false if a needed refresh failed. */
    suspend fun ensureWallpaperBucketFresh(source: WallpaperSource, bucket: String, pageSize: Int = DEFAULT_PAGE_SIZE): Boolean

    fun observeFeaturedWallpapers(): Flow<List<Wallpaper>>

    suspend fun ensureFeaturedFresh()

    /**
     * Never throws for network problems: ids that can't be resolved (offline and not seen
     * before) are simply missing from the result. Results follow the order of [ids]. Resolved
     * wallpapers are memoised in memory, so repeated lookups cost no Firestore reads.
     */
    suspend fun getWallpapersByIds(ids: List<String>): List<Wallpaper>

    suspend fun getWallpaperById(id: String): Wallpaper?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun toggleFavorite(wallpaperId: String)

    suspend fun searchWallpapersByTag(tag: String, limit: Int = 30): List<Wallpaper>

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}