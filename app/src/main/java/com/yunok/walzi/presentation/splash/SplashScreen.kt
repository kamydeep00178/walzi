package com.yunok.walzi.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunok.walzi.R
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.GradientSignature
import kotlinx.coroutines.delay

/**
 * Background: large dark glass-panel tiles with a neon-gradient edge glow, tilted at a slight
 * angle - not a marquee of solid-color squares. Each tile is near-black with a thin
 * violet/cyan/pink gradient border plus a faint matching radial glow bleeding in from one
 * corner, evoking backlit glass rather than filled color blocks.
 */
private val tileGlowColors = listOf(
    Color(0xFF7C5CFF) to Color(0xFF4CD9E8),
    Color(0xFF4CD9E8) to Color(0xFFFF5C9E),
    Color(0xFFFF5C9E) to Color(0xFF7C5CFF),
    Color(0xFF5C1FB7) to Color(0xFF1AA6B7),
    Color(0xFF8A1F5C) to Color(0xFFB7285B),
    Color(0xFF0A5C6B) to Color(0xFF7C5CFF),
)

private const val SPLASH_DURATION_MS = 1800L

/**
 * Custom in-app splash shown as the first Compose frame (after the system's static
 * androidx.core.splashscreen icon has already appeared/disappeared for cold start).
 * Auto-advances after [SPLASH_DURATION_MS] via [onFinished] - wire this up in MainActivity
 * to swap into AppRoot once it fires. See MainActivity.kt for the swap logic.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val gridState = rememberLazyGridState()
    val repeatedTiles = remember { List(200) { tileGlowColors[it % tileGlowColors.size] } }

    LaunchedEffect(Unit) {
        while (true) {
            gridState.scrollBy(0.6f)
            delay(16)
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(BgApp)) {
        // Slight tilt + overscan (scaled up) so rotation never reveals a bare background
        // corner - mirrors the angled glass-panel look rather than an axis-aligned grid.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -7f
                    scaleX = 1.35f
                    scaleY = 1.35f
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(repeatedTiles.size) { index ->
                    GlassGlowTile(colors = repeatedTiles[index])
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(GradientSignature)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "W",
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                )
            }

            Box(Modifier.padding(top = 18.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_wordmark),
                           contentDescription = "Walzi",
                    modifier = Modifier.height(34.dp)
                )
            }

            Box(Modifier.padding(top = 10.dp)) {
                Text(
                    text = "Wallpapers made for your screen",
                    color = Color.White.copy(alpha = 0.7f),
                    style = TextStyle(fontSize = 12.5.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GlassGlowTile(colors: Pair<Color, Color>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0B0B10))
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(listOf(colors.first, colors.second, Color.Transparent)),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        // Faint glow biased toward one corner, matching the border's color, to suggest
        // backlit glass rather than a flat centered highlight.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.first.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset.Zero,
                        radius = 340f,
                    ),
                ),
        )
    }
}