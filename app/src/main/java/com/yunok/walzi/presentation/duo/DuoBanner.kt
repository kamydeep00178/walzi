package com.yunok.walzi.presentation.duo

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.yunok.walzi.domain.model.DuoConfig
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.presentation.theme.Elevated2
import kotlinx.coroutines.delay

/** Cover decode size: full-width banner, ~180dp tall. */
private val COVER_SIZE = Size(1200, 680)

private const val COVER_DISPLAY_MS = 3400L
private const val COVER_FADE_MS = 900

/**
 * Full-width Duo banner for the Collections tab: the 4-5 cover images rotate with a crossfade
 * and a slow "Ken Burns" zoom, behind the Duo title, subtitle and an animated "LOCK + HOME"
 * badge. Rotation runs only while the screen is visible (RESUMED).
 */
@Composable
fun DuoBanner(config: DuoConfig, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val covers = config.coverUrls
    var index by remember(covers) { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Warm the cache so every cover is ready before it fades in.
    LaunchedEffect(covers) {
        covers.forEach { url ->
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).size(COVER_SIZE).build())
        }
    }

    LaunchedEffect(covers, lifecycleOwner) {
        if (covers.size < 2) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(COVER_DISPLAY_MS)
                index = (index + 1) % covers.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Crossfade(targetState = index, animationSpec = tween(COVER_FADE_MS), label = "duoCover") { i ->
            // Slow zoom while each cover is on screen.
            val zoom = remember(i) { Animatable(1f) }
            LaunchedEffect(i) {
                zoom.animateTo(1.12f, tween((COVER_DISPLAY_MS + COVER_FADE_MS).toInt(), easing = LinearEasing))
            }
            ThumbImage(
                url = covers[i.coerceIn(0, covers.lastIndex)],
                placeholderKey = covers[i.coerceIn(0, covers.lastIndex)],
                size = COVER_SIZE,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom.value
                        scaleY = zoom.value
                    }
            )
        }

        // Legibility gradient (left side), drawn once per size.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val brush = Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                    onDrawBehind { drawRect(brush) }
                }
        )

        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerBadge("LOCK + HOME")
            Text(config.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            if (config.subtitle.isNotBlank()) {
                Text(
                    config.subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Explore", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
        }

        // Which cover is showing.
        if (covers.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                covers.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(width = if (i == index) 14.dp else 5.dp, height = 5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (i == index) Accent3 else Elevated2.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

/** Gradient pill with a light sweep moving across it - read in the draw phase only. */
@Composable
private fun ShimmerBadge(text: String) {
    val transition = rememberInfiniteTransition(label = "badge")
    val sweep = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "badgeSweep"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .drawWithCache {
                val base = Brush.linearGradient(GradientSignature)
                onDrawBehind {
                    drawRect(base)
                    val x = sweep.value * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                            start = Offset(x - size.width * 0.4f, 0f),
                            end = Offset(x, size.height)
                        )
                    )
                }
            }
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
    }
}
