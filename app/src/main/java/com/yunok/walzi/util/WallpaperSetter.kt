package com.yunok.walzi.util

import android.app.WallpaperManager
import android.content.Context
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
    suspend fun setWallpaper(imageUrl: String, target: WallpaperTarget): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Same app-wide ImageLoader (disk cache shared with the preview, so no
                // re-download) but a bounded, memory-cache-free decode - see wallpaperBitmapRequest.
                val result = context.imageLoader.execute(context.wallpaperBitmapRequest(imageUrl))
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: return@withContext Result.failure(
                        (result as? ErrorResult)?.throwable ?: IllegalStateException("Could not load image")
                    )

                val wallpaperManager = WallpaperManager.getInstance(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val flags = when (target) {
                        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                        WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                        WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }
                    wallpaperManager.setBitmap(bitmap, null, true, flags)
                } else {
                    // Pre-N devices only support a single system wallpaper.
                    wallpaperManager.setBitmap(bitmap)
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
}
