package com.yunok.walzi.presentation.history

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.size.Size
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.AppliedWallpaper
import com.yunok.walzi.util.WallpaperApplier
import com.yunok.walzi.util.WallpaperHistoryStore
import com.yunok.walzi.util.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val HISTORY_THUMB_SIZE = Size(200, 360)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: WallpaperHistoryStore,
    private val applier: WallpaperApplier
) : ViewModel() {

    val entries: StateFlow<List<AppliedWallpaper>> = history.entries

    /** Entry (by appliedAt) currently being re-applied, to show a spinner on its row. */
    private val _applyingAt = MutableStateFlow<Long?>(null)
    val applyingAt: StateFlow<Long?> = _applyingAt.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun applyAgain(entry: AppliedWallpaper) {
        if (_applyingAt.value != null) return
        viewModelScope.launch {
            _applyingAt.value = entry.appliedAt
            val result = applier.reapply(entry)
            _applyingAt.value = null
            _messages.send(if (result.isSuccess) "Wallpaper applied again" else "Couldn't apply it. Try again.")
        }
    }

    fun clear() = history.clear()
}

/** Recently applied wallpapers (manual, daily auto-rotate and the Shuffle widget), newest first. */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val applyingAt by viewModel.applyingAt.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
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
                Text(
                    "History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 6.dp).weight(1f)
                )
                if (entries.isNotEmpty()) {
                    TextButton(onClick = viewModel::clear) { Text("Clear", color = TextSecondary) }
                }
            }

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = TextTertiary)
                        Text("No wallpapers applied yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        Text(
                            "Wallpapers you set with Walzi show up here, so you can apply them again.",
                            color = TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.appliedAt }) { entry ->
                        HistoryRow(
                            entry = entry,
                            isApplying = applyingAt == entry.appliedAt,
                            onApplyAgain = { viewModel.applyAgain(entry) }
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
private fun HistoryRow(entry: AppliedWallpaper, isApplying: Boolean, onApplyAgain: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThumbImage(
            url = entry.thumbUrl.ifEmpty { entry.imageUrl },
            placeholderKey = entry.wallpaperId,
            size = HISTORY_THUMB_SIZE,
            contentDescription = entry.title,
            modifier = Modifier.size(width = 54.dp, height = 90.dp).clip(RoundedCornerShape(10.dp))
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(entry.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (entry.isDuo) "Duo (Lock + Home)" else entry.target.label()} · ${DateUtils.getRelativeTimeSpanString(entry.appliedAt)}",
                color = TextTertiary,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!entry.adjustments.isDefault) {
                Text("Adjusted", color = Accent1, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (isApplying) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(end = 6.dp), color = Accent1, strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onApplyAgain) { Text("Apply again", color = Accent1, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun WallpaperTarget.label(): String = when (this) {
    WallpaperTarget.HOME -> "Home screen"
    WallpaperTarget.LOCK -> "Lock screen"
    WallpaperTarget.BOTH -> "Home & Lock"
}
