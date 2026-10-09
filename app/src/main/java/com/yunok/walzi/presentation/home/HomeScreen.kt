package com.yunok.walzi.presentation.home

import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.size.Size
import com.google.android.gms.ads.nativead.NativeAd
import com.yunok.walzi.R
import com.yunok.walzi.ads.AdViewModel
import com.yunok.walzi.ads.BannerAdComposable
import com.yunok.walzi.ads.NATIVE_ADS_PER_FEED
import com.yunok.walzi.ads.NativeAdCard
import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.DuoConfig
import com.yunok.walzi.presentation.duo.DuoBanner
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.components.CardStyle
import com.yunok.walzi.presentation.components.CategoryTile
import com.yunok.walzi.presentation.components.ErrorState
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.components.wallpaperCardsWithAds
import com.yunok.walzi.presentation.components.bottomScrim
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.BorderColor
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Left-to-right order and labels of the swipeable tab row - index must match HorizontalPager pages. */
private val FEED_TABS = listOf(
    FeedTab.RECENT to "RECENT",
    FeedTab.COLLECTIONS to "COLLECTIONS",
    FeedTab.POPULAR to "POPULAR",
    FeedTab.FAVORITES to "FAVOURITES"
)
private val TAB_ORDER = FEED_TABS.map { it.first }

/** Start fetching the next page when the last visible item is within this many items of the end. */
private const val PREFETCH_DISTANCE = 6

/** The Collections grid's single native ad sits after this many category tiles (an even
 *  number, so it starts a fresh row). */
private const val COLLECTIONS_AD_AFTER = 6

/** Cards in the Popular tab's "Top 10" deck. */
private const val DECK_COUNT = 10

/** Decode targets matched to each slot's on-screen size, so memory stays proportional to what's visible. */
private val FEATURED_IMAGE_SIZE = Size(1000, 840)

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
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenDuo: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    adViewModel: AdViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = TAB_ORDER.indexOf(state.selectedTab)) { TAB_ORDER.size }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()

    // Feeds get NATIVE_ADS_PER_FEED ads, then one for the Collections grid's tile and one for the
    // Surprise overlay (5 in total - AdMob's per-request maximum). Each ad fills one slot only.
    LaunchedEffect(adViewModel) { adViewModel.requestNativeAds(NATIVE_ADS_PER_FEED + 2) }
    val nativeAds = adViewModel.nativeAds

    // Opening a category is a natural break: a frequency-capped interstitial may show first,
    // and navigation continues once it's closed (or immediately when skipped).
    val onCategoryClickWithAd: (String) -> Unit = remember(onCategoryClick, activity, adViewModel) {
        { id -> if (activity != null) adViewModel.showInterstitial(activity) { onCategoryClick(id) } else onCategoryClick(id) }
    }

    // "Surprise me": random pick from wallpapers already cached on the phone (instant, offline).
    var showSurprise by remember { mutableStateOf(false) }
    val recentPicks = remember { mutableStateListOf<String>() }
    val surprisePool = remember(state.featuredWallpapers, state.recent.wallpapers, state.popular.wallpapers) {
        (state.featuredWallpapers + state.recent.wallpapers + state.popular.wallpapers).distinctBy { it.id }
    }
    // The button shows only the dice while the user scrolls down, the full label when scrolling up.
    var fabExpanded by remember { mutableStateOf(true) }
    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) fabExpanded = false else if (available.y > 8f) fabExpanded = true
                return Offset.Zero
            }
        }
    }

    val goToTab: (FeedTab) -> Unit = remember(pagerState, scope) {
        { tab -> scope.launch { pagerState.animateScrollToPage(TAB_ORDER.indexOf(tab)) } }
    }

    // settledPage (not currentPage): tapping a far tab animates *through* the pages in between,
    // and currentPage would report - and trigger loads for - every one of them on the way.
    LaunchedEffect(pagerState, viewModel) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.sendIntent(HomeIntent.SelectTab(TAB_ORDER[page]))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        TopAppBarRow(onOpenDrawer = onOpenDrawer, onOpenSearch = onOpenSearch, onOpenNotifications = onOpenNotifications)
        // currentPage is read *inside* FeedTabRow (via the lambda), so a page change recomposes
        // just the tab row, not this whole screen.
        FeedTabRow(selectedIndex = { pagerState.currentPage }, onSelect = goToTab)

        // Content area: the pager plus the Surprise button, which sits inside it - so it is always
        // above (never touching) the banner ad below.
        Box(modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(fabScrollConnection)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val tab = TAB_ORDER[page]

            if (tab == FeedTab.COLLECTIONS) {
                CollectionsGrid(
                    categories = state.categories,
                    duoConfig = state.duoConfig,
                    onDuoClick = {
                        viewModel.trackDuoBannerClick()
                        onOpenDuo()
                    },
                    nativeAd = nativeAds.getOrNull(NATIVE_ADS_PER_FEED),
                    onCategoryClick = { id ->
                        viewModel.trackCategoryClick(id)
                        onCategoryClickWithAd(id)
                    }
                )
            } else {
                // Per-tab lambdas capture only stable values (tab / viewModel) - never `state` -
                // so unrelated state changes don't invalidate every visible card.
                val onClick = remember(tab, onWallpaperClick) {
                    { id: String -> onWallpaperClick(id, tab.toSourceParam()) }
                }
                val onLoadMore = remember(tab, viewModel) { { viewModel.sendIntent(HomeIntent.LoadNextPage(tab)) } }
                val onRetry = remember(tab, viewModel) { { viewModel.sendIntent(HomeIntent.Retry(tab)) } }

                FeedPage(
                    tab = tab,
                    tabState = state.tab(tab),
                    featured = state.featuredWallpapers,
                    playlists = state.playlists,
                    nativeAds = nativeAds.take(NATIVE_ADS_PER_FEED),
                    onWallpaperClick = onClick,
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                    onOpenList = onOpenList,
                    onViewAllFeatured = { goToTab(FeedTab.POPULAR) }
                )
            }
        }

        if (surprisePool.size >= 3) {
            SurpriseFab(
                expanded = fabExpanded,
                onClick = { showSurprise = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
        }

        // Policy: no ads on screens without content (empty, loading or error states).
        val settledTab = TAB_ORDER[pagerState.settledPage]
        val tabHasContent = if (settledTab == FeedTab.COLLECTIONS) state.categories.isNotEmpty() else state.tab(settledTab).wallpapers.isNotEmpty()
        // Policy: also removed while the Surprise overlay covers the screen - a hidden ad must not
        // keep "showing" (and counting impressions) behind other content.
        if (tabHasContent && !showSurprise) BannerAdComposable()
    }

    // Full-screen slot-machine picker, shown above everything (incl. system bars) as a dialog.
    if (showSurprise) {
        Dialog(
            onDismissRequest = { showSurprise = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            SurpriseOverlay(
                pool = surprisePool,
                recentPicks = recentPicks,
                onPicked = { picked ->
                    recentPicks.add(0, picked.id)
                    while (recentPicks.size > 5) recentPicks.removeAt(recentPicks.lastIndex)
                },
                onSpin = viewModel::trackSurpriseSpin,
                onOpen = { picked ->
                    viewModel.trackSurpriseOpen(picked)
                    showSurprise = false
                    onWallpaperClick(picked.id, "recent")
                },
                onClose = { showSurprise = false },
                // Its own ad (not one already showing in the feed behind the overlay).
                nativeAd = nativeAds.getOrNull(NATIVE_ADS_PER_FEED + 1)
            )
        }
    }
}

@Composable
private fun FeedPage(
    tab: FeedTab,
    tabState: TabState,
    featured: List<Wallpaper>,
    playlists: List<PlaylistPreview>,
    nativeAds: List<NativeAd>,
    onWallpaperClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenList: (String) -> Unit,
    onViewAllFeatured: () -> Unit
) {
    when {
        tabState.isLoading && tabState.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent3)
        }

        tabState.hasError && tabState.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorState(onRetry = onRetry)
        }

        tab == FeedTab.FAVORITES && tabState.wallpapers.isEmpty() -> Column(Modifier.fillMaxSize()) {
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

        else -> {
            val header: (@Composable () -> Unit)? = when (tab) {
                FeedTab.RECENT -> {
                    {
                        Column {
                            FeaturedCarousel(
                                wallpapers = featured,
                                onWallpaperClick = onWallpaperClick,
                                onViewAll = onViewAllFeatured
                            )
                            YourCollectionsShelf(
                                playlists = playlists,
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
                        Column {
                            // "Top 10" playing-card deck: 10 random popular wallpapers, preferring
                            // ones beyond the first 10 so the deck doesn't repeat the grid's start.
                            // Picked once and kept while paging; re-picked only if a card is gone.
                            var deckIds by rememberSaveable { mutableStateOf(listOf<String>()) }
                            val byId = remember(tabState.wallpapers) { tabState.wallpapers.associateBy { it.id } }
                            LaunchedEffect(byId) {
                                val stale = deckIds.size < minOf(DECK_COUNT, byId.size) || deckIds.any { it !in byId }
                                if (stale) {
                                    val all = tabState.wallpapers
                                    deckIds = (all.drop(DECK_COUNT).shuffled() + all.take(DECK_COUNT).shuffled())
                                        .take(DECK_COUNT)
                                        .map { it.id }
                                }
                            }
                            val top10 = remember(deckIds, byId) { deckIds.mapNotNull { byId[it] } }
                            if (top10.size >= 3) {
                                SectionHeader(
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    title = {
                                        Row {
                                            Text("TOP 10", color = Accent3, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(" THIS WEEK", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    },
                                    subtitle = "Swipe the cards - tap one to open it"
                                )
                                PopularCardDeck(
                                    wallpapers = top10,
                                    onWallpaperClick = onWallpaperClick,
                                    modifier = Modifier.padding(bottom = 18.dp)
                                )
                            }
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

            WallpaperMasonry(
                wallpapers = tabState.wallpapers,
                isLoadingMore = tabState.isLoadingMore,
                // Native ads only belong in the two infinite feeds, not the user's own Favourites.
                nativeAds = if (tab == FeedTab.FAVORITES) emptyList() else nativeAds,
                onLoadMore = onLoadMore,
                onWallpaperClick = onWallpaperClick,
                headerContent = header,
                // Animated cards with a per-tab badge (Popular matches the Top 10 deck).
                cardStyle = when (tab) {
                    FeedTab.POPULAR -> CardStyle.POPULAR
                    FeedTab.FAVORITES -> CardStyle.FAVORITE
                    else -> CardStyle.RECENT
                }
            )
        }
    }
}

@Composable
private fun TopAppBarRow(onOpenDrawer: () -> Unit, onOpenSearch: () -> Unit, onOpenNotifications: () -> Unit) {
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
private fun FeedTabRow(selectedIndex: () -> Int, onSelect: (FeedTab) -> Unit) {
    val selected = TAB_ORDER[selectedIndex()]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        FEED_TABS.forEach { (tab, label) ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
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
 *
 * Autoplay only runs while the screen is RESUMED - it used to keep ticking (and animating)
 * with the app in the background.
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(pagerState, wallpapers.size, lifecycleOwner) {
        if (wallpapers.size < 2) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(3200)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % wallpapers.size)
                }
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
                ThumbImage(
                    url = wallpaper.gridImageUrl,
                    placeholderKey = wallpaper.id,
                    size = FEATURED_IMAGE_SIZE,
                    contentDescription = wallpaper.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.matchParentSize().bottomScrim(startFraction = 0.35f, maxAlpha = 0.7f))
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

@Composable
private fun WallpaperMasonry(
    wallpapers: List<Wallpaper>,
    isLoadingMore: Boolean,
    nativeAds: List<NativeAd>,
    onLoadMore: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    cardStyle: CardStyle? = null
) {
    val gridState = rememberLazyStaggeredGridState()
    // Cards already animated in this grid, so each animates only once.
    val dealtIds = remember { mutableSetOf<String>() }
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    // Emits true whenever the user is within PREFETCH_DISTANCE items of the end. The effect is
    // keyed on wallpapers.size so it restarts (and re-evaluates immediately) every time a page
    // lands - a plain "fire once per false->true transition" would stall if the user is still
    // near the end after a page loads, silently ending pagination with no loader shown.
    LaunchedEffect(gridState, wallpapers.size) {
        if (wallpapers.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - PREFETCH_DISTANCE
        }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect { currentOnLoadMore() }
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
            item(key = "header", span = StaggeredGridItemSpan.FullLine, contentType = "header") { content() }
        }

        wallpaperCardsWithAds(
            wallpapers = wallpapers,
            nativeAds = nativeAds,
            onWallpaperClick = onWallpaperClick,
            style = cardStyle,
            dealtIds = dealtIds
        )

        if (isLoadingMore) {
            item(key = "loading_more", span = StaggeredGridItemSpan.FullLine, contentType = "loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Accent3, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun CollectionsGrid(
    categories: List<Category>,
    duoConfig: DuoConfig?,
    onDuoClick: () -> Unit,
    nativeAd: NativeAd?,
    onCategoryClick: (String) -> Unit
) {
    // Tiles already popped in, so each animates only once.
    val shownTiles = remember { mutableSetOf<String>() }
    // One full-row native ad among the category tiles, only where tiles continue after it.
    val adAfter = if (nativeAd != null && categories.size > COLLECTIONS_AD_AFTER) COLLECTIONS_AD_AFTER else categories.size

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
        // Full-width Duo banner (rotating covers) - opens the Duo screen.
        if (duoConfig?.isVisible == true) {
            item(key = "duo_banner", span = { GridItemSpan(maxLineSpan) }, contentType = "duo_banner") {
                DuoBanner(config = duoConfig, onClick = onDuoClick)
            }
        }
        itemsIndexed(categories.subList(0, adAfter), key = { _, c -> c.id }) { index, category ->
            val animateIn = remember(category.id) { shownTiles.add(category.id) }
            CategoryTile(category = category, onClick = { onCategoryClick(category.id) }, index = index, animateIn = animateIn)
        }
        if (adAfter < categories.size) {
            item(key = "native_ad", span = { GridItemSpan(maxLineSpan) }, contentType = "native_ad") {
                NativeAdCard(nativeAd = nativeAd)
            }
            itemsIndexed(categories.subList(adAfter, categories.size), key = { _, c -> c.id }) { index, category ->
                val animateIn = remember(category.id) { shownTiles.add(category.id) }
                CategoryTile(category = category, onClick = { onCategoryClick(category.id) }, index = index, animateIn = animateIn)
            }
        }
    }
}
