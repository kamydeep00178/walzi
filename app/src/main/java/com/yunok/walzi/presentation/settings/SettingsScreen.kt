package com.yunok.walzi.presentation.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunok.walzi.ads.ConsentManager
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.BgApp
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.Surface
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextTertiary

/**
 * TODO: replace both URLs once you've pushed privacy-policy.html and terms-and-conditions.html
 * to GitHub Pages - e.g. https://<your-github-username>.github.io/<repo-name>/privacy-policy.html
 */
private const val PRIVACY_POLICY_URL = "https://yunoktech-pixel.github.io/walzi-legal/privacy-policy.html"
private const val TERMS_URL = "https://yunoktech-pixel.github.io/walzi-legal/terms-and-conditions.html"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var autoRotate by remember { mutableStateOf(true) }
    var dailySuggest by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }
    var hqDownload by remember { mutableStateOf(true) }
    val ctx    = LocalContext.current

    var showPrivacyRow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showPrivacyRow = ConsentManager.isPrivacyOptionsRequired(ctx)
    }

    Column(modifier = Modifier.fillMaxSize().background(BgApp).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.padding(start = 6.dp))
        }

        SettingsSection(title = "Wallpaper") {
            SettingsToggleRow("Auto-rotate wallpaper", "Change automatically every day", autoRotate) { autoRotate = it }
            SettingsToggleRow("Daily suggestions", "Get a hand-picked pick each morning", dailySuggest) { dailySuggest = it }
        }

        SettingsSection(title = "Downloads") {
            SettingsToggleRow("Wi-Fi only downloads", "Avoid using mobile data", wifiOnly) { wifiOnly = it }
            SettingsToggleRow("High-quality downloads", "Larger file size, sharper detail", hqDownload) { hqDownload = it }
        }

        SettingsSection(title = "About") {
            if (showPrivacyRow) {
                SettingsLinkRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Ad Privacy /n Options Manage data sharing preferences",
                    onClick = {
                        (ctx as? Activity)?.let { activity ->
                            ConsentManager.showPrivacyOptionsForm(activity)
                        }
                    }
                )
            }
            SettingsLinkRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                onClick = { openUrl(context, PRIVACY_POLICY_URL) }
            )
            SettingsLinkRow(
                icon = Icons.Filled.Description,
                title = "Terms & Conditions",
                onClick = { openUrl(context, TERMS_URL) }
            )
            SettingsLinkRow(
                icon = Icons.Filled.Star,
                title = "Rate the app",
                onClick = { openPlayStoreListing(context) }
            )
            SettingsLinkRow(
                icon = Icons.Filled.Share,
                title = "Share app",
                onClick = { shareApp(context) },
                showDivider = false
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun openPlayStoreListing(context: android.content.Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
            }
        )
    } catch (e: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
    }
}

private fun shareApp(context: android.content.Context) {
    val packageName = context.packageName
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out Walzi - a wallpaper app I use! https://play.google.com/store/apps/details?id=$packageName"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Walzi"))
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        color = TextTertiary,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextTertiary, fontSize = 11.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = Accent1,
                uncheckedTrackColor = Elevated2
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.padding(end = 12.dp))
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
        if (showDivider) {
            androidx.compose.material3.Divider(color = Elevated2)
        }
    }
}