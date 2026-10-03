package com.yunok.walzi.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.size.Size
import com.yunok.walzi.domain.model.Category

/** Category tiles are small (fixed 150dp height, half-screen width) - keep the decode target tiny. */
private val CATEGORY_TILE_SIZE = Size(400, 300)

@Composable
fun CategoryTile(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        ThumbImage(
            url = category.tileImageUrl,
            placeholderKey = category.id,
            size = CATEGORY_TILE_SIZE,
            contentDescription = category.name,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.matchParentSize().bottomScrim(startFraction = 0.3f, maxAlpha = 0.75f))
        Text(
            text = category.name.uppercase(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        )
    }
}
