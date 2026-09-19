package com.yunok.walzi.presentation.wallpaperdetail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.size.Size
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.WallpaperTarget
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
    viewModel: WallpaperDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WallpaperDetailEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.text) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        if (state.isLoading || state.wallpapers.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Accent1)
        } else {
            val context = LocalContext.current
            val pagerState = rememberPagerState(initialPage = state.initialIndex) { state.wallpapers.size }
            val currentWallpaper = state.wallpapers.getOrNull(
                pagerState.currentPage.coerceIn(0, state.wallpapers.lastIndex)
            )

            // Prefetch the next page a few swipes before the user actually hits the end.
            LaunchedEffect(pagerState.currentPage, state.wallpapers.size) {
                if (pagerState.currentPage >= state.wallpapers.size - 3) {
                    viewModel.sendIntent(WallpaperDetailIntent.LoadNextPage)
                }
            }

            val storagePermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    currentWallpaper?.let { viewModel.sendIntent(WallpaperDetailIntent.Download(it.id)) }
                }
            }

            fun requestDownload(wallpaperId: String) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    viewModel.sendIntent(WallpaperDetailIntent.Download(wallpaperId))
                }
            }

            // Only the image itself lives inside the pager, so only the image moves during a
            // vertical swipe. Buttons/text below are a separate fixed overlay in the same Box.
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val wallpaper = state.wallpapers[page]
                AsyncImage(
                    // Full-screen preview (and, by extension, what Set Wallpaper / Download act
                    // on) always requests Size.ORIGINAL - never the downsized grid thumbnail decode.
                    model = remember(wallpaper.imageUrl) {
                        ImageRequest.Builder(context)
                            .data(wallpaper.imageUrl)
                            .size(Size.ORIGINAL)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Fixed overlay: position stays put regardless of swipe progress. Only the data it
            // shows (title, favorite state, action targets) updates once a swipe settles on a
            // new page, via currentWallpaper.
            if (currentWallpaper != null) {
                WallpaperOverlayTopBar(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                WallpaperOverlayBottomBar(
                    wallpaper = currentWallpaper,
                    isApplyingWallpaper = state.isApplyingWallpaper && state.targetWallpaperId == currentWallpaper.id,
                    isDownloading = state.isDownloading,
                    onToggleFavorite = { viewModel.sendIntent(WallpaperDetailIntent.ToggleFavorite(currentWallpaper.id)) },
                    onOpenAddToList = { viewModel.sendIntent(WallpaperDetailIntent.OpenAddToListSheet(currentWallpaper.id)) },
                    onDownload = { requestDownload(currentWallpaper.id) },
                    onOpenSetSheet = { viewModel.sendIntent(WallpaperDetailIntent.OpenSetWallpaperSheet(currentWallpaper.id)) },
                    modifier = Modifier.align(Alignment.BottomCenter)
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
                    containerColor = Elevated
                ) {
                    SetWallpaperSheetContent(
                        wallpaperTitle = target?.title.orEmpty(),
                        onSelect = { t ->
                            target?.let { viewModel.sendIntent(WallpaperDetailIntent.ConfirmSetWallpaper(it.id, t)) }
                        }
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

@Composable
private fun WallpaperOverlayTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
            .padding(top = 20.dp, bottom = 40.dp, start = 12.dp, end = 12.dp)
    ) {
        CircleIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
    }
}

@Composable
private fun WallpaperOverlayBottomBar(
    wallpaper: Wallpaper,
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
        Text(wallpaper.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Text(
            "${wallpaper.categoryName} · ${wallpaper.resolution} · ${wallpaper.sizeLabel}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 3.dp, bottom = 16.dp)
        )
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
                    imageVector = if (wallpaper.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (wallpaper.isFavorite) Accent2 else Color.White
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
private fun SetWallpaperSheetContent(wallpaperTitle: String, onSelect: (WallpaperTarget) -> Unit) {
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