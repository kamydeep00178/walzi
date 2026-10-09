package com.yunok.walzi.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.yunok.walzi.R
import com.yunok.walzi.ads.AdFreeManager
import com.yunok.walzi.ads.AdViewModel
import com.yunok.walzi.ads.AdsConfig
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BorderColor
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.findActivity

/** Each item carries its own icon color (not just an active-state tint) - gives the plain
 *  list some visual life, matching the reference's colorful-icon look, without needing a
 *  profile/login header or any "upgrade" styling above it. */
private data class DrawerItem(val route: String, val label: String, val icon: ImageVector, val iconColor: Color)

private val drawerItems = listOf(
    DrawerItem(Screen.Home.route, "Home", Icons.Filled.Home, Accent3),
    DrawerItem(Screen.Favorites.route, "Favorites", Icons.Filled.Favorite, Accent2),
    DrawerItem(Screen.Lists.route, "My Lists", Icons.Filled.PlaylistPlay, Accent1),
    DrawerItem(Screen.History.route, "History", Icons.Filled.History, Accent3),
    DrawerItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, TextSecondary)
)

@Composable
fun AppDrawerContent(
    navController: NavController,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = Elevated) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 28.dp)) {

            Box(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_wordmark),
                    contentDescription = "Walzi",
                    modifier = Modifier.height(34.dp)
                )
            }

           /* Text(
                text = "walzi",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )*/

            Divider(color = BorderColor, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

            drawerItems.forEach { item ->
                val active = currentRoute == item.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item.route) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 13.dp, height = 20.dp), contentAlignment = Alignment.CenterStart) {
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(item.iconColor)
                            )
                        }
                    }
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = item.iconColor,
                        modifier = Modifier.size(23.dp)
                    )
                    Text(
                        item.label,
                        color = if (active) TextPrimary else TextSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.5.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // "Remove ads for 24 hours" (opt-in rewarded ad) - only when ads are on.
            if (AdsConfig.ADS_ENABLED) {
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                RemoveAdsDrawerItem()
            }
        }
    }
}

private const val HOUR_MS = 60L * 60L * 1000L

/**
 * Drawer entry for the "Remove ads for 24 hours" reward. Opt-in: tapping opens a dialog that
 * says exactly what the user gets; the video plays only after "Watch video", and the reward is
 * granted only when the SDK reports it earned. While active it shows the time left instead.
 */
@Composable
private fun RemoveAdsDrawerItem(adViewModel: AdViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val adFree by AdFreeManager.isAdFree.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (adFree) Color.Transparent else Accent1.copy(alpha = 0.14f))
            .clickable(enabled = !adFree) { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (adFree) Icons.Filled.Block else Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = Accent1,
            modifier = Modifier.padding(start = 9.dp).size(23.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            if (adFree) {
                val hoursLeft = ((AdFreeManager.remainingMs() + HOUR_MS - 1) / HOUR_MS).coerceAtLeast(1)
                Text("Ads removed", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("About $hoursLeft h left", color = TextTertiary, fontSize = 11.5.sp)
            } else {
                Text("Remove ads for 24 hours", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Watch one short video", color = TextTertiary, fontSize = 11.5.sp)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Elevated,
            title = { Text("Remove ads for 24 hours", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Watch one short video ad to hide all other ads in Walzi for the next 24 hours.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val host = activity
                    if (host == null || !adViewModel.isRewardedReady) {
                        Toast.makeText(context, "No video available right now. Please try again in a minute.", Toast.LENGTH_SHORT).show()
                    } else {
                        adViewModel.showRewarded(host) { earned ->
                            if (earned) {
                                AdFreeManager.grant()
                                Toast.makeText(context, "Ads removed for 24 hours.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Watch the full video to remove ads.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) { Text("Watch video", color = Accent1, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}