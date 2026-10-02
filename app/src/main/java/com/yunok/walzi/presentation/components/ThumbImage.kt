package com.yunok.walzi.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size

/**
 * Cropped image thumbnail with a shimmering placeholder, built for use inside scrolling grids.
 *
 * This deliberately avoids SubcomposeAsyncImage: subcomposition adds a full extra composition
 * pass per item, which is measurable jank in a staggered grid. Here the image is a plain
 * [Image] backed by an [AsyncImagePainter]; the loading/error overlays are ordinary composables.
 *
 * The decode [size] must be explicit (a painter can't wait for layout) - callers pick a size
 * that matches the on-screen slot so memory stays proportional to what's actually visible.
 *
 * @param placeholderKey seeds the deterministic placeholder color (wallpaper id, url, ...).
 * @param memoryCacheKey optional explicit memory-cache key - see [com.yunok.walzi.util.thumbMemoryKey].
 */
@Composable
fun ThumbImage(
    url: String,
    placeholderKey: String,
    size: Size,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    memoryCacheKey: String? = null
) {
    val context = LocalContext.current
    val request = remember(url, size, memoryCacheKey) {
        ImageRequest.Builder(context)
            .data(url)
            .size(size)
            .memoryCacheKey(memoryCacheKey)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request, contentScale = ContentScale.Crop)
    val baseColor = remember(placeholderKey) { placeholderColorFor(placeholderKey) }

    // The static tint sits behind the image so a crossfade blends from color -> photo.
    Box(modifier = modifier.background(baseColor.copy(alpha = 0.35f))) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        when (painter.state) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Empty -> ShimmerPlaceholder(baseColor = baseColor)
            // Error: keep the calm static tint - an endless shimmer would look like "still loading".
            else -> Unit
        }
    }
}
