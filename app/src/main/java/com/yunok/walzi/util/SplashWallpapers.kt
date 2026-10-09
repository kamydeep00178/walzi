package com.yunok.walzi.util

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.repository.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real wallpapers for the splash screen's moving grid.
 *
 * [cachedUrls] returns the top wallpapers (Popular first, then Recent - from the Room cache) whose
 * thumbnail is *already in Coil's disk cache*, so the splash never waits on the network. If too
 * few are cached (first install), the splash keeps its glass tiles.
 *
 * [prefetchTop] quietly downloads those top thumbnails after start-up, so the next launch has them.
 */
@Singleton
class SplashWallpapers @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WallpaperRepository
) {
    /** Thumbnail URLs of the top wallpapers, in order; empty if unavailable. */
    private suspend fun topThumbUrls(): List<String> {
        val popular = repository.observeCachedWallpapers(WallpaperSource.Popular.cacheBucket()).first()
        val recent = repository.observeCachedWallpapers(WallpaperSource.Recent.cacheBucket()).first()
        return (popular + recent).distinctBy { it.id }.map { it.gridImageUrl }.take(TOP_COUNT)
    }

    /** Top thumbnails already on disk (usable instantly, offline too). */
    suspend fun cachedUrls(): List<String> = withContext(Dispatchers.IO) {
        try {
            val disk = context.imageLoader.diskCache ?: return@withContext emptyList()
            topThumbUrls().filter { url -> disk.openSnapshot(url)?.use { true } ?: false }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Downloads the top thumbnails into the disk cache (one by one, low priority, no memory cache). */
    suspend fun prefetchTop() {
        try {
            topThumbUrls().forEach { url ->
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(GRID_THUMBNAIL_SIZE)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best effort - the splash just falls back to its glass tiles.
        }
    }

    companion object {
        const val TOP_COUNT = 10

        /** Show real wallpapers only when at least this many are cached; otherwise glass tiles. */
        const val MIN_FOR_SPLASH = 4
    }
}
