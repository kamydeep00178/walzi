package com.yunok.walzi.util

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size

/**
 * Longest edge (px) we ever decode when *applying* a wallpaper. Phone panels top out well below
 * this, while decoded bitmap memory grows with the square of the edge: an 8K source is ~130 MB
 * decoded, which is an OutOfMemoryError waiting to happen on mid-range devices - and in
 * AutoRotateWorker that means a background crash.
 */
private const val MAX_WALLPAPER_EDGE_PX = 4096

/**
 * Request for a full-quality *software* bitmap to hand to WallpaperManager.
 *
 *  - Bounded (never upscaled, never above [MAX_WALLPAPER_EDGE_PX]).
 *  - Memory cache disabled: a one-off multi-tens-of-MB bitmap must not sit in the shared cache
 *    evicting every grid thumbnail. The download itself still uses the disk cache.
 */
fun Context.wallpaperBitmapRequest(imageUrl: String): ImageRequest =
    ImageRequest.Builder(this)
        .data(imageUrl)
        .size(Size(MAX_WALLPAPER_EDGE_PX, MAX_WALLPAPER_EDGE_PX))
        .scale(Scale.FIT)
        .precision(Precision.INEXACT)
        .allowHardware(false) // WallpaperManager needs a software bitmap
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()

/** Key shared by grid thumbnails and the detail screen's placeholder, so opening a wallpaper
 *  paints the already-decoded thumbnail instantly instead of a blank frame. */
fun thumbMemoryKey(imageUrl: String): String = "thumb:$imageUrl"
