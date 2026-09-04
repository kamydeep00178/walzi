package com.yunok.walzi.presentation.lists

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
    viewModel: ListsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeList = state.lists.firstOrNull { it.id == state.autoRotateSettings.activeListId }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("My Lists", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.padding(start = 6.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                AutoRotateCard(
                    enabled = state.autoRotateSettings.enabled,
                    activeListName = activeList?.name ?: "Choose a list",
                    target = state.autoRotateSettings.target,
                    onToggle = { viewModel.sendIntent(ListsIntent.ToggleAutoRotate(it)) },
                    onPickList = { viewModel.sendIntent(ListsIntent.OpenActiveListPicker) },
                    onPickTarget = { viewModel.sendIntent(ListsIntent.SetAutoRotateTarget(it)) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 22.dp, bottom = 10.dp,end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "YOUR LISTS",
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    IconButton(onClick = { viewModel.sendIntent(ListsIntent.OpenCreateListDialog) }) {
                        Icon(Icons.Filled.Add, contentDescription = "New list", tint = Accent1)
                    }
                }
            }

            items(state.lists, key = { it.id }) { list ->
                ListRow(list = list, onClick = { onOpenList(list.id) })
            }
        }
    }

    if (state.showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.sendIntent(ListsIntent.DismissCreateListDialog) },
            containerColor = Elevated,
            title = { Text("New list", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = state.newListNameDraft,
                    onValueChange = { viewModel.sendIntent(ListsIntent.UpdateNewListNameDraft(it)) },
                    placeholder = { Text("e.g. Weekend Vibes") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.sendIntent(ListsIntent.ConfirmCreateList) }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.sendIntent(ListsIntent.DismissCreateListDialog) }) { Text("Cancel") }
            }
        )
    }

    if (state.showActiveListPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.sendIntent(ListsIntent.DismissActiveListPicker) },
            containerColor = Elevated
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).padding(bottom = 26.dp)) {
                Text("Auto-rotate from…", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 14.dp))
                state.lists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface)
                            .clickable { viewModel.sendIntent(ListsIntent.SetActiveList(list.id)) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(list.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${list.wallpaperIds.size} wallpapers", color = TextTertiary, fontSize = 11.5.sp)
                        }
                        if (list.id == state.autoRotateSettings.activeListId) {
                            Text("Active", color = Accent1, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoRotateCard(
    enabled: Boolean,
    activeListName: String,
    target: WallpaperTarget,
    onToggle: (Boolean) -> Unit,
    onPickList: () -> Unit,
    onPickTarget: (WallpaperTarget) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PlaylistPlay, contentDescription = null, tint = Accent1)
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text("Auto-rotate daily", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text("Changes your wallpaper once a day", color = TextTertiary, fontSize = 11.5.sp)
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Accent1, uncheckedTrackColor = Elevated2)
            )
        }

        if (enabled) {
            Divider(color = Elevated2, modifier = Modifier.padding(vertical = 14.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPickList),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("List", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(activeListName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
            }

            Text("Apply to", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetChip("Home", target == WallpaperTarget.HOME) { onPickTarget(WallpaperTarget.HOME) }
                TargetChip("Lock", target == WallpaperTarget.LOCK) { onPickTarget(WallpaperTarget.LOCK) }
                TargetChip("Both", target == WallpaperTarget.BOTH) { onPickTarget(WallpaperTarget.BOTH) }
            }
        }
    }
}

@Composable
private fun TargetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Accent1 else Elevated2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) androidx.compose.ui.graphics.Color.White else TextTertiary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
    }
}

@Composable
private fun ListRow(list: WallpaperList, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(list.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            Text(
                "${list.wallpaperIds.size} wallpaper${if (list.wallpaperIds.size == 1) "" else "s"}" + if (list.isDefault) " · Default" else "",
                color = TextTertiary,
                fontSize = 11.5.sp
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
    }
}