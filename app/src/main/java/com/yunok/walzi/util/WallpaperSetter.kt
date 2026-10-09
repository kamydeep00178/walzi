package com.yunok.walzi.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.imageLoader
import coil.request.ErrorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class WallpaperTarget { HOME, LOCK, BOTH }

class WallpaperSetter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun setWallpaper(
        imageUrl: String,
        target: WallpaperTarget,
        adjustments: WallpaperAdjustments = WallpaperAdjustments.NONE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Same app-wide ImageLoader (disk cache shared with the preview, so no
            // re-download) but a bounded, memory-cache-free decode - see wallpaperBitmapRequest.
            val result = context.imageLoader.execute(context.wallpaperBitmapRequest(imageUrl))
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: return@withContext Result.failure(
                    (result as? ErrorResult)?.throwable ?: IllegalStateException("Could not load image")
                )

            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Pre-N devices only support a single system wallpaper.
                val m = context.resources.displayMetrics
                wallpaperManager.setBitmap(bitmap.withAdjustments(adjustments, m.widthPixels, m.heightPixels, m.density))
                return@withContext Result.success(Unit)
            }

            when (target) {
                WallpaperTarget.HOME -> apply(wallpaperManager, bitmap, adjustments, WallpaperManager.FLAG_SYSTEM)
                WallpaperTarget.LOCK -> apply(wallpaperManager, bitmap, adjustments, WallpaperManager.FLAG_LOCK)
                WallpaperTarget.BOTH -> apply(
                    wallpaperManager, bitmap, adjustments,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            // An Error, not an Exception - without this it would crash the process.
            Result.failure(IllegalStateException("Not enough memory to set this wallpaper", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * A Duo: [lockImageUrl] on the lock screen and [homeImageUrl] on the home screen. The two
     * images are decoded one after the other (never both in memory at once). Pre-N devices have
     * a single wallpaper, so only the home image is set there.
     */
    suspend fun setDuo(homeImageUrl: String, lockImageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val manager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                manager.setBitmap(loadBitmap(homeImageUrl))
                return@withContext Result.success(Unit)
            }
            apply(manager, loadBitmap(lockImageUrl), WallpaperAdjustments.NONE, WallpaperManager.FLAG_LOCK)
            apply(manager, loadBitmap(homeImageUrl), WallpaperAdjustments.NONE, WallpaperManager.FLAG_SYSTEM)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Result.failure(IllegalStateException("Not enough memory to set this wallpaper", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Bounded, memory-cache-free software decode (see wallpaperBitmapRequest); throws on failure. */
    private suspend fun loadBitmap(imageUrl: String): Bitmap {
        val result = context.imageLoader.execute(context.wallpaperBitmapRequest(imageUrl))
        return (result.drawable as? BitmapDrawable)?.bitmap
            ?: throw ((result as? ErrorResult)?.throwable ?: IllegalStateException("Could not load image"))
    }

    /** Applies [adjustments] to a copy of [bitmap] and sets it on [flags], with the position crop hint. */
    private fun apply(manager: WallpaperManager, bitmap: Bitmap, adjustments: WallpaperAdjustments, flags: Int) {
        val metrics = context.resources.displayMetrics
        val adjusted = bitmap.withAdjustments(adjustments, metrics.widthPixels, metrics.heightPixels, metrics.density)
        try {
            val cropHint = visibleCropHint(
                adjusted.width, adjusted.height,
                metrics.widthPixels, metrics.heightPixels,
                adjustments.position
            )
            manager.setBitmap(adjusted, cropHint, true, flags)
        } finally {
            if (adjusted !== bitmap) adjusted.recycle()
        }
    }
}
