package com.yunok.walzi.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uses the system default font family; swap in Space Grotesk / Inter via
// res/font if you want an exact match to the design mock.
val WalziTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = TextSecondary),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextTertiary)
)
