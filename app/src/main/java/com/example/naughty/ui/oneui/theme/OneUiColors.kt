package com.example.naughty.ui.oneui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class OneUiBadgePalette(
    val blue: Color,
    val purple: Color,
    val orange: Color,
    val green: Color,
    val teal: Color,
    val red: Color,
    val gray: Color
) {
    val rose: Color get() = red
    companion object {
        val Dark = OneUiBadgePalette(
            blue = Color(0xFF2C75FF),
            purple = Color(0xFF7E57C2),
            orange = Color(0xFFFF9500),
            green = Color(0xFF34C759),
            teal = Color(0xFF00C7BE),
            red = Color(0xFFFF453A),
            gray = Color(0xFF636366)
        )
        val Light = OneUiBadgePalette(
            blue = Color(0xFF1B6EF3),
            purple = Color(0xFF673AB7),
            orange = Color(0xFFF57C00),
            green = Color(0xFF2E7D32),
            teal = Color(0xFF009688),
            red = Color(0xFFD32F2F),
            gray = Color(0xFF8E8E93)
        )
    }
}

@Immutable
data class OneUiColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfacePressed: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    val warning: Color,
    val success: Color,
    val switchTrackOff: Color,
    val scrim: Color,
    val badge: OneUiBadgePalette
) {
    val error: Color get() = destructive
    val hairline: Color get() = divider
    val surfaceVariant: Color get() = surfaceElevated
    val border: Color get() = divider
    fun toMaterialColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = surfaceElevated,
                onPrimaryContainer = textPrimary,
                secondary = textSecondary,
                onSecondary = onAccent,
                background = background,
                onBackground = textPrimary,
                surface = surface,
                onSurface = textPrimary,
                surfaceVariant = surfaceElevated,
                onSurfaceVariant = textSecondary,
                outline = divider,
                error = destructive,
                onError = onAccent
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = surfaceElevated,
                onPrimaryContainer = textPrimary,
                secondary = textSecondary,
                onSecondary = onAccent,
                background = background,
                onBackground = textPrimary,
                surface = surface,
                onSurface = textPrimary,
                surfaceVariant = surfaceElevated,
                onSurfaceVariant = textSecondary,
                outline = divider,
                error = destructive,
                onError = onAccent
            )
        }
    }
}

val OneUiDark = OneUiColors(
    isDark = true,
    background = Color(0xFF000000),         // Pure AMOLED pitch black
    surface = Color(0xFF1C1C1E),            // Elevated dark squircle island
    surfaceElevated = Color(0xFF252528),    // Floating dock, sheets, dialogs
    surfacePressed = Color(0xFF2C2C2F),     // Pressed row / card state
    divider = Color(0xFF2C2C30),            // Inset hairline divider
    textPrimary = Color(0xFFFAFAFA),        // Clean crisp white
    textSecondary = Color(0xFF9E9EA3),      // Secondary gray
    textTertiary = Color(0xFF6B6B70),       // Muted metadata
    accent = Color(0xFF3E91FF),             // Samsung One UI 9 vivid blue
    onAccent = Color(0xFFFFFFFF),
    destructive = Color(0xFFFF453A),        // Samsung destructive red
    warning = Color(0xFFFF9F0A),
    success = Color(0xFF34C759),
    switchTrackOff = Color(0xFF3A3A3C),     // Switch off track
    scrim = Color(0x99000000),
    badge = OneUiBadgePalette.Dark
)

val OneUiLight = OneUiColors(
    isDark = false,
    background = Color(0xFFF2F2F7),         // Soft light gray background
    surface = Color(0xFFFFFFFF),            // Clean white card island
    surfaceElevated = Color(0xFFFFFFFF),
    surfacePressed = Color(0xFFE5E5EA),
    divider = Color(0xFFE5E5EA),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF6C6C70),
    textTertiary = Color(0xFF8E8E93),
    accent = Color(0xFF007AFF),             // Samsung light blue
    onAccent = Color(0xFFFFFFFF),
    destructive = Color(0xFFFF3B30),
    warning = Color(0xFFFF9500),
    success = Color(0xFF34C759),
    switchTrackOff = Color(0xFFE5E5EA),
    scrim = Color(0x66000000),
    badge = OneUiBadgePalette.Light
)
