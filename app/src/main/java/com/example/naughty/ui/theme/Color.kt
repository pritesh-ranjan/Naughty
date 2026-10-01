package com.example.naughty.ui.theme

import androidx.compose.ui.graphics.Color

// Stealth Dark Aesthetic Palette (Reference design)
val StealthDarkBackground = Color(0xFF0E0F12)
val StealthCardSurface = Color(0xFF16171B)
val StealthCardBorder = Color(0xFF202227)
val StealthAppBadgePill = Color(0xFF22242A)
val StealthAppBadgeText = Color(0xFFC5C8D1)
val StealthDockBackground = Color(0xFF141519)
val StealthDockBorder = Color(0xFF22242B)
val StealthTextPrimary = Color(0xFFFFFFFF)
val StealthTextSecondary = Color(0xFF8E929E)
val StealthTextMuted = Color(0xFF656873)
val StealthIndicatorActive = Color(0xFFE2E4EB)
val StealthIndicatorInactive = Color(0xFF35373F)
val StealthFabWhite = Color(0xFFFFFFFF)
val StealthFabBlack = Color(0xFF000000)

// Main backgrounds
val WarmLinenLight = Color(0xFFF7F5EE)
val WarmCharcoalDark = StealthDarkBackground

// Pure surfaces for floating bars, sheets, dialogs
val PureWhiteSurface = Color(0xFFFFFFFF)
val DarkElevatedSurface = StealthCardSurface

// Typography colors
val WarmTextPrimaryLight = Color(0xFF1E1D1B)
val WarmTextSecondaryLight = Color(0xFF7E7A73)
val WarmTextMutedLight = Color(0xFFA5A097)

val WarmTextPrimaryDark = StealthTextPrimary
val WarmTextSecondaryDark = StealthTextSecondary
val WarmTextMutedDark = StealthTextMuted

// Subtitle tracking color
val SubtitleColorLight = Color(0xFF949088)
val SubtitleColorDark = StealthTextMuted

// Title highlighter brush
val TitleHighlighterPink = Color(0xFFFCD5CF)

// Standard theme colors
val LightBackground = WarmLinenLight
val LightSurface = PureWhiteSurface
val LightSurfaceVariant = Color(0xFFF0ECE3)
val LightTextPrimary = WarmTextPrimaryLight
val LightTextSecondary = WarmTextSecondaryLight
val LightAccent = Color(0xFF262523)
val LightAccentContainer = Color(0xFFEBE7DD)
val LightOutline = Color(0xFFE4DFD6)
val LightError = Color(0xFFD32F2F)
val LightArchiveAmber = Color(0xFFFFA000)

val DarkBackground = StealthDarkBackground
val DarkSurface = StealthCardSurface
val DarkSurfaceVariant = Color(0xFF1A1B20)
val DarkTextPrimary = StealthTextPrimary
val DarkTextSecondary = StealthTextSecondary
val DarkAccent = Color(0xFFEAE7E0)
val DarkAccentContainer = Color(0xFF222329)
val DarkOutline = StealthCardBorder
val DarkError = Color(0xFFEF5350)
val DarkArchiveAmber = Color(0xFFFFB300)

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
        // Sleek uniform stealth dark cards for home page list & search
        NoteColorPalette(
            surface = StealthCardSurface,
            border = if (noteTheme.isDark) noteTheme.border.copy(alpha = 0.7f) else StealthCardBorder,
            textPrimary = StealthTextPrimary,
            textSecondary = StealthTextSecondary,
            iconTint = noteTheme.accent
        )
    } else {
        // Clean surfaces for light theme
        NoteColorPalette(
            surface = noteTheme.surface,
            border = noteTheme.border,
            textPrimary = noteTheme.textPrimary,
            textSecondary = noteTheme.textSecondary,
            iconTint = noteTheme.accent
        )
    }
}
