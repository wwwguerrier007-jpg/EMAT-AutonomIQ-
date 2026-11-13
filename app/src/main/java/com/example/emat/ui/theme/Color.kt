package com.example.emat.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// SLEEK INTERFACE: HIGH-CONTRAST PALETTE
// Charcoal, Deep Slate, and Professional Electric Blue for Status Indicators
// ============================================================================

// 1. Charcoal Foundations (Deep Institutional Black & Crisp Surfaces)
val SleekCharcoalBg = Color(0xFF080A0E)          // Deep Institutional Black (#080A0E)
val SleekCharcoalSurface = Color(0xFF131822)     // Elevated Slate Surface (#131822)
val SleekCharcoalCard = Color(0xFF151C28)        // Distinct Card Background (#151C28)
val SleekCharcoalElevated = Color(0xFF1C2433)    // Elevated Surface (#1C2433)
val SleekCharcoalActive = Color(0xFF1F293D)      // Active / Input Container (#1F293D)

// 2. Crisp Borders & Accents (High Contrast, Razor Sharp Definition)
val SleekDeepSlateDark = Color(0xFF0F141D)       // Deep Slate Dark
val SleekDeepSlateBorderHairline = Color(0xFF2D3748) // Clear Visible Micro-border (#2D3748)
val SleekDeepSlateBorderSubtle = Color(0xFF3B485D)   // Distinct Card Outline (#3B485D)
val SleekDeepSlateBorderMedium = Color(0xFF4B5E78)   // High-Contrast Border (#4B5E78)
val SleekDeepSlateMuted = Color(0xFF64748B)     // Slate Neutral
val SleekDeepSlateText = Color(0xFFE2E8F0)      // Bright Slate Text (#E2E8F0)
val SleekDeepSlateMutedText = Color(0xFF94A3B8) // Clear Muted Text (#94A3B8)

// 3. Professional Electric Blue (Status Indicators & Primary Highlights)
val ElectricBlue = Color(0xFF2196F3)             // Professional Electric Blue (#2196F3)
val ElectricBlueBright = Color(0xFF38BDF8)       // Vivid High-Contrast Cyan-Blue (#38BDF8)
val ElectricBlueVibrant = Color(0xFF60A5FA)      // High-Contrast Light Blue (#60A5FA)
val ElectricBlueDim = Color(0x2B2196F3)          // 17% Electric Blue Container
val ElectricBlueBorder = Color(0x802196F3)       // 50% High-Contrast Electric Blue Outline
val ElectricBlueGlow = Color(0xB32196F3)         // 70% Blue Status Glow

// 4. High-Contrast Typography (Crisp & Legible On Any Display)
val SleekTextWhite = Color(0xFFFFFFFF)           // Pure High-Contrast White
val SleekTextPrimary = Color(0xFFF8FAFC)         // Crisp Bright Off-White
val SleekTextSubtle = Color(0xFFE2E8F0)          // High-Readability Light Silver
val SleekTextMuted = Color(0xFFA0AEC0)           // Clear Legible Slate (Labels, Secondary)
val SleekTextOffset = Color(0xFF718096)          // Visible Secondary Slate

// 5. Semantic Trading & Verification Status Colors
val StatusGreenEmerald = Color(0xFF10B981)       // Bullish / Healthy / Approved
val StatusGreenDim = Color(0x1A10B981)
val StatusGreenBorder = Color(0x3310B981)

val StatusRedRose = Color(0xFFF43F5E)            // Bearish / Emergency / Alert
val StatusRedDim = Color(0x1AF43F5E)
val StatusRedBorder = Color(0x33F43F5E)

val StatusAmberWarning = Color(0xFFF59E0B)       // Monitoring / Pending / High Impact
val StatusAmberDim = Color(0x1AF59E0B)
val StatusAmberBorder = Color(0x33F59E0B)

// ============================================================================
// COMPATIBILITY ALIASES (Guarantees backward compatibility across all screens)
// ============================================================================
val EmatBackground = SleekCharcoalBg
val EmatSurface = SleekCharcoalSurface
val EmatSurfaceCard = SleekCharcoalCard
val EmatSurfaceElevated = SleekCharcoalElevated
val EmatSurfaceActive = SleekCharcoalActive

val EmatTextWhite = SleekTextWhite
val EmatTextPrimary = SleekTextPrimary
val EmatTextSubtle = SleekTextSubtle
val EmatTextMuted = SleekTextMuted
val EmatTextOffset = SleekTextOffset

val EmatBorderHairline = SleekDeepSlateBorderHairline
val EmatBorderSubtle = SleekDeepSlateBorderSubtle
val EmatBorderMedium = SleekDeepSlateBorderMedium
val EmatBorderSteel = SleekDeepSlateBorderHairline
val EmatBorderSteelLight = SleekDeepSlateBorderSubtle

// Electric Blue mapped to tech indicators
val EmatTechCyan = ElectricBlueBright
val EmatTechCyanDim = ElectricBlueDim
val EmatTechCyanBorder = ElectricBlueBorder
val EmatTechCyanGlow = ElectricBlueGlow

val EmatLongGreen = StatusGreenEmerald
val EmatLongGreenDim = StatusGreenDim
val EmatLongGreenBorder = StatusGreenBorder
val EmatBullishGreen = StatusGreenEmerald

val EmatShortRed = StatusRedRose
val EmatShortRedDim = StatusRedDim
val EmatShortRedBorder = StatusRedBorder
val EmatBearishRed = StatusRedRose

val EmatWarningAmber = StatusAmberWarning
val EmatWarningAmberDim = StatusAmberDim
val EmatWarningAmberBorder = StatusAmberBorder

val EmatPlatinumLight = SleekTextWhite
val EmatSilverPrimary = SleekTextPrimary
val EmatSilverSecondary = SleekTextSubtle
val EmatSteelMuted = SleekTextMuted
val EmatSteelDark = SleekTextOffset
val EmatDisconnectedSlate = SleekDeepSlateMutedText

// Sleek Linear Gradient Brushes
val SleekBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x333B82F6), // Electric blue hairline touch
        Color(0x1A2B364A), // Deep slate
        Color(0x261E2634)  // Hairline slate
    )
)

val MetallicBorderBrush = SleekBorderBrush

val TitaniumGleamBrush = Brush.linearGradient(
    colors = listOf(
        SleekTextWhite,
        SleekDeepSlateText,
        ElectricBlueBright
    )
)


