package com.yunok.walzi.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.size.Size
import com.yunok.walzi.domain.model.Wallpaper

/**
 * Grid thumbnails don't need full wallpaper resolution decoded into memory - capping the
 * decode target here keeps the masonry grid smooth and the memory/disk cache footprint small.
 * Note: this reduces decode + cache size, not network bytes downloaded (R2 serves one file
 * per wallpaper) - see WallpaperDetailScreen for the full-resolution request used on open.
 *
 * No favorite/heart button here by design - favoriting happens from the full-screen detail
 * view's action row, keeping every grid (Recent, Popular, Category, Favourites, List detail)
 * uncluttered and visually consistent.
 */
private val GRID_THUMBNAIL_SIZE = Size(480, 800)

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    aspectRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = remember(wallpaper.imageUrl) {
                ImageRequest.Builder(context)
                    .data(wallpaper.imageUrl)
                    .size(GRID_THUMBNAIL_SIZE)
                    .crossfade(true)
                    .build()
            },
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerPlaceholder(baseColor = placeholderColorFor(wallpaper.id), modifier = Modifier.fillMaxSize()) },
            error = { ShimmerPlaceholder(baseColor = placeholderColorFor(wallpaper.id), modifier = Modifier.fillMaxSize()) },
            success = { SubcomposeAsyncImageContent() }
        )

        // Bottom scrim so the title stays legible over any image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        startY = 0.4f
                    )
                )
        )

        Text(
            text = wallpaper.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}