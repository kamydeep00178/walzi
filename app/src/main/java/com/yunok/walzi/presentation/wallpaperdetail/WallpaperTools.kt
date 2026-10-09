package com.yunok.walzi.presentation.wallpaperdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Elevated2
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary
import com.yunok.walzi.util.WallpaperAdjustments
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Which screen the "Preview on my phone" mockup imitates. */
enum class MockupMode { HOME, LOCK }

/** Muted, varied colors so the fake app icons read as "icons" without branding. */
private val MOCK_ICON_COLORS = listOf(
    Color(0xFF4CD9E8), Color(0xFFFF5C9E), Color(0xFF7C5CFF), Color(0xFFFFB547),
    Color(0xFF34C759), Color(0xFF0A84FF), Color(0xFFFF453A), Color(0xFF8E8E93)
)

/**
 * "Preview on my phone": a fake home screen (clock, app grid, dock) or lock screen (big clock,
 * date, shortcuts) drawn over the wallpaper, so the user can judge readability before setting.
 * Tapping anywhere outside the Home/Lock switch closes it.
 */
@Composable
fun PhoneMockupOverlay(
    mode: MockupMode,
    onModeChange: (MockupMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { LocalDateTime.now() }
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val date = remember(now) { now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    ) {
        when (mode) {
            MockupMode.HOME -> HomeMockup(time = time)
            MockupMode.LOCK -> LockMockup(time = time, date = date)
        }

        // Home / Lock switch + hint. Consumes its own taps so they don't close the preview.
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(4.dp)
            ) {
                MockupTab("Home screen", mode == MockupMode.HOME) { onModeChange(MockupMode.HOME) }
                MockupTab("Lock screen", mode == MockupMode.LOCK) { onModeChange(MockupMode.LOCK) }
            }
            Text(
                "Tap anywhere to close",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun MockupTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Accent1 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

@Composable
private fun HomeMockup(time: String) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        // Status-bar style clock.
        Text(time, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(130.dp))
        // Big home-screen clock widget.
        Text(time, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.weight(1f))
        // 4 x 4 app grid with labels.
        repeat(4) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(4) { col -> MockAppIcon(MOCK_ICON_COLORS[(row * 4 + col) % MOCK_ICON_COLORS.size], label = "App") }
            }
        }
        // Search pill.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5C5C68), modifier = Modifier.size(18.dp))
        }
        // Dock.
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(4) { MockAppIcon(MOCK_ICON_COLORS[(it + 3) % MOCK_ICON_COLORS.size], label = null) }
        }
    }
}

@Composable
private fun MockAppIcon(color: Color, label: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(color))
        if (label != null) {
            Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun LockMockup(time: String, date: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(date, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
            Text(time, color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Light)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 36.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LockShortcut { Icon(Icons.Filled.FlashlightOn, contentDescription = null, tint = Color.White) }
            LockShortcut { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White) }
        }
    }
}

@Composable
private fun LockShortcut(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) { icon() }
}

/**
 * Sliders for [WallpaperAdjustments]. Changes apply live to the preview behind the sheet, and
 * the same values are used when the wallpaper is set.
 */
@Composable
fun AdjustSheetContent(
    adjustments: WallpaperAdjustments,
    onChange: (WallpaperAdjustments) -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Adjust", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { onChange(WallpaperAdjustments.NONE) }, enabled = !adjustments.isDefault) {
                Text("Reset", color = if (adjustments.isDefault) TextTertiary else TextSecondary)
            }
            TextButton(onClick = onDone) { Text("Done", color = Accent1, fontWeight = FontWeight.Bold) }
        }
        AdjustSlider("Dim", "Keeps icons readable", adjustments.dim, 0f..WallpaperAdjustments.MAX_DIM) {
            onChange(adjustments.copy(dim = it))
        }
        AdjustSlider("Blur", "Softer, calmer background", adjustments.blur, 0f..1f) {
            onChange(adjustments.copy(blur = it))
        }
        AdjustSlider(
            "Brightness", "Darker or brighter", adjustments.brightness,
            WallpaperAdjustments.MIN_BRIGHTNESS..WallpaperAdjustments.MAX_BRIGHTNESS
        ) { onChange(adjustments.copy(brightness = it)) }
        AdjustSlider("Position", "Which part of the image is shown", adjustments.position, -1f..1f) {
            onChange(adjustments.copy(position = it))
        }
        Text(
            "Applied when you tap Set Wallpaper. Download always saves the original.",
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun AdjustSlider(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(subtitle, color = TextTertiary, fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Accent1,
                activeTrackColor = Accent1,
                inactiveTrackColor = Elevated2
            )
        )
    }
}
