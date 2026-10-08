package com.example.naughty.ui.theme

import androidx.compose.ui.graphics.Color

// Amoled Black & Crystal White Base Tokens
val AmoledBlack = Color(0xFF000000)
val CrystalWhite = Color(0xFFFFFFFF)

// Popping Modern Bright Accent Palette (Inspired by antinote.io)
val ElectricGreen = Color(0xFF00F59B) // Antinote signature popping green
val ElectricCyan = Color(0xFF00E5FF)  // Vivid electric sky/cyan
val ElectricPink = Color(0xFFFF3366)  // Vibrant popping coral pink
val ElectricAmber = Color(0xFFFF9500) // Popping neon amber/orange
val ElectricPurple = Color(0xFFA855F7)// Vivid electric violet

// Dark Modern Aesthetic Palette
val StealthDarkBackground = AmoledBlack
val StealthCardSurface = Color(0xFF0A0A0C)
val StealthCardBorder = Color(0xFF191A20)
val StealthAppBadgePill = Color(0xFF14151B)
val StealthAppBadgeText = Color(0xFFFFFFFF)
val StealthDockBackground = AmoledBlack
val StealthDockBorder = Color(0xFF1A1A22)
val StealthTextPrimary = CrystalWhite
val StealthTextSecondary = Color(0xFFA1A1AA)
val StealthTextMuted = Color(0xFF71717A)
val StealthIndicatorActive = ElectricGreen
val StealthIndicatorInactive = Color(0xFF27272A)
val StealthFabWhite = ElectricGreen
val StealthFabBlack = AmoledBlack

// Main backgrounds
val WarmLinenLight = CrystalWhite
val WarmCharcoalDark = AmoledBlack

// Pure surfaces for floating bars, sheets, dialogs
val PureWhiteSurface = CrystalWhite
val DarkElevatedSurface = StealthCardSurface

// Typography colors
val WarmTextPrimaryLight = Color(0xFF09090B)
val WarmTextSecondaryLight = Color(0xFF52525B)
val WarmTextMutedLight = Color(0xFF71717A)

val WarmTextPrimaryDark = StealthTextPrimary
val WarmTextSecondaryDark = StealthTextSecondary
val WarmTextMutedDark = StealthTextMuted

// Subtitle tracking color
val SubtitleColorLight = Color(0xFF71717A)
val SubtitleColorDark = StealthTextMuted

// Title highlighter brush
val TitleHighlighterPink = Color(0xFFFFE4E6)

// Standard theme colors
val LightBackground = CrystalWhite
val LightSurface = CrystalWhite
val LightSurfaceVariant = Color(0xFFF4F4F5)
val LightTextPrimary = WarmTextPrimaryLight
val LightTextSecondary = WarmTextSecondaryLight
val LightAccent = AmoledBlack
val LightAccentContainer = Color(0xFFE6FAF0)
val LightOutline = Color(0xFFE4E4E7)
val LightError = ElectricPink
val LightArchiveAmber = ElectricAmber

val DarkBackground = AmoledBlack
val DarkSurface = StealthCardSurface
val DarkSurfaceVariant = Color(0xFF121216)
val DarkTextPrimary = StealthTextPrimary
val DarkTextSecondary = StealthTextSecondary
val DarkAccent = CrystalWhite
val DarkAccentContainer = Color(0xFF10281E)
val DarkOutline = StealthCardBorder
val DarkError = ElectricPink
val DarkArchiveAmber = ElectricAmber

// Data class for note card color palette (bridged with NoteThemeRegistry)
data class NoteColorPalette(
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val iconTint: Color
)

fun getNoteColorPalette(theme: String?, isDark: Boolean): NoteColorPalette {
    val noteTheme = NoteThemeRegistry.getTheme(theme, isDark)
    return if (isDark) {
        // Sleek uniform amoled dark cards with hairline border and popping accents
        NoteColorPalette(
            surface = StealthCardSurface,
            border = if (noteTheme.isDark) noteTheme.border.copy(alpha = 0.5f) else StealthCardBorder,
            textPrimary = StealthTextPrimary,
            textSecondary = StealthTextSecondary,
            iconTint = noteTheme.accent
        )
    } else {
        // Clean crisp crystal white surfaces for light theme
        NoteColorPalette(
            surface = noteTheme.surface,
            border = noteTheme.border.copy(alpha = 0.45f),
            textPrimary = noteTheme.textPrimary,
            textSecondary = noteTheme.textSecondary,
            iconTint = noteTheme.accent
        )
    }
}
