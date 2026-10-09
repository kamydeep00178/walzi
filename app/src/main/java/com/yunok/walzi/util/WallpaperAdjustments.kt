package com.yunok.walzi.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * User tweaks applied before setting a wallpaper. The full-screen preview shows the same values
 * live (Compose blur / color matrix / crop alignment), so what you see is what gets set.
 *
 * @param dim        0 (none) .. 0.7 - darkens, so home-screen icons stay readable.
 * @param blur       0 (none) .. 1 (strong).
 * @param brightness -0.5 (darker) .. 0.5 (brighter).
 * @param position   -1 (left) .. 1 (right) - which part of a wide image is visible.
 */
data class WallpaperAdjustments(
    val dim: Float = 0f,
    val blur: Float = 0f,
    val brightness: Float = 0f,
    val position: Float = 0f
) {
    val isDefault: Boolean get() = this == NONE

    /** Single RGB scale combining brightness and dim - shared by the preview and the bitmap. */
    val colorScale: Float get() = (1f + brightness) * (1f - dim)

    companion object {
        val NONE = WallpaperAdjustments()

        const val MAX_DIM = 0.7f
        const val MIN_BRIGHTNESS = -0.5f
        const val MAX_BRIGHTNESS = 0.5f

        /** Blur at full strength, as seen on screen. The preview and the set bitmap both use it. */
        const val MAX_BLUR_DP = 16f
    }
}

/**
 * Returns a new bitmap with brightness/dim and blur applied ([this] is left untouched).
 *
 * Blur is specified in *screen* terms (see [WallpaperAdjustments.MAX_BLUR_DP]) and converted to
 * this bitmap's pixels using the screen size, so the result matches the on-screen preview
 * whatever the image resolution.
 */
fun Bitmap.withAdjustments(
    adjustments: WallpaperAdjustments,
    screenWidthPx: Int,
    screenHeightPx: Int,
    density: Float
): Bitmap {
    if (adjustments.dim == 0f && adjustments.brightness == 0f && adjustments.blur == 0f) return this

    var result = this
    if (adjustments.blur > 0f) {
        // How many bitmap pixels one screen pixel covers when the image is cropped to fill the screen.
        val bitmapPxPerScreenPx = 1f / max(screenWidthPx.toFloat() / width, screenHeightPx.toFloat() / height)
        val radiusBitmapPx = adjustments.blur.coerceIn(0f, 1f) * WallpaperAdjustments.MAX_BLUR_DP * density * bitmapPxPerScreenPx
        result = result.cheapBlur(radiusBitmapPx)
    }

    val scale = adjustments.colorScale
    if (scale != 1f) {
        val out = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(scale, scale, scale, 1f) })
        }
        Canvas(out).drawBitmap(result, 0f, 0f, paint)
        if (result !== this) result.recycle()
        result = out
    }
    return result
}

/**
 * Fast blur without RenderScript: shrink, then scale back up with filtering. Shrinking by about
 * the blur radius (in bitmap pixels) gives a soft result close to a Gaussian blur of that radius,
 * at a tiny fraction of the cost. Done in two steps so it doesn't look blocky.
 */
private fun Bitmap.cheapBlur(radiusPx: Float): Bitmap {
    val factor = radiusPx.coerceAtLeast(1f)
    if (factor <= 1.05f) return this
    val firstStep = kotlin.math.sqrt(factor)
    val midW = max(1, (width / firstStep).roundToInt())
    val midH = max(1, (height / firstStep).roundToInt())
    val mid = Bitmap.createScaledBitmap(this, midW, midH, true)
    val small = Bitmap.createScaledBitmap(mid, max(1, (width / factor).roundToInt()), max(1, (height / factor).roundToInt()), true)
    val back = Bitmap.createScaledBitmap(small, width, height, true)
    mid.recycle()
    small.recycle()
    return back
}

/**
 * The region of a [bitmapWidth] x [bitmapHeight] image that should be visible on a
 * [screenWidth] x [screenHeight] screen, shifted horizontally by [position] (-1 left .. 1 right).
 * Passed to WallpaperManager as the visible-crop hint.
 */
fun visibleCropHint(
    bitmapWidth: Int,
    bitmapHeight: Int,
    screenWidth: Int,
    screenHeight: Int,
    position: Float
): Rect {
    val screenAspect = screenWidth.toFloat() / screenHeight
    val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight
    return if (bitmapAspect > screenAspect) {
        // Wider than the screen: choose which horizontal slice is shown.
        val cropWidth = (bitmapHeight * screenAspect).roundToInt()
        val left = ((bitmapWidth - cropWidth) * (position.coerceIn(-1f, 1f) + 1f) / 2f).roundToInt()
        Rect(left, 0, left + cropWidth, bitmapHeight)
    } else {
        // Taller than the screen: keep it centred vertically.
        val cropHeight = (bitmapWidth / screenAspect).roundToInt()
        val top = (bitmapHeight - cropHeight) / 2
        Rect(0, top, bitmapWidth, top + cropHeight)
    }
}
