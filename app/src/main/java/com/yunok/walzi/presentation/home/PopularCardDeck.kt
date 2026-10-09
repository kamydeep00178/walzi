package com.yunok.walzi.presentation.home

import android.annotation.SuppressLint
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.components.bottomScrim
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.GRID_THUMBNAIL_SIZE
import com.yunok.walzi.util.thumbMemoryKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** How many cards of the deck are drawn (the rest wait hidden behind). */
private const val VISIBLE_CARDS = 4
private const val AUTO_ADVANCE_MS = 3500L

/**
 * "Top 10" for the Popular tab: the wallpapers stacked like a hand of playing cards. Cards
 * behind the top one are fanned (alternating tilt), smaller and lower. Swipe the top card left
 * or right and it flies off with a spin and goes to the back; the next card rises into place.
 * It also deals itself every [AUTO_ADVANCE_MS] while the screen is visible. Tap the top card to
 * open it.
 *
 * All motion is read in graphicsLayer (draw phase), so animating never recomposes the cards.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PopularCardDeck(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (wallpapers.size < 2) return

    // Deck order as ids; the first id is the top card. Reset if the Top 10 itself changes.
    val ids = remember(wallpapers) { wallpapers.map { it.id } }
    var order by remember(ids) { mutableStateOf(ids) }
    val byId = remember(wallpapers) { wallpapers.associateBy { it.id } }

    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(380.dp)) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val throwPx = widthPx * 1.2f

        /** Fly the top card off to [direction] (-1 left, 1 right), then send it to the back. */
        suspend fun dealTop(direction: Float) {
            dragX.animateTo(direction * throwPx, tween(320, easing = FastOutSlowInEasing))
            order = order.drop(1) + order.first()
            dragX.snapTo(0f)
        }

        // Auto-deal while visible and not being touched.
        LaunchedEffect(ids, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(AUTO_ADVANCE_MS)
                    if (!isDragging && dragX.value == 0f) dealTop(-1f)
                }
            }
        }

        // Gentle float on the top card.
        val float by rememberInfiniteTransition(label = "deckFloat").animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "deckFloatY"
        )

        // Only ids we have a wallpaper for, so the lookup below never fails (no early return
        // inside key {} - that produces bytecode D8 cannot dex).
        val visible = order.take(VISIBLE_CARDS).filter { it in byId }
        // Draw back-to-front; key by id so a card keeps its state as its depth changes.
        visible.asReversed().forEach { id ->
            key(id) {
                val wallpaper = byId.getValue(id)
                val depth = order.indexOf(id)
                val rank = ids.indexOf(id) + 1
                val isTop = depth == 0

                // Depth transforms animate when a card moves up the deck.
                val scale by animateFloatAsState(1f - depth * 0.07f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "s")
                val offsetY by animateFloatAsState(depth * 18f, spring(stiffness = Spring.StiffnessLow), label = "y")
                val tilt by animateFloatAsState(
                    if (depth == 0) 0f else (if (depth % 2 == 1) -1f else 1f) * (3f + depth * 2.5f),
                    spring(stiffness = Spring.StiffnessLow),
                    label = "t"
                )
                val density = LocalDensity.current

                DeckCard(
                    wallpaper = wallpaper,
                    rank = rank,
                    isTop = isTop,
                    onClick = { onWallpaperClick(wallpaper.id) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex((VISIBLE_CARDS - depth).toFloat())
                        .graphicsLayer {
                            val drag = if (isTop) dragX.value else 0f
                            translationX = drag
                            translationY = with(density) { (offsetY + if (isTop) float else 0f).dp.toPx() }
                            rotationZ = tilt + drag / 22f
                            scaleX = scale
                            scaleY = scale
                            // Fade the card being thrown away.
                            alpha = if (isTop) 1f - (drag.absoluteValue / throwPx).coerceIn(0f, 0.6f) else 1f
                        }
                        .then(
                            if (isTop) {
                                Modifier.pointerInput(ids) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isDragging = true },
                                        onHorizontalDrag = { change, amount ->
                                            change.consume()
                                            scope.launch { dragX.snapTo(dragX.value + amount) }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            scope.launch {
                                                if (dragX.value.absoluteValue > widthPx * 0.25f) {
                                                    dealTop(if (dragX.value > 0) 1f else -1f)
                                                } else {
                                                    dragX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            scope.launch { dragX.animateTo(0f, spring()) }
                                        }
                                    )
                                }
                            } else Modifier
                        )
                )
            }
        }

        // Position: which of the Top 10 is on top.
        val topRank = ids.indexOf(order.first()) + 1
        Text(
            "$topRank / ${ids.size}",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** One playing card: wallpaper, corner rank with a "fire" suit, and the title at the bottom. */
@Composable
private fun DeckCard(
    wallpaper: Wallpaper,
    rank: Int,
    isTop: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(210.dp)
            .aspectRatio(0.66f)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = if (isTop) 2.dp else 1.dp,
                color = if (isTop) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(22.dp)
            )
            .then(if (isTop) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Same request as the grid card -> shares its memory cache (instant when already seen).
        ThumbImage(
            url = wallpaper.gridImageUrl,
            placeholderKey = wallpaper.id,
            size = GRID_THUMBNAIL_SIZE,
            contentDescription = wallpaper.title,
            memoryCacheKey = thumbMemoryKey(wallpaper.imageUrl),
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.matchParentSize().bottomScrim(startFraction = 0.45f, maxAlpha = 0.8f))

        // Playing-card corner: rank + suit.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("#$rank", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Accent2, modifier = Modifier.size(14.dp))
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TOP 10", color = Accent3, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Text(
                wallpaper.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
