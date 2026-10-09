package com.yunok.walzi.presentation.duo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.size.Size
import com.yunok.walzi.ads.AdViewModel
import com.yunok.walzi.ads.BannerAdComposable
import com.yunok.walzi.domain.model.Duo
import com.yunok.walzi.presentation.components.ErrorState
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.WallpaperTarget
import com.yunok.walzi.util.findActivity
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Phone-frame thumbnail decode size (portrait). */
private val FRAME_SIZE = Size(600, 1300)

/**
 * All Duos as swipeable cards. Each card shows the pair side by side as two phone frames
 * (Lock | Home); swipe left/right for the next Duo. Set Duo applies both at once; Lock only /
 * Home only apply one half. Tap a frame to see that image full screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuoScreen(
    onBack: () -> Unit,
    viewModel: DuoViewModel = hiltViewModel(),
    adViewModel: AdViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    var fullScreenUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DuoEvent.Message -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = event.text,
                        actionLabel = if (event.undoable) "Undo" else null,
                        duration = if (event.undoable) SnackbarDuration.Long else SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo()
                }
                // Frequency-capped inside InterstitialAdManager; a no-op when ads are off.
                DuoEvent.Applied -> activity?.let { adViewModel.showInterstitial(it) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text(state.title, fontWeight = FontWeight.Black, fontSize = 19.sp, color = TextPrimary, letterSpacing = 2.sp)
                    if (state.subtitle.isNotBlank()) {
                        Text(state.subtitle, fontSize = 11.5.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && state.duos.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Accent3)
                    state.hasError ->
                        ErrorState(onRetry = viewModel::retry, modifier = Modifier.align(Alignment.Center))
                    state.duos.isEmpty() ->
                        Text("No Duos yet - check back soon.", color = TextTertiary, modifier = Modifier.align(Alignment.Center))
                    else -> DuoPager(
                        duos = state.duos,
                        applying = state.applying,
                        onViewed = viewModel::onDuoViewed,
                        onApply = viewModel::apply,
                        onOpenFullScreen = { fullScreenUrl = it }
                    )
                }
            }

            // Policy: banner only when the screen has content.
            if (state.duos.isNotEmpty()) BannerAdComposable()
        }

        fullScreenUrl?.let { url ->
            BackHandler { fullScreenUrl = null }
            FullScreenImage(url = url, onClose = { fullScreenUrl = null })
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DuoPager(
    duos: List<Duo>,
    applying: Pair<String, WallpaperTarget>?,
    onViewed: (Duo) -> Unit,
    onApply: (Duo, WallpaperTarget) -> Unit,
    onOpenFullScreen: (String) -> Unit
) {
    val pagerState = rememberPagerState { duos.size }
    val settled = duos.getOrNull(pagerState.settledPage)
    LaunchedEffect(settled?.id) { settled?.let(onViewed) }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 30.dp),
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val duo = duos[page]
            // Neighbouring cards shrink and fade slightly - a modern "carousel" feel.
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            DuoCard(
                duo = duo,
                onOpenFullScreen = onOpenFullScreen,
                modifier = Modifier.graphicsLayer {
                    val scale = lerp(1f, 0.9f, offset)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(1f, 0.55f, offset)
                }
            )
        }

        // Position dots.
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val current = pagerState.currentPage
            duos.indices.forEach { i ->
                if ((i - current).absoluteValue <= 4) {
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (i == current) 16.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (i == current) Accent3 else Elevated2)
                    )
                }
            }
        }

        val current = duos.getOrNull(pagerState.currentPage)
        if (current != null) {
            DuoActions(
                busyTarget = applying?.takeIf { it.first == current.id }?.second,
                enabled = applying == null,
                onApply = { target -> onApply(current, target) }
            )
        }
    }
}

@Composable
private fun DuoCard(duo: Duo, onOpenFullScreen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(Surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PhoneFrame(url = duo.lockCardUrl, label = "Lock", isLock = true, onClick = { onOpenFullScreen(duo.lockImageUrl) }, modifier = Modifier.weight(1f))
            PhoneFrame(url = duo.homeCardUrl, label = "Home", isLock = false, onClick = { onOpenFullScreen(duo.homeImageUrl) }, modifier = Modifier.weight(1f))
        }
        Text(
            duo.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text("Tap a screen to view it full size", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A portrait "phone" with the wallpaper, a hint of its screen (clock / dock) and a label. */
@Composable
private fun PhoneFrame(url: String, label: String, isLock: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 19.5f)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        ThumbImage(url = url, placeholderKey = url, size = FRAME_SIZE, contentDescription = "$label screen", modifier = Modifier.fillMaxSize())
        if (isLock) {
            Text(
                "09:41",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp)
            )
        } else {
            // A few dock "icons".
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.75f))) }
            }
        }
        Text(
            label.uppercase(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DuoActions(busyTarget: WallpaperTarget?, enabled: Boolean, onApply: (WallpaperTarget) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(GradientSignature))
                .clickable(enabled = enabled) { onApply(WallpaperTarget.BOTH) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (busyTarget == WallpaperTarget.BOTH) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Set Duo  ·  Lock + Home", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            SecondaryAction("Lock only", busyTarget == WallpaperTarget.LOCK, enabled, Modifier.weight(1f)) { onApply(WallpaperTarget.LOCK) }
            SecondaryAction("Home only", busyTarget == WallpaperTarget.HOME, enabled, Modifier.weight(1f)) { onApply(WallpaperTarget.HOME) }
        }
    }
}

@Composable
private fun SecondaryAction(label: String, busy: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Elevated2, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Accent1, strokeWidth = 2.dp)
        } else {
            Text(label, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

/** One Duo image at full size. Tap anywhere (or Back) to close. */
@Composable
private fun FullScreenImage(url: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    ) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(
            "Tap to close",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
