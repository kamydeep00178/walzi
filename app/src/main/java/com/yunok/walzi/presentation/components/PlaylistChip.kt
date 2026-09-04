package com.yunok.walzi.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.yunok.walzi.presentation.theme.Surface

/**
 * Compact, single-photo card - shorter and simpler than the earlier 3-photo collage design,
 * matching the "Dark" / "4K wallpapers" / "Nature" reference style: one background image, a
 * dark scrim, and a centered bold label. Trades the collage effect for using noticeably less
 * vertical space in the Recent tab.
 */
@Composable
fun PlaylistChip(
    name: String,
    wallpaperCount: Int,
    previewImageUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.9f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val coverUrl = previewImageUrls.firstOrNull()
        if (coverUrl == null) {
            Box(Modifier.fillMaxSize().background(Surface))
        } else {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ShimmerPlaceholder(baseColor = placeholderColorFor(coverUrl), modifier = Modifier.fillMaxSize()) },
                error = { ShimmerPlaceholder(baseColor = placeholderColorFor(coverUrl), modifier = Modifier.fillMaxSize()) },
                success = { SubcomposeAsyncImageContent() }
            )
        }

        // Scrim so the centered label stays legible over any photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f))
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                Icons.Filled.PlaylistPlay,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$wallpaperCount wallpaper${if (wallpaperCount == 1) "" else "s"}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}