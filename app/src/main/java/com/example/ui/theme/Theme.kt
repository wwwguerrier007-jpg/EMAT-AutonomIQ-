package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleekDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = SleekTextWhite,
    primaryContainer = ElectricBlueDim,
    onPrimaryContainer = ElectricBlueVibrant,
    secondary = ElectricBlueBright,
    onSecondary = SleekCharcoalBg,
    secondaryContainer = SleekCharcoalElevated,
    onSecondaryContainer = SleekTextPrimary,
    tertiary = SleekDeepSlateText,
    onTertiary = SleekCharcoalBg,
    background = SleekCharcoalBg,
    onBackground = SleekTextPrimary,
    surface = SleekCharcoalSurface,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekCharcoalCard,
    onSurfaceVariant = SleekTextMuted,
    outline = SleekDeepSlateBorderHairline,
    outlineVariant = SleekDeepSlateBorderSubtle,
    error = EmatShortRed,
    onError = SleekTextWhite
)

@Composable
fun EmatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EmatTheme(content = content)
}


