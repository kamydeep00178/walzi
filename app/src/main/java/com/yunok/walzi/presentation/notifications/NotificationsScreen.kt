package com.yunok.walzi.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.size.Size
import com.yunok.walzi.ads.AdViewModel
import com.yunok.walzi.ads.NativeAdCard
import com.yunok.walzi.data.local.entity.NotificationEntity
import com.yunok.walzi.presentation.components.ShimmerPlaceholder
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.components.placeholderColorFor
import com.yunok.walzi.presentation.navigation.DeepLinkTarget
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary
import java.util.concurrent.TimeUnit

/** The single native ad row sits after this many notifications. */
private const val NOTIFICATIONS_AD_AFTER = 3

/** 48dp thumbnail - decode small (covers up to ~3x density). */
private val NOTIFICATION_THUMB_SIZE = Size(160, 160)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigate: (DeepLinkTarget) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
    adViewModel: AdViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(adViewModel) { adViewModel.requestNativeAds(1) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationsEffect.Navigate -> onNavigate(effect.target)
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
            Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.padding(start = 6.dp))
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent3)
            }
            state.notifications.isEmpty() -> EmptyNotificationsState()
            else -> {
                val notifications = state.notifications
                val nativeAd = adViewModel.nativeAds.firstOrNull()
                // One native ad row, only where real notifications continue after it.
                val adAfter = if (nativeAd != null && notifications.size > NOTIFICATIONS_AD_AFTER) NOTIFICATIONS_AD_AFTER else notifications.size
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notifications.subList(0, adAfter), key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { viewModel.sendIntent(NotificationsIntent.NotificationTapped(notification)) }
                        )
                    }
                    if (adAfter < notifications.size) {
                        item(key = "native_ad", contentType = "native_ad") {
                            NativeAdCard(nativeAd = nativeAd, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                        items(notifications.subList(adAfter, notifications.size), key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = { viewModel.sendIntent(NotificationsIntent.NotificationTapped(notification)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = TextTertiary)
            Text(
                "No notifications yet",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "New wallpapers and updates will show up here.",
                color = TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).padding(top = 6.dp)) {
            if (!notification.isRead) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Accent3))
            }
        }
        Spacer(Modifier.padding(start = 6.dp))

        val contentAlpha = if (notification.isRead) 0.6f else 1f

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (notification.imageUrl != null) {
                // ThumbImage, not SubcomposeAsyncImage: no extra composition pass per row.
                ThumbImage(
                    url = notification.imageUrl,
                    placeholderKey = notification.id,
                    size = NOTIFICATION_THUMB_SIZE,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ShimmerPlaceholder(baseColor = placeholderColorFor(notification.id), modifier = Modifier.fillMaxSize())
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                notification.title,
                color = TextPrimary.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            if (notification.body.isNotBlank()) {
                Text(
                    notification.body,
                    color = TextTertiary.copy(alpha = contentAlpha),
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                relativeTime(notification.receivedAt),
                color = TextTertiary.copy(alpha = contentAlpha * 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun relativeTime(atMillis: Long): String {
    val diffMs = System.currentTimeMillis() - atMillis
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        hours < 1 -> "Just now"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}