package com.yunok.walzi.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.domain.model.Tag
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.components.WallpaperCard
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary

private val ASPECT_RATIOS = listOf(0.55f, 0.62f, 0.5f, 0.68f, 0.58f, 0.72f)
private fun aspectRatioFor(id: String) = ASPECT_RATIOS[(id.hashCode() and 0x7fffffff) % ASPECT_RATIOS.size]

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onWallpaperClick: (String, String) -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        SearchBar(
            query = state.query,
            onQueryChange = { viewModel.sendIntent(SearchIntent.QueryChanged(it)) },
            onClear = { viewModel.sendIntent(SearchIntent.ClearQuery) },
            onBack = onBack
        )

        when {
            state.selectedTag == null -> TagSuggestionsContent(
                state = state,
                onTagTapped = { viewModel.sendIntent(SearchIntent.TagTapped(it)) },
                onCategoryClick = onCategoryClick
            )

            state.isLoadingResults -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent3)
            }

            state.resultsError != null -> SearchErrorState(message = state.resultsError!!)

            state.wallpaperResults.isEmpty() -> NoResultsState(tag = state.selectedTag!!)

            else -> WallpaperResultsGrid(
                tag = state.selectedTag!!,
                wallpapers = state.wallpaperResults,
                onWallpaperClick = { id -> onWallpaperClick(id, state.selectedTag!!) }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Elevated)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent3),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search by tag - try smile, sunset...", color = TextTertiary, fontSize = 14.sp)
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun TagSuggestionsContent(
    state: SearchState,
    onTagTapped: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (state.query.isBlank() && state.recentTagSearches.isNotEmpty()) {
            SectionLabel("RECENT SEARCHES")
            ChipRow(items = state.recentTagSearches, onClick = onTagTapped)
        }

        if (state.matchingCategories.isNotEmpty()) {
            SectionLabel("MATCHING CATEGORIES", topPadding = 20.dp)
            ChipRow(items = state.matchingCategories.map { it.name }, onClick = { name ->
                state.matchingCategories.firstOrNull { it.name == name }?.let { onCategoryClick(it.id) }
            })
        }

        if (state.matchingTags.isNotEmpty()) {
            SectionLabel("MATCHING TAGS", topPadding = 20.dp)
            TagChipRow(tags = state.matchingTags, onClick = onTagTapped)
        } else if (state.nearTags.isNotEmpty()) {
            SectionLabel("NEAR MATCHES", topPadding = 20.dp)
            TagChipRow(tags = state.nearTags, onClick = onTagTapped)
        }

        if (state.otherTags.isNotEmpty()) {
            SectionLabel("OTHER TAGS", topPadding = 20.dp)
            TagChipRow(tags = state.otherTags, onClick = onTagTapped)
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text,
        color = TextTertiary,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = topPadding, bottom = 10.dp)
    )
}

@Composable
private fun ChipRow(items: List<String>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Elevated)
                    .clickable { onClick(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, color = Accent3, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TagChipRow(tags: List<Tag>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags, key = { it.name }) { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Elevated)
                    .clickable { onClick(tag.name) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag.name, color = Accent3, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (tag.wallpaperCount > 0) {
                        Text(
                            " · ${tag.wallpaperCount}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary)
            Text("Search couldn't complete", color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            Text(message, color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun NoResultsState(tag: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary)
            Text("No wallpapers tagged \"$tag\"", color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            Text("Try another tag from the suggestions.", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun WallpaperResultsGrid(tag: String, wallpapers: List<Wallpaper>, onWallpaperClick: (String) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                "\"$tag\" · ${wallpapers.size} RESULT${if (wallpapers.size == 1) "" else "S"}",
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        items(wallpapers, key = { it.id }) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                aspectRatio = aspectRatioFor(wallpaper.id),
                onClick = { onWallpaperClick(wallpaper.id) }
            )
        }
    }
}