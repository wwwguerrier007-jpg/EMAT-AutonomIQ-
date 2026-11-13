package com.example.emat.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

/**
 * Sleek Interface Extended Semantic Color System
 * Encapsulates the high-contrast palette of charcoal, deep slate, and professional electric blue.
 */
@Immutable
data class SleekColors(
    // Charcoal Foundation
    val charcoalBg: Color = SleekCharcoalBg,
    val charcoalSurface: Color = SleekCharcoalSurface,
    val charcoalCard: Color = SleekCharcoalCard,
    val charcoalElevated: Color = SleekCharcoalElevated,
    val charcoalActive: Color = SleekCharcoalActive,

    // Deep Slate Accents & Borders
    val deepSlateDark: Color = SleekDeepSlateDark,
    val deepSlateBorderHairline: Color = SleekDeepSlateBorderHairline,
    val deepSlateBorderSubtle: Color = SleekDeepSlateBorderSubtle,
    val deepSlateBorderMedium: Color = SleekDeepSlateBorderMedium,
    val deepSlateMuted: Color = SleekDeepSlateMuted,
    val deepSlateText: Color = SleekDeepSlateText,
    val deepSlateMutedText: Color = SleekDeepSlateMutedText,

    // Professional Electric Blue (Status Indicators & Highlights)
    val electricBlue: Color = ElectricBlue,
    val electricBlueBright: Color = ElectricBlueBright,
    val electricBlueVibrant: Color = ElectricBlueVibrant,
    val electricBlueDim: Color = ElectricBlueDim,
    val electricBlueBorder: Color = ElectricBlueBorder,
    val electricBlueGlow: Color = ElectricBlueGlow,

    // High-Contrast Typography
    val textWhite: Color = SleekTextWhite,
    val textPrimary: Color = SleekTextPrimary,
    val textSubtle: Color = SleekTextSubtle,
    val textMuted: Color = SleekTextMuted,
    val textOffset: Color = SleekTextOffset,

    // Verification & Trading Status Semantics
    val statusGreen: Color = StatusGreenEmerald,
    val statusGreenDim: Color = StatusGreenDim,
    val statusGreenBorder: Color = StatusGreenBorder,
    val statusRed: Color = StatusRedRose,
    val statusRedDim: Color = StatusRedDim,
    val statusRedBorder: Color = StatusRedBorder,
    val statusAmber: Color = StatusAmberWarning,
    val statusAmberDim: Color = StatusAmberDim,
    val statusAmberBorder: Color = StatusAmberBorder
)

val LocalSleekColors = staticCompositionLocalOf { SleekColors() }

/**
 * Access object for Sleek Interface theme properties anywhere in Compose.
 */
object SleekTheme {
    val colors: SleekColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSleekColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

/**
 * Standard Material 3 darkColorScheme mapped to the high-contrast Sleek Interface palette:
 * - Charcoal (#121212) as background
 * - Deep Slate (#1E1E1E) for surface & card backgrounds
 * - Professional Electric Blue (#2196F3) for primary indicators and accents
 */
private val SleekDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,                         // #2196F3 Professional Electric Blue
    onPrimary = SleekTextWhite,                     // #FFFFFF
    primaryContainer = ElectricBlueDim,             // 12% #2196F3
    onPrimaryContainer = ElectricBlueVibrant,       // #64B5F6
    secondary = ElectricBlue,                       // #2196F3 Accent
    onSecondary = SleekCharcoalBg,                  // #121212
    secondaryContainer = SleekCharcoalElevated,     // #252525
    onSecondaryContainer = SleekTextPrimary,        // #F1F5F9
    tertiary = SleekDeepSlateText,                  // #9E9E9E
    onTertiary = SleekCharcoalBg,                   // #121212
    tertiaryContainer = SleekDeepSlateDark,         // #181818
    onTertiaryContainer = SleekTextSubtle,          // #CBD5E1
    background = SleekCharcoalBg,                   // #121212 Charcoal Background
    onBackground = SleekTextPrimary,                // #F1F5F9
    surface = SleekCharcoalSurface,                 // #1E1E1E Deep Slate Surface
    onSurface = SleekTextPrimary,                   // #F1F5F9
    surfaceVariant = SleekCharcoalCard,             // #1E1E1E Deep Slate Card Background
    onSurfaceVariant = SleekTextMuted,              // #9E9E9E
    surfaceTint = ElectricBlue,                     // #2196F3
    outline = SleekDeepSlateBorderHairline,         // #2A2A2A
    outlineVariant = SleekDeepSlateBorderSubtle,    // #333333
    error = StatusRedRose,                          // #F43F5E
    onError = SleekTextWhite,                       // #FFFFFF
    errorContainer = StatusRedDim,
    onErrorContainer = StatusRedRose,
    scrim = Color.Black
)

private val SleekLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = ElectricBlue,
    secondary = Color(0xFF0D47A1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF546E7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECEFF1),
    onTertiaryContainer = Color(0xFF263238),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = ElectricBlue,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = StatusRedRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = StatusRedRose,
    scrim = Color.Black
)

/**
 * Three distinct theme modes for EMAT AutonomIQ
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemInDarkTheme
    }

    companion object {
        fun fromString(value: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: SYSTEM
        }
    }
}

/**
 * Sleek Interface Theme: Supports Dark (default institutional), Light, and System Default themes.
 */
@Composable
fun SleekInterfaceTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val sleekColors = SleekColors()
    val view = LocalView.current
    val colorScheme = if (darkTheme) SleekDarkColorScheme else SleekLightColorScheme

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSleekColors provides sleekColors,
        LocalTextStyle provides TextStyle(fontFamily = EmatFontFamily)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Custom EmatTheme wrapper supporting Dark, Light, and System default modes.
 */
@Composable
fun EmatTheme(
    darkTheme: Boolean = true, // Default high-contrast dark institutional theme
    content: @Composable () -> Unit
) {
    SleekInterfaceTheme(darkTheme = darkTheme, content = content)
}

/**
 * EmatAutonomIQTheme alias mapping to the centralized EmatTheme.
 */
@Composable
fun EmatAutonomIQTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    EmatTheme(darkTheme = darkTheme, content = content)
}

