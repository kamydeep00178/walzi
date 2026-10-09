package com.yunok.walzi.presentation.wallpaperdetail

import kotlin.math.absoluteValue
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import com.yunok.walzi.util.WallpaperAdjustments
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.BiasAlignment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.activity.compose.BackHandler
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yunok.walzi.ads.AdFreeManager
import com.yunok.walzi.ads.AdViewModel
import com.yunok.walzi.ads.AdsConfig
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.components.ErrorState
import com.yunok.walzi.util.GRID_THUMBNAIL_SIZE
import com.yunok.walzi.util.ImageLoadProgress
import com.yunok.walzi.util.findActivity
import com.yunok.walzi.util.thumbMemoryKey
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.WallpaperTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen wallpaper preview with TikTok/Reels-style vertical swipe between wallpapers.
 * Swiping down (or up) pages through the same list the user tapped into (feed / recent /
 * popular / category / favorites); more pages load automatically a few swipes before the
 * end via [WallpaperDetailIntent.LoadNextPage].
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailScreen(
    onBack: () -> Unit,
    viewModel: WallpaperDetailViewModel = hiltViewModel(),
    adViewModel: AdViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WallpaperDetailEffect.ShowMessage -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.text,
                        actionLabel = if (effect.undoable) "Undo" else null,
                        // Long when undoable: an interstitial may cover the first seconds.
                        duration = if (effect.undoable) SnackbarDuration.Long else SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.sendIntent(WallpaperDetailIntent.Undo)
                }
                // Frequency-capped inside InterstitialAdManager; a no-op when ads are disabled.
                WallpaperDetailEffect.ActionCompleted -> activity?.let { adViewModel.showInterstitial(it) }
            }
        }
    }

    // Wallpaper id the "Watch an ad to download" dialog is open for, if any.
    var rewardPromptFor by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Accent1)
        } else if (state.hasError || state.wallpapers.isEmpty()) {
            WallpaperOverlayTopBar(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
            ErrorState(
                onRetry = { viewModel.sendIntent(WallpaperDetailIntent.Retry) },
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val pagerState = rememberPagerState(initialPage = state.initialIndex) { state.wallpapers.size }
            val currentWallpaper = state.wallpapers.getOrNull(
                pagerState.currentPage.coerceIn(0, state.wallpapers.lastIndex)
            )

            // Analytics: one wallpaper_view per wallpaper the pager settles on (the VM dedupes).
            val settledWallpaperId = state.wallpapers.getOrNull(pagerState.settledPage)?.id
            LaunchedEffect(settledWallpaperId) {
                settledWallpaperId?.let(viewModel::onWallpaperViewed)
            }

            // Adjust (dim / blur / brightness / position) belongs to the wallpaper on screen and
            // resets when the user swipes to another one. Preview mockup / Adjust sheet state.
            var adjustments by remember(settledWallpaperId) { mutableStateOf(WallpaperAdjustments.NONE) }
            var mockupMode by remember { mutableStateOf<MockupMode?>(null) }
            var showAdjustSheet by remember { mutableStateOf(false) }
            BackHandler(enabled = mockupMode != null) { mockupMode = null }

            // Prefetch the next page a few swipes before the user actually hits the end.
            LaunchedEffect(pagerState.currentPage, state.wallpapers.size) {
                if (pagerState.currentPage >= state.wallpapers.size - 3) {
                    viewModel.sendIntent(WallpaperDetailIntent.LoadNextPage)
                }
            }

            // The download waiting for the storage permission (pre-Android 10 only).
            var pendingDownload by remember { mutableStateOf<WallpaperDetailIntent.Download?>(null) }
            val storagePermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) pendingDownload?.let(viewModel::sendIntent)
                pendingDownload = null
            }

            fun startDownload(wallpaperId: String, viaReward: Boolean) {
                val download = WallpaperDetailIntent.Download(wallpaperId, viaReward)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    pendingDownload = download
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    viewModel.sendIntent(download)
                }
            }

            // Download is offered behind an opt-in rewarded ad. Whenever no ad can be shown
            // (ads off, ad-free, no fill / offline) it simply downloads - never blocked.
            fun requestDownload(wallpaperId: String) {
                if (AdsConfig.ADS_ENABLED && !AdFreeManager.isAdFreeNow() && adViewModel.isRewardedReady) {
                    rewardPromptFor = wallpaperId
                } else {
                    startDownload(wallpaperId, viaReward = false)
                }
            }

            rewardPromptFor?.let { wallpaperId ->
                AlertDialog(
                    onDismissRequest = { rewardPromptFor = null },
                    containerColor = Elevated,
                    title = { Text("Download wallpaper", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Watch a short video ad to save this wallpaper to your gallery in full quality.",
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            rewardPromptFor = null
                            val host = activity
                            if (host == null) {
                                startDownload(wallpaperId, viaReward = false)
                            } else {
                                adViewModel.showRewarded(host) { earned ->
                                    if (earned) {
                                        startDownload(wallpaperId, viaReward = true)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Watch the full video to download.")
                                        }
                                    }
                                }
                            }
                        }) { Text("Watch ad", color = Accent1, fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { rewardPromptFor = null }) { Text("Cancel", color = TextSecondary) }
                    }
                )
            }

            // Opening animation: the image eases in (slight zoom-out + fade) and the bars slide in.
            val entry = remember { Animatable(0f) }
            LaunchedEffect(Unit) { entry.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }

            // Only the image itself lives inside the pager, so only the image moves during a
            // vertical swipe. Buttons/text below are a separate fixed overlay in the same Box.
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val e = entry.value
                        alpha = e
                        scaleX = 1.06f - 0.06f * e
                        scaleY = 1.06f - 0.06f * e
                    },
                // Compose the next/previous wallpaper before the swipe reaches it, so it isn't
                // blank when it slides in. Cheap: neighbours only ever load their thumbnail -
                // the original waits until the user actually settles on that page.
                beyondViewportPageCount = 1
            ) { page ->
                val isSettledHere by remember(page) { derivedStateOf { pagerState.settledPage == page } }
                // Swipe "depth" effect: a page moving away shrinks, rounds its corners and dims,
                // like flipping through a stack of cards. Follows the finger exactly; read in
                // the draw phase only, so swiping never recomposes the page.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val f = offset.absoluteValue.coerceIn(0f, 1f)
                            val s = lerp(1f, 0.86f, f)
                            scaleX = s
                            scaleY = s
                            alpha = lerp(1f, 0.45f, f)
                            shape = RoundedCornerShape((28f * f).dp)
                            clip = f > 0f
                        }
                ) {
                    PreviewPage(
                        wallpaper = state.wallpapers[page],
                        isSettledHere = isSettledHere,
                        // Live preview of the adjustments on the wallpaper being edited only.
                        adjustments = if (isSettledHere) adjustments else WallpaperAdjustments.NONE
                    )
                }
            }

            // "Preview on my phone": the mockup replaces all controls until closed.
            mockupMode?.let { mode ->
                PhoneMockupOverlay(
                    mode = mode,
                    onModeChange = { mockupMode = it },
                    onClose = { mockupMode = null }
                )
            }

            // Fixed overlay: position stays put regardless of swipe progress. Only the data it
            // shows (title, favorite state, action targets) updates once a swipe settles on a
            // new page, via currentWallpaper.
            if (currentWallpaper != null && mockupMode == null) {
                WallpaperOverlayTopBar(
                    onBack = onBack,
                    onPreview = { mockupMode = MockupMode.HOME },
                    onAdjust = { showAdjustSheet = true },
                    isAdjusted = !adjustments.isDefault,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = entry.value
                            translationY = (1f - entry.value) * -40.dp.toPx()
                        }
                )

                // Drawn after the top bar so it sits above its gradient, at the very top edge.
                OriginalLoadProgressBar(
                    imageUrl = currentWallpaper.imageUrl,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                WallpaperOverlayBottomBar(
                    wallpaper = currentWallpaper,
                    isFavorite = currentWallpaper.id in state.favoriteIds,
                    isApplyingWallpaper = state.isApplyingWallpaper && state.targetWallpaperId == currentWallpaper.id,
                    isDownloading = state.isDownloading,
                    onToggleFavorite = { viewModel.sendIntent(WallpaperDetailIntent.ToggleFavorite(currentWallpaper.id)) },
                    onOpenAddToList = { viewModel.sendIntent(WallpaperDetailIntent.OpenAddToListSheet(currentWallpaper.id)) },
                    onDownload = { requestDownload(currentWallpaper.id) },
                    onOpenSetSheet = { viewModel.sendIntent(WallpaperDetailIntent.OpenSetWallpaperSheet(currentWallpaper.id)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            alpha = entry.value
                            translationY = (1f - entry.value) * 60.dp.toPx()
                        }
                )
            }

            // Small position indicator, e.g. "4 / 20" - confirms there's more to swipe through.
       /*     Text(
                text = "${pagerState.currentPage + 1} / ${state.wallpapers.size}${if (!state.endReached) "+" else ""}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )*/

            if (state.showTargetSheet) {
                val target = state.wallpapers.firstOrNull { it.id == state.targetWallpaperId }
                ModalBottomSheet(
                    onDismissRequest = { viewModel.sendIntent(WallpaperDetailIntent.DismissSetWallpaperSheet) },
                    // Open fully, so every option and the adjust note are visible.
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Elevated
                ) {
                    SetWallpaperSheetContent(
                        wallpaperTitle = target?.title.orEmpty(),
                        isAdjusted = !adjustments.isDefault,
                        onSelect = { t ->
                            target?.let {
                                viewModel.sendIntent(WallpaperDetailIntent.ConfirmSetWallpaper(it.id, t, adjustments))
                            }
                        }
                    )
                }
            }

            if (showAdjustSheet) {
                // Transparent scrim so the live preview behind the sheet stays fully visible.
                ModalBottomSheet(
                    onDismissRequest = { showAdjustSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Elevated.copy(alpha = 0.96f),
                    scrimColor = Color.Transparent
                ) {
                    AdjustSheetContent(
                        adjustments = adjustments,
                        onChange = { adjustments = it },
                        onDone = { showAdjustSheet = false }
                    )
                }
            }

            if (state.showAddToListSheet) {
                val targetId = state.addToListWallpaperId.orEmpty()
                ModalBottomSheet(
                    onDismissRequest = { viewModel.sendIntent(WallpaperDetailIntent.DismissAddToListSheet) },
                    containerColor = Elevated
                ) {
                    AddToListSheetContent(
                        lists = state.availableLists,
                        wallpaperId = targetId,
                        newListNameDraft = state.newListNameDraft,
                        onToggleList = { listId, currentlyIn ->
                            viewModel.sendIntent(WallpaperDetailIntent.ToggleWallpaperInList(targetId, listId, currentlyIn))
                        },
                        onNewListNameChange = { viewModel.sendIntent(WallpaperDetailIntent.UpdateNewListNameDraft(it)) },
                        onCreateAndAdd = { viewModel.sendIntent(WallpaperDetailIntent.CreateListAndAddWallpaper(targetId)) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

/** How long the user must stay on a wallpaper before its full-resolution original loads. */
private const val ORIGINAL_LOAD_DELAY_MS = 500L

/**
 * One full-screen page, in two layers:
 *  1. The thumbnail - the exact same request as the grid card (url, size, memory key), so it's
 *     painted instantly from the memory cache, or fetched cheaply if it isn't cached.
 *  2. The original, added on top only after the user has stayed on this page for
 *     [ORIGINAL_LOAD_DELAY_MS]. Swiping past quickly never downloads it. It fades in over the
 *     thumbnail, and if it fails the thumbnail simply stays.
 *
 * The original decodes at the page's on-screen size (Coil measures it), not Size.ORIGINAL - a 4K
 * source is ~33 MB decoded. Set Wallpaper / Download still use the full-resolution file
 * (see WallpaperSetter / ImageDownloader).
 */
@Composable
private fun PreviewPage(
    wallpaper: Wallpaper,
    isSettledHere: Boolean,
    adjustments: WallpaperAdjustments = WallpaperAdjustments.NONE
) {
    val context = LocalContext.current
    // Live preview of Adjust: same crop alignment, colour scale and (API 31+) blur that
    // WallpaperSetter applies to the real bitmap.
    val alignment = BiasAlignment(adjustments.position, 0f)
    val scale = adjustments.colorScale
    val colorFilter = remember(scale) {
        if (scale == 1f) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(scale, scale, scale, 1f) })
    }
    val imageModifier = Modifier
        .fillMaxSize()
        .then(if (adjustments.blur > 0f) Modifier.blur((adjustments.blur * WallpaperAdjustments.MAX_BLUR_DP).dp) else Modifier)
    // Once loaded, the original stays while this page is composed, so swiping back is instant.
    var loadOriginal by remember(wallpaper.id) { mutableStateOf(false) }

    LaunchedEffect(wallpaper.id, isSettledHere) {
        if (!isSettledHere || loadOriginal) return@LaunchedEffect
        // Without a separate thumbnail, the grid already downloaded the original file - waiting
        // would save no data, so show the sharp version straight away.
        if (wallpaper.thumbUrl.isNotEmpty()) delay(ORIGINAL_LOAD_DELAY_MS)
        loadOriginal = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = remember(wallpaper.id) {
                ImageRequest.Builder(context)
                    .data(wallpaper.gridImageUrl)
                    .size(GRID_THUMBNAIL_SIZE)
                    .memoryCacheKey(thumbMemoryKey(wallpaper.imageUrl))
                    .build()
            },
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            colorFilter = colorFilter,
            modifier = imageModifier
        )
        if (loadOriginal) {
            AsyncImage(
                model = remember(wallpaper.id) {
                    ImageRequest.Builder(context).data(wallpaper.imageUrl).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = alignment,
                colorFilter = colorFilter,
                modifier = imageModifier
            )
        }
    }
}

/**
 * Thin bar at the top showing the *real* download progress of the current wallpaper's original
 * (bytes received / file size). Hidden while only the thumbnail is shown, when the original comes
 * from cache, and once the download finishes. Indeterminate if the server sends no file size.
 *
 * Collects the progress itself, so its ~100 updates per download recompose only this bar.
 */
@Composable
private fun OriginalLoadProgressBar(imageUrl: String, modifier: Modifier = Modifier) {
    val progressFlow = remember(imageUrl) { ImageLoadProgress.track(imageUrl) }
    DisposableEffect(imageUrl) {
        onDispose { ImageLoadProgress.untrack(imageUrl) }
    }
    val progress by progressFlow.collectAsState()
    val current = progress ?: return

    val barModifier = modifier.fillMaxWidth().height(3.dp)
    val trackColor = Color.White.copy(alpha = 0.15f)
    if (current == ImageLoadProgress.UNKNOWN) {
        LinearProgressIndicator(modifier = barModifier, color = Accent1, trackColor = trackColor)
    } else {
        val animated by animateFloatAsState(targetValue = current, label = "originalLoadProgress")
        LinearProgressIndicator(
            progress = { animated },
            modifier = barModifier,
            color = Accent1,
            trackColor = trackColor
        )
    }
}

@Composable
private fun WallpaperOverlayTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPreview: (() -> Unit)? = null,
    onAdjust: (() -> Unit)? = null,
    isAdjusted: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
            .padding(top = 20.dp, bottom = 40.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
        Spacer(Modifier.weight(1f))
        if (onPreview != null) {
            CircleIconButton(icon = Icons.Filled.Visibility, contentDescription = "Preview on my phone", onClick = onPreview)
        }
        if (onAdjust != null) {
            Spacer(Modifier.width(10.dp))
            CircleIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = "Adjust",
                // Accent tint = adjustments are active and will be applied.
                tint = if (isAdjusted) Accent1 else Color.White,
                onClick = onAdjust
            )
        }
    }
}

@Composable
private fun WallpaperOverlayBottomBar(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    isApplyingWallpaper: Boolean,
    isDownloading: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAddToList: () -> Unit,
    onDownload: () -> Unit,
    onOpenSetSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(top = 50.dp, bottom = 22.dp, start = 20.dp, end = 20.dp)
    ) {
        // The title slides up and fades into the next one when the wallpaper changes.
        AnimatedContent(
            targetState = wallpaper.title,
            transitionSpec = {
                (slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(320))) togetherWith
                    (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(220)))
            },
            label = "title"
        ) { title ->
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Heart "pop" when favourite is toggled (not when swiping to another wallpaper).
        val heartScale = remember { Animatable(1f) }
        var lastFavorite by remember(wallpaper.id) { mutableStateOf(isFavorite) }
        LaunchedEffect(wallpaper.id, isFavorite) {
            if (isFavorite != lastFavorite) {
                lastFavorite = isFavorite
                heartScale.snapTo(0.6f)
                heartScale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
            }
        }

        // All four actions in one horizontal row: Favorite, Add to List, Download (fixed-size
        // icon buttons), then Set Wallpaper taking the remaining width.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Accent2 else Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = heartScale.value
                        scaleY = heartScale.value
                    }
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onOpenAddToList),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to list", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(enabled = !isDownloading, onClick = onDownload),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(GradientSignature))
                    .clickable(enabled = !isApplyingWallpaper, onClick = onOpenSetSheet)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isApplyingWallpaper) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Set Wallpaper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SetWallpaperSheetContent(
    wallpaperTitle: String,
    isAdjusted: Boolean,
    onSelect: (WallpaperTarget) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).padding(bottom = 26.dp)) {
        Text(
            "Set \"$wallpaperTitle\" as wallpaper for…",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        SheetOption(Icons.Filled.PhoneAndroid, "Home Screen", "Shown behind your app icons") { onSelect(WallpaperTarget.HOME) }
        SheetOption(Icons.Filled.LockClock, "Lock Screen", "Shown when your phone is locked") { onSelect(WallpaperTarget.LOCK) }
        SheetOption(Icons.Filled.Check, "Home & Lock Screen", "Set on both screens at once") { onSelect(WallpaperTarget.BOTH) }
        if (isAdjusted) {
            Text("Your Adjust settings will be applied.", color = TextTertiary, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Elevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent1)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = TextTertiary, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun AddToListSheetContent(
    lists: List<WallpaperList>,
    wallpaperId: String,
    newListNameDraft: String,
    onToggleList: (listId: String, currentlyIn: Boolean) -> Unit,
    onNewListNameChange: (String) -> Unit,
    onCreateAndAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).padding(bottom = 26.dp)) {
        Text(
            "Add to list",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        lists.forEach { list ->
            val isIn = wallpaperId in list.wallpaperIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .clickable { onToggleList(list.id, isIn) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isIn,
                    onCheckedChange = { onToggleList(list.id, isIn) },
                    colors = CheckboxDefaults.colors(checkedColor = Accent1)
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(list.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${list.wallpaperIds.size} wallpapers", color = TextTertiary, fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newListNameDraft,
                onValueChange = onNewListNameChange,
                placeholder = { Text("New list name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onCreateAndAdd,
                enabled = newListNameDraft.isNotBlank(),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text("Add")
            }
        }
    }
}