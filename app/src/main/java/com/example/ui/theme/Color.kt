package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Charcoal Foundations
val SleekCharcoalBg = Color(0xFF121212)
val SleekCharcoalSurface = Color(0xFF1E1E1E)
val SleekCharcoalCard = Color(0xFF1E1E1E)
val SleekCharcoalElevated = Color(0xFF252525)
val SleekCharcoalActive = Color(0xFF2C2C2C)

// Deep Slate
val SleekDeepSlateDark = Color(0xFF181818)
val SleekDeepSlateBorderHairline = Color(0xFF2A2A2A)
val SleekDeepSlateBorderSubtle = Color(0xFF333333)
val SleekDeepSlateBorderMedium = Color(0xFF444444)
val SleekDeepSlateMuted = Color(0xFF555555)
val SleekDeepSlateText = Color(0xFF9E9E9E)
val SleekDeepSlateMutedText = Color(0xFF757575)

// Professional Electric Blue (Status Indicators)
val ElectricBlue = Color(0xFF2196F3)
val ElectricBlueBright = Color(0xFF2196F3)
val ElectricBlueVibrant = Color(0xFF64B5F6)
val ElectricBlueDim = Color(0x1F2196F3)
val ElectricBlueBorder = Color(0x4D2196F3)
val ElectricBlueGlow = Color(0x802196F3)

// High-Contrast Typography
val SleekTextWhite = Color(0xFFFFFFFF)
val SleekTextPrimary = Color(0xFFF1F5F9)
val SleekTextSubtle = Color(0xFFCBD5E1)
val SleekTextMuted = Color(0xFF94A3B8)
val SleekTextOffset = Color(0xFF64748B)

// Compatibility Aliases
val EmatBackground = SleekCharcoalBg
val EmatSurface = SleekCharcoalSurface
val EmatSurfaceElevated = SleekCharcoalElevated
val EmatSurfaceCard = SleekCharcoalCard

val EmatPlatinumLight = SleekTextWhite
val EmatSilverPrimary = SleekTextPrimary
val EmatSilverSecondary = SleekTextSubtle
val EmatSteelMuted = SleekTextMuted
val EmatSteelDark = SleekTextOffset
val EmatBorderSteel = SleekDeepSlateBorderHairline
val EmatBorderSteelLight = SleekDeepSlateBorderSubtle

val EmatLongGreen = Color(0xFF10B981)
val EmatLongGreenDim = Color(0x1A10B981)
val EmatShortRed = Color(0xFFF43F5E)
val EmatShortRedDim = Color(0x1AF43F5E)
val EmatWarningAmber = Color(0xFFF59E0B)
val EmatWarningAmberDim = Color(0x1AF59E0B)
val EmatTechCyan = ElectricBlueBright
val EmatTechCyanDim = ElectricBlueDim
val EmatDisconnectedSlate = SleekDeepSlateMutedText

val MetallicBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x333B82F6),
        Color(0x1A2B364A),
        Color(0x261E2634)
    )
)

val TitaniumGleamBrush = Brush.linearGradient(
    colors = listOf(
        SleekTextWhite,
        SleekDeepSlateText,
        ElectricBlueBright
    )
)


