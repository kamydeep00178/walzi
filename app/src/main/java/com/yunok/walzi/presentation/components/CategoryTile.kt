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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.size.Size
import com.yunok.walzi.domain.model.Category
import kotlinx.coroutines.launch

/** Category tiles are small (fixed 150dp height, half-screen width) - keep the decode target tiny. */
private val CATEGORY_TILE_SIZE = Size(400, 300)

/**
 * Collections tile: image with a thin light border, the name in spaced capitals and a small
 * arrow button. Squeezes slightly while pressed. When [animateIn] is true it pops in the first
 * time it appears (fade + rise + springy scale), with the two columns staggered by [index].
 */
@Composable
fun CategoryTile(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    animateIn: Boolean = false
) {
    val progress = remember(category.id) { Animatable(if (animateIn) 0f else 1f) }
    val scale = remember(category.id) { Animatable(if (animateIn) 0.88f else 1f) }
    LaunchedEffect(category.id) {
        if (progress.value < 1f) {
            launch { progress.animateTo(1f, tween(380, delayMillis = if (index % 2 == 1) 80 else 0, easing = FastOutSlowInEasing)) }
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .graphicsLayer {
                val p = progress.value
                alpha = p
                translationY = (1f - p) * 36.dp.toPx()
                scaleX = scale.value * pressScale
                scaleY = scale.value * pressScale
            }
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        ThumbImage(
            url = category.tileImageUrl,
            placeholderKey = category.id,
            size = CATEGORY_TILE_SIZE,
            contentDescription = category.name,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.matchParentSize().bottomScrim(startFraction = 0.3f, maxAlpha = 0.78f))

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name.uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
