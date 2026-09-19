package com.yunok.walzi.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.yunok.walzi.R
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.components.CategoryTile
import com.yunok.walzi.presentation.components.ShimmerPlaceholder
import com.yunok.walzi.presentation.components.WallpaperCard
import com.yunok.walzi.presentation.components.placeholderColorFor
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.BorderColor
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ASPECT_RATIOS = listOf(0.55f, 0.62f, 0.5f, 0.68f, 0.58f, 0.72f)
private fun aspectRatioFor(id: String) = ASPECT_RATIOS[(id.hashCode() and 0x7fffffff) % ASPECT_RATIOS.size]

/** Left-to-right page order for the swipeable tab row - index must match HorizontalPager pages. */
private val TAB_ORDER = listOf(FeedTab.RECENT, FeedTab.COLLECTIONS, FeedTab.POPULAR, FeedTab.FAVORITES)

/** Maps the selected feed tab to the "source" query param WallpaperDetail uses to keep paging. */
private fun FeedTab.toSourceParam() = when (this) {
    FeedTab.RECENT -> "recent"
    FeedTab.POPULAR -> "popular"
    FeedTab.FAVORITES -> "favorites"
    FeedTab.COLLECTIONS -> "recent"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onWallpaperClick: (wallpaperId: String, source: String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenList: (String) -> Unit,
    onOpenSearch: () -> Unit,          // <- new
    onOpenNotifications: () -> Unit,   // <- new
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = TAB_ORDER.indexOf(state.selectedTab)) { TAB_ORDER.size }
    val scope = rememberCoroutineScope()

    fun goToTab(tab: FeedTab) {
        scope.launch { pagerState.animateScrollToPage(TAB_ORDER.indexOf(tab)) }
    }

    // Swipe settles on a new page -> load that tab, same as if it had been tapped.
    LaunchedEffect(pagerState.currentPage) {
        val tab = TAB_ORDER[pagerState.currentPage]
        if (tab != state.selectedTab) {
            viewModel.sendIntent(HomeIntent.SelectTab(tab))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        TopAppBarRow(onOpenDrawer = onOpenDrawer,onOpenSearch = onOpenSearch, onOpenNotifications = onOpenNotifications)
        FeedTabRow(
            selected = TAB_ORDER[pagerState.currentPage],
            onSelect = { tab -> goToTab(tab) }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxSize()
        ) { page ->
            val pageTab = TAB_ORDER[page]

            when {
                pageTab != state.selectedTab || state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent3)
                }

                pageTab == FeedTab.COLLECTIONS -> CollectionsGrid(
                    categories = state.categories,
                    onCategoryClick = onCategoryClick
                )

                pageTab == FeedTab.FAVORITES && state.wallpapers.isEmpty() -> Column(Modifier.fillMaxSize()) {
                    SectionHeader(
                        modifier = Modifier.padding(16.dp),
                        title = { Text("FAVOURITES", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        subtitle = "Your saved wallpapers",
                        showAccentBar = true
                    )
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyFavoritesContent()
                    }
                }

                else -> WallpaperMasonry(
                    wallpapers = state.wallpapers,
                    isLoadingMore = state.isLoadingMore,
                    onLoadMore = { viewModel.sendIntent(HomeIntent.LoadNextPage) },
                    onWallpaperClick = { id -> onWallpaperClick(id, state.selectedTab.toSourceParam()) },
                    headerContent = when (pageTab) {
                        FeedTab.RECENT -> {
                            {
                                Column {
                                    FeaturedCarousel(
                                        wallpapers = state.featuredWallpapers,
                                        onWallpaperClick = { id -> onWallpaperClick(id, state.selectedTab.toSourceParam()) },
                                        onViewAll = { goToTab(FeedTab.POPULAR) }
                                    )
                                    YourCollectionsSection(
                                        playlists = state.playlists,
                                        onOpenList = onOpenList,
                                        modifier = Modifier.padding(top = 20.dp)
                                    )
                                    Text(
                                        "DISCOVER",
                                        color = TextTertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                                    )
                                }
                            }
                        }
                        FeedTab.POPULAR -> {
                            {
                                SectionHeader(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    title = {
                                        Row {
                                            Text("POPULAR", color = Accent3, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(" WALLPAPERS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    },
                                    subtitle = "Top wallpapers loved by everyone"
                                )
                            }
                        }
                        FeedTab.FAVORITES -> {
                            {
                                SectionHeader(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    title = { Text("FAVOURITES", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                    subtitle = "Your saved wallpapers",
                                    showAccentBar = true
                                )
                            }
                        }
                        FeedTab.COLLECTIONS -> null
                    }
                )
            }
        }
    }
}

@Composable
private fun TopAppBarRow(onOpenDrawer: () -> Unit,onOpenSearch: () -> Unit, onOpenNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextPrimary)
            }
            Image(
                painter = painterResource(R.drawable.ic_wordmark),
                contentDescription = "Walzi",
                modifier = Modifier
                    .height(22.dp)
                    .padding(start = 6.dp)
            )
        }
        Row {
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextPrimary)
            }
            /*IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextPrimary)
            }*/
        }
    }
}

@Composable
private fun FeedTabRow(selected: FeedTab, onSelect: (FeedTab) -> Unit) {
    val tabs = listOf(
        FeedTab.RECENT to "Recent",
        FeedTab.COLLECTIONS to "Collections",
        FeedTab.POPULAR to "Popular",
        FeedTab.FAVORITES to "Favourites"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (active) TextPrimary else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Box(modifier = Modifier.size(width = if (active) 22.dp else 0.dp, height = 2.5.dp).background(Accent3))
            }
        }
    }
    androidx.compose.material3.Divider(color = BorderColor)
}

/**
 * Shared header row used by Popular, Favourites, and (as an inline-built equivalent) Recent's
 * Today's Picks carousel: a title slot (so callers can mix colors, e.g. Popular's two-tone
 * title), a subtitle line beneath it, an optional small accent bar to the left (Favourites),
 * and an optional "View all" link on the right (only Recent's carousel uses this today).
 */
@Composable
private fun SectionHeader(
    title: @Composable () -> Unit,
    subtitle: String,
    modifier: Modifier = Modifier,
    showAccentBar: Boolean = false,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showAccentBar) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Accent3)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column {
                title()
                Text(subtitle, color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onViewAll != null) {
            Row(
                modifier = Modifier.clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View all", color = Accent3, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Accent3, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyFavoritesContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = TextTertiary)
        Text(
            "No favourites yet",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            "Tap the heart on any wallpaper to save it here.",
            color = TextTertiary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Large center-focused "peek" carousel, auto-advancing on its own - the next/previous cards
 * peek in from the screen edges (via HorizontalPager's contentPadding), and it snaps forward
 * one page every ~3.2s unless the user is actively dragging it. Fed the day's country+category
 * pick from HomeViewModel (see WallpaperRepository.observeFeaturedWallpapers).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedCarousel(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (String) -> Unit,
    onViewAll: () -> Unit
) {
    if (wallpapers.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { wallpapers.size })

    LaunchedEffect(pagerState, wallpapers.size) {
        while (true) {
            delay(3200)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % wallpapers.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        SectionHeader(
            modifier = Modifier.padding(bottom = 12.dp),
            title = { Text("TODAY'S PICKS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            subtitle = "Curated wallpapers for you",
            onViewAll = onViewAll
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 34.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) { page ->
            val wallpaper = wallpapers[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onWallpaperClick(wallpaper.id) }
            ) {
                SubcomposeAsyncImage(
                    model = wallpaper.imageUrl,
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        ShimmerPlaceholder(baseColor = placeholderColorFor(wallpaper.id), modifier = Modifier.fillMaxSize())
                    },
                    error = {
                        ShimmerPlaceholder(baseColor = placeholderColorFor(wallpaper.id), modifier = Modifier.fillMaxSize())
                    },
                    success = { SubcomposeAsyncImageContent() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 0.35f
                            )
                        )
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Accent3.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("FEATURED", color = Accent3, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(
                        wallpaper.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "${wallpaper.categoryName} • ${wallpaper.resolution}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            wallpapers.indices.forEach { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) Accent3 else Elevated2)
                )
            }
        }
    }
}

/**
 * "Folder card" style: circular icon, title + count, chevron on the right, and a strip of up
 * to 3 small thumbnails below. Shows only the user's real custom/default lists - no synthetic
 * Favourites entry here, since Favourites already has its own tab with its own proper header.
 */
@Composable
private fun YourCollectionsSection(
    playlists: List<PlaylistPreview>,
    onOpenList: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            "YOUR COLLECTIONS",
            color = TextTertiary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CollectionCard(
                icon = Icons.Filled.Image,
                iconTint = Accent3,
                title = playlists[0].list.name,
                count = playlists[0].list.wallpaperIds.size,
                previewImageUrls = playlists[0].previewImageUrls,
                onClick = { onOpenList(playlists[0].list.id) },
                modifier = Modifier.weight(1f)
            )
            if (playlists.size > 1) {
                CollectionCard(
                    icon = Icons.Filled.Image,
                    iconTint = Accent3,
                    title = playlists[1].list.name,
                    count = playlists[1].list.wallpaperIds.size,
                    previewImageUrls = playlists[1].previewImageUrls,
                    onClick = { onOpenList(playlists[1].list.id) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(Modifier.weight(1f))
            }
        }
        // Any further custom lists beyond the first two show as additional full-width rows,
        // keeping the layout predictable regardless of how many lists exist.
        playlists.drop(2).forEach { preview ->
            CollectionCard(
                icon = Icons.Filled.Image,
                iconTint = Accent3,
                title = preview.list.name,
                count = preview.list.wallpaperIds.size,
                previewImageUrls = preview.previewImageUrls,
                onClick = { onOpenList(preview.list.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun CollectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    previewImageUrls: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
                Text(
                    "$count wallpaper${if (count == 1) "" else "s"}",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }

        if (previewImageUrls.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                previewImageUrls.take(3).forEach { url ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        SubcomposeAsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { ShimmerPlaceholder(baseColor = placeholderColorFor(url), modifier = Modifier.fillMaxSize()) },
                            error = { ShimmerPlaceholder(baseColor = placeholderColorFor(url), modifier = Modifier.fillMaxSize()) },
                            success = { SubcomposeAsyncImageContent() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperMasonry(
    wallpapers: List<Wallpaper>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    headerContent: (@Composable () -> Unit)? = null
) {
    val gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()

    // Fires "load next page" whenever the user is within 6 items of the end - re-evaluated on
    // every scroll AND every time the list grows (via the wallpapers.size key), not just once
    // per true/false transition. A plain LaunchedEffect(booleanFlag) would only fire the very
    // first time the threshold is crossed - if the user keeps scrolling fast enough to stay
    // within that threshold across multiple loaded pages, it would never fire again, silently
    // stalling pagination with no loader shown.
    LaunchedEffect(gridState, wallpapers.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (wallpapers.isNotEmpty() && lastVisible >= wallpapers.size - 6) {
                    onLoadMore()
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
        headerContent?.let { content ->
            item(span = StaggeredGridItemSpan.FullLine) { content() }
        }

        items(wallpapers, key = { it.id }) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                aspectRatio = aspectRatioFor(wallpaper.id),
                onClick = { onWallpaperClick(wallpaper.id) }
            )
        }
        if (isLoadingMore) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Accent3, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun CollectionsGrid(
    categories: List<com.yunok.walzi.domain.model.Category>,
    onCategoryClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                modifier = Modifier.padding(bottom = 14.dp),
                title = { Text("EXPLORE COLLECTIONS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                subtitle = "Beautiful themes for every mood and moment.",
            )
        }
        items(categories, key = { it.id }) { category ->
            CategoryTile(category = category, onClick = { onCategoryClick(category.id) })
        }
    }
}