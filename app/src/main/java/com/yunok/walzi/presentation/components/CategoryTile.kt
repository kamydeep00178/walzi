package com.yunok.walzi.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
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
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = remember(category.imageUrl) {
                ImageRequest.Builder(context)
                    .data(category.imageUrl)
                    .size(CATEGORY_TILE_SIZE)
                    .crossfade(true)
                    .build()
            },
            contentDescription = category.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerPlaceholder(baseColor = placeholderColorFor(category.id), modifier = Modifier.fillMaxSize()) },
            error = { ShimmerPlaceholder(baseColor = placeholderColorFor(category.id), modifier = Modifier.fillMaxSize()) },
            success = { SubcomposeAsyncImageContent() }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 0.3f
                    )
                )
        )
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