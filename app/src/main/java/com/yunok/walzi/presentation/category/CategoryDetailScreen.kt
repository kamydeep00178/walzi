package com.yunok.walzi.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.presentation.components.WallpaperCard
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.TextPrimary

private val ASPECT_RATIOS = listOf(0.55f, 0.62f, 0.5f, 0.68f, 0.58f, 0.72f)
private fun aspectRatioFor(id: String) = ASPECT_RATIOS[(id.hashCode() and 0x7fffffff) % ASPECT_RATIOS.size]

@Composable
fun CategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = state.categoryName.ifEmpty { "Category" },
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = TextPrimary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent3)
            }
        } else {
            val gridState = rememberLazyStaggeredGridState()
            // Same fix as HomeScreen's WallpaperMasonry: re-evaluate on every scroll AND every
            // list-size change, not just once per boolean transition, or fast scrolling can
            // stall pagination silently after the first page.
            LaunchedEffect(gridState, state.wallpapers.size) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                    .collect { lastVisible ->
                        if (state.wallpapers.isNotEmpty() && lastVisible >= state.wallpapers.size - 6) {
                            viewModel.sendIntent(CategoryDetailIntent.LoadNextPage)
                        }
                    }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.wallpapers, key = { it.id }) { wallpaper ->
                    WallpaperCard(
                        wallpaper = wallpaper,
                        aspectRatio = aspectRatioFor(wallpaper.id),
                        onClick = { onWallpaperClick(wallpaper.id) }
                    )
                }
                if (state.isLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Accent3, strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}