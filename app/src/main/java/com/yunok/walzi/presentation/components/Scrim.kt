package com.yunok.walzi.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Bottom-weighted dark gradient so overlaid titles stay legible on any image.
 *
 * [startFraction] is a *fraction of the height* (0..1) where the gradient begins. (Brush's own
 * startY is in pixels - passing 0.4f there, as the old code did, starts the fade at ~0px, i.e.
 * darkens the whole image.) The brush is built once per size in drawWithCache, not per recomposition.
 */
fun Modifier.bottomScrim(startFraction: Float, maxAlpha: Float): Modifier = drawWithCache {
    val brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = maxAlpha)),
        startY = size.height * startFraction,
        endY = size.height
    )
    onDrawBehind { drawRect(brush) }
}
