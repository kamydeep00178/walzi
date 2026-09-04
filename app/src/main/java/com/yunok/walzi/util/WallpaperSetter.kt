package com.yunok.walzi.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
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
                // Reuses the app-wide cached ImageLoader (see WalziApp.newImageLoader) instead
                // of spinning up a fresh one - if the user just viewed this wallpaper full-screen,
                // this hits cache instead of re-downloading. Size.ORIGINAL guarantees we set the
                // actual full-resolution image, never a downsampled grid-thumbnail decode.
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .allowHardware(false) // need a software bitmap to hand to WallpaperManager
                    .build()
                val drawable = context.imageLoader.execute(request).drawable
                    ?: return@withContext Result.failure(IllegalStateException("Could not load image"))
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    ?: return@withContext Result.failure(IllegalStateException("Unsupported image format"))

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
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}