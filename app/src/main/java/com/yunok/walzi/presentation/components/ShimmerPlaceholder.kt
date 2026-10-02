package com.yunok.walzi.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A varied, saturated palette so different wallpapers/categories get visibly different
 * placeholder colors while loading, rather than one repeated flat gray box.
 */
private val PLACEHOLDER_COLORS = listOf(
    Color(0xFF7C5CFF), // violet
    Color(0xFFFF5C9E), // pink
    Color(0xFF4CD9E8), // cyan
    Color(0xFFB7285B), // rose
    Color(0xFF1AA6B7), // teal
    Color(0xFF8A1F5C), // magenta
    Color(0xFFC34CFF), // purple
    Color(0xFF4C9A2A), // green
    Color(0xFFB7601A), // amber
    Color(0xFF5C1FB7), // indigo
)

/** Deterministic per-item pick (same id always gets the same color) rather than a truly
 *  random re-roll every recomposition, so a given wallpaper's placeholder doesn't flicker
 *  between colors while it's loading. */
fun placeholderColorFor(id: String): Color =
    PLACEHOLDER_COLORS[(id.hashCode() and 0x7fffffff) % PLACEHOLDER_COLORS.size]

/**
 * Modern diagonal shimmer sweep over a colored base - used as the loading state for grid
 * thumbnails so scrolling through not-yet-loaded wallpapers feels lively instead of showing
 * blank/gray boxes.
 *
 * The animated offset is read inside the draw phase only, so the sweep re-draws every frame but
 * never recomposes. (Reading it in the composable body - as this used to - recomposed every
 * visible placeholder on every frame while scrolling.)
 */
@Composable
fun ShimmerPlaceholder(baseColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val base = baseColor.copy(alpha = 0.35f)
                val sweepColors = listOf(
                    baseColor.copy(alpha = 0.55f),
                    baseColor.copy(alpha = 0.85f),
                    baseColor.copy(alpha = 0.55f)
                )
                onDrawBehind {
                    val t = translate.value // draw-phase read
                    drawRect(base)
                    drawRect(
                        Brush.linearGradient(
                            colors = sweepColors,
                            start = Offset(t, t),
                            end = Offset(t + 400f, t + 400f)
                        )
                    )
                }
            }
    )
}
