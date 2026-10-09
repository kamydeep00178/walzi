package com.yunok.walzi.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.util.GRID_THUMBNAIL_SIZE
import com.yunok.walzi.util.thumbMemoryKey
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Which screen a card is on - picks its badge and entrance style. */
enum class CardStyle { RECENT, POPULAR, CATEGORY, FAVORITE, LIST }

/** Wallpapers added within this window get a "NEW" badge on the Recent grid. */
private val NEW_WINDOW_MS = TimeUnit.DAYS.toMillis(7)

/**
 * The app's animated wallpaper card (matches the Popular "Top 10" deck): rounded card with a
 * thin light border, a per-screen badge, a press "squeeze", and - when [dealIn] is true - an
 * entrance the first time it appears: it rises, fades in and springs to full size. Popular
 * cards also tilt in, like a dealt playing card.
 *
 * Badges: POPULAR "#rank 🔥" + "POPULAR" label · RECENT "NEW" (added in the last 7 days) ·
 * FAVORITE heart · CATEGORY / LIST none. All motion is read in graphicsLayer (draw phase).
 */
@Composable
fun AnimatedWallpaperCard(
    wallpaper: Wallpaper,
    style: CardStyle,
    rank: Int,
    aspectRatio: Float,
    dealIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tiltFrom = if (style == CardStyle.POPULAR) (if (rank % 2 == 0) 6f else -6f) else 0f
    // Stagger the two columns slightly.
    val delayMs = if (rank % 2 == 0) 70 else 0
    val progress = remember(wallpaper.id) { Animatable(if (dealIn) 0f else 1f) }
    val scale = remember(wallpaper.id) { Animatable(if (dealIn) 0.86f else 1f) }

    LaunchedEffect(wallpaper.id) {
        if (progress.value < 1f) {
            launch { progress.animateTo(1f, tween(420, delayMillis = delayMs, easing = FastOutSlowInEasing)) }
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    // Press feedback: a small squeeze while the finger is down.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .graphicsLayer {
                val p = progress.value
                translationY = (1f - p) * 60.dp.toPx()
                rotationZ = (1f - p) * tiltFrom
                alpha = p
                scaleX = scale.value * pressScale
                scaleY = scale.value * pressScale
            }
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        // Same request as WallpaperCard -> shares its memory cache with the detail screen.
        ThumbImage(
            url = wallpaper.gridImageUrl,
            placeholderKey = wallpaper.id,
            size = GRID_THUMBNAIL_SIZE,
            contentDescription = wallpaper.title,
            memoryCacheKey = thumbMemoryKey(wallpaper.imageUrl),
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.matchParentSize().bottomScrim(startFraction = 0.5f, maxAlpha = 0.75f))

        CardBadge(style = style, rank = rank, wallpaper = wallpaper, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 9.dp)) {
            if (style == CardStyle.POPULAR) {
                Text("POPULAR", color = Accent3, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Text(
                wallpaper.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardBadge(style: CardStyle, rank: Int, wallpaper: Wallpaper, modifier: Modifier) {
    when (style) {
        CardStyle.POPULAR -> Row(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#$rank", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Accent2, modifier = Modifier.padding(start = 3.dp).size(12.dp))
        }

        CardStyle.RECENT -> {
            val isNew = wallpaper.createdAt > 0 && System.currentTimeMillis() - wallpaper.createdAt < NEW_WINDOW_MS
            if (isNew) {
                Text(
                    "NEW",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.sp,
                    modifier = modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.linearGradient(GradientSignature))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        CardStyle.FAVORITE -> Box(
            modifier = modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = "Favourite", tint = Accent2, modifier = Modifier.size(14.dp))
        }

        CardStyle.CATEGORY, CardStyle.LIST -> Unit
    }
}
