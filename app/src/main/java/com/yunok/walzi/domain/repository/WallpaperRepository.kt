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

    fun observeCachedWallpapers(bucket: String): Flow<List<Wallpaper>>

    suspend fun ensureWallpaperBucketFresh(source: WallpaperSource, bucket: String, pageSize: Int = DEFAULT_PAGE_SIZE)

    fun observeFeaturedWallpapers(): Flow<List<Wallpaper>>

    suspend fun ensureFeaturedFresh()

    suspend fun getWallpapersByIds(ids: List<String>): List<Wallpaper>

    suspend fun getWallpaperById(id: String): Wallpaper?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun toggleFavorite(wallpaperId: String)

    suspend fun searchWallpapersByTag(tag: String, limit: Int = 30): List<Wallpaper>

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}