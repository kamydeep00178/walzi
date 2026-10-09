package com.yunok.walzi.presentation.lists

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.ads.BannerAdComposable
import com.yunok.walzi.presentation.components.AnimatedWallpaperCard
import com.yunok.walzi.presentation.components.CardStyle
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary
import androidx.compose.foundation.shape.RoundedCornerShape

private fun aspectRatioFor(id: String) =
    com.yunok.walzi.presentation.components.aspectRatioFor(id, com.yunok.walzi.presentation.components.GalleryAspectRatios)

@Composable
fun ListDetailScreen(
    listId: String,
    onBack: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Cards already animated in, so each animates only once.
    val dealtIds = remember { mutableSetOf<String>() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ListDetailEffect.NavigateBack -> onBack()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = state.listName.ifEmpty { "List" },
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = TextPrimary,
                modifier = Modifier.padding(start = 6.dp).weight(1f)
            )

            if (state.isActiveAutoRotateList) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Accent1.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PlaylistPlay, contentDescription = null, tint = Accent1, modifier = Modifier.padding(end = 4.dp))
                    Text("Auto-rotating", color = Accent1, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!state.isActiveAutoRotateList) {
                        DropdownMenuItem(
                            text = { Text("Use for Auto-Rotate") },
                            onClick = {
                                showMenu = false
                                viewModel.sendIntent(ListDetailIntent.SetAsActiveAutoRotateList)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            viewModel.sendIntent(ListDetailIntent.OpenRenameDialog)
                        }
                    )
                    if (!state.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Delete list") },
                            onClick = {
                                showMenu = false
                                viewModel.sendIntent(ListDetailIntent.OpenDeleteConfirm)
                            }
                        )
                    }
                }
            }
        }

        // Content takes the remaining height; the banner sits below it, never over it.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent3)
                }
                state.wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("This list is empty", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Open a wallpaper and tap \"Add to List\" to save it here.",
                            color = TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                            style = CardStyle.LIST,
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

    if (state.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.sendIntent(ListDetailIntent.DismissRenameDialog) },
            containerColor = Elevated,
            title = { Text("Rename list", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = state.renameDraft,
                    onValueChange = { viewModel.sendIntent(ListDetailIntent.UpdateRenameDraft(it)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.sendIntent(ListDetailIntent.ConfirmRename) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.sendIntent(ListDetailIntent.DismissRenameDialog) }) { Text("Cancel") }
            }
        )
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.sendIntent(ListDetailIntent.DismissDeleteConfirm) },
            containerColor = Elevated,
            title = { Text("Delete \"${state.listName}\"?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This only removes the list, not the wallpapers themselves.", color = TextTertiary) },
            confirmButton = {
                TextButton(onClick = { viewModel.sendIntent(ListDetailIntent.ConfirmDelete) }) {
                    Text("Delete", color = Color(0xFFFF5C5C))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.sendIntent(ListDetailIntent.DismissDeleteConfirm) }) { Text("Cancel") }
            }
        )
    }
}