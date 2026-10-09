package com.yunok.walzi.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Collections
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
import androidx.compose.ui.zIndex
import coil.size.Size
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import kotlinx.coroutines.launch

/** Fan photos are small - decode them small. */
private val FAN_PHOTO_SIZE = Size(220, 340)

/**
 * "Your Collections" on the Recent tab: a horizontal shelf of cards. Each card shows the list's
 * first 3 wallpapers as a fanned hand of photos that gently "breathes", opens wider while
 * pressed, then the list name and count. Cards slide in from the right on first show.
 */
@Composable
fun YourCollectionsShelf(
    playlists: List<PlaylistPreview>,
    onOpenList: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) return

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Text("YOUR COLLECTIONS", color = TextTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(
                "  ·  ${playlists.size}",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            itemsIndexed(playlists, key = { _, p -> p.list.id }) { index, preview ->
                CollectionFanCard(
                    preview = preview,
                    index = index,
                    onClick = { onOpenList(preview.list.id) }
                )
            }
        }
    }
}

@Composable
private fun CollectionFanCard(preview: PlaylistPreview, index: Int, onClick: () -> Unit) {
    // Slide in from the right, staggered by position.
    val enter = remember(preview.list.id) { Animatable(0f) }
    LaunchedEffect(preview.list.id) {
        enter.animateTo(1f, tween(450, delayMillis = (index * 70).coerceAtMost(350), easing = FastOutSlowInEasing))
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    // Fan opens wider while pressed.
    val spreadBoost by animateFloatAsState(if (pressed) 1.25f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "spread")
    // Gentle idle "breathing" of the fan.
    val breathe by rememberInfiniteTransition(label = "fan").animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    Column(
        modifier = Modifier
            .width(168.dp)
            .graphicsLayer {
                alpha = enter.value
                translationX = (1f - enter.value) * 80.dp.toPx()
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(
                1.dp,
                Brush.linearGradient(GradientSignature.map { it.copy(alpha = 0.55f) }),
                RoundedCornerShape(20.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        // The fanned hand of photos.
        Box(
            modifier = Modifier.fillMaxWidth().height(116.dp),
            contentAlignment = Alignment.Center
        ) {
            val urls = preview.previewImageUrls.take(3)
            if (urls.isEmpty()) {
                Icon(Icons.Filled.Collections, contentDescription = null, tint = Accent1, modifier = Modifier.size(34.dp))
            } else {
                // Side photos first, the middle one on top.
                val order = when (urls.size) {
                    1 -> listOf(0)
                    2 -> listOf(0, 1)
                    else -> listOf(0, 2, 1)
                }
                order.forEach { i ->
                    val slot = when {
                        urls.size == 1 -> 0f
                        urls.size == 2 -> if (i == 0) -0.6f else 0.6f
                        else -> (i - 1).toFloat() // -1, 0, 1
                    }
                    ThumbImage(
                        url = urls[i],
                        placeholderKey = urls[i],
                        size = FAN_PHOTO_SIZE,
                        modifier = Modifier
                            .zIndex(if (slot == 0f) 2f else 1f)
                            .size(width = 62.dp, height = 96.dp)
                            .graphicsLayer {
                                val spread = breathe * spreadBoost
                                translationX = slot * 38.dp.toPx() * spread
                                translationY = (slot * slot) * 6.dp.toPx()
                                rotationZ = slot * 12f * spread
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preview.list.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val count = preview.list.wallpaperIds.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Accent3))
                    Text(
                        "$count wallpaper${if (count == 1) "" else "s"}",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
