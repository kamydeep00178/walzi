package com.yunok.walzi.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WalziColorScheme = darkColorScheme(
    primary = Accent1,
    secondary = Accent2,
    tertiary = Accent3,
    background = BgApp,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor
)

@Composable
fun WalziTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WalziColorScheme,
        typography = WalziTypography,
        content = content
    )
}
