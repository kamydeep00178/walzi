package com.yunok.walzi.presentation.category

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.presentation.components.ErrorState
import com.yunok.walzi.presentation.components.WallpaperCard
import com.yunok.walzi.presentation.components.aspectRatioFor
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.TextPrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Start fetching the next page when the last visible item is within this many items of the end. */
private const val PREFETCH_DISTANCE = 6

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

        when {
            state.isLoading && state.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent3)
            }

            state.hasError && state.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorState(onRetry = { viewModel.sendIntent(CategoryDetailIntent.Retry) })
            }

            else -> {
                val gridState = rememberLazyStaggeredGridState()
                val wallpapers = state.wallpapers
                val currentOnWallpaperClick by rememberUpdatedState(onWallpaperClick)

                // Re-evaluated on every scroll AND every list-size change (the effect restarts
                // when a page lands), so fast scrolling can't silently stall pagination.
                LaunchedEffect(gridState, wallpapers.size) {
                    if (wallpapers.isEmpty()) return@LaunchedEffect
                    snapshotFlow {
                        val info = gridState.layoutInfo
                        (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - PREFETCH_DISTANCE
                    }
                        .distinctUntilChanged()
                        .filter { nearEnd -> nearEnd }
                        .collect { viewModel.sendIntent(CategoryDetailIntent.LoadNextPage) }
                }

                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallpapers, key = { it.id }, contentType = { "wallpaper" }) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            aspectRatio = aspectRatioFor(wallpaper.id),
                            onClick = { currentOnWallpaperClick(wallpaper.id) }
                        )
                    }
                    if (state.isLoadingMore) {
                        item(key = "loading_more", span = StaggeredGridItemSpan.FullLine, contentType = "loading") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Accent3, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
