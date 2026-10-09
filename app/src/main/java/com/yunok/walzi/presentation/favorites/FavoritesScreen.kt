package com.yunok.walzi.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.ads.BannerAdComposable
import com.yunok.walzi.presentation.components.AnimatedWallpaperCard
import com.yunok.walzi.presentation.components.CardStyle
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary

private fun aspectRatioFor(id: String) =
    com.yunok.walzi.presentation.components.aspectRatioFor(id, com.yunok.walzi.presentation.components.GalleryAspectRatios)

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Cards already animated in, so each animates only once.
    val dealtIds = remember { mutableSetOf<String>() }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Favorites", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.padding(start = 6.dp))
        }

        // Content takes the remaining height; the banner sits below it, never over it.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent3)
                }
                state.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = TextTertiary)
                        Text("No favorites yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        Text("Tap the heart on any wallpaper to save it here.", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.wallpapers, key = { _, w -> w.id }) { index, wallpaper ->
                        // Animates in once (dealtIds), with this screen's badge.
                        val dealIn = remember(wallpaper.id) { dealtIds.add(wallpaper.id) }
                        AnimatedWallpaperCard(
                            wallpaper = wallpaper,
                            style = CardStyle.FAVORITE,
                            rank = index + 1,
                            aspectRatio = aspectRatioFor(wallpaper.id),
                            dealIn = dealIn,
                            onClick = { onWallpaperClick(wallpaper.id) }
                        )
                    }
                }
            }
        }

        // Policy: no ads on screens without content (empty, loading or error states).
        if (!state.isLoading && state.wallpapers.isNotEmpty()) BannerAdComposable()
    }
}
