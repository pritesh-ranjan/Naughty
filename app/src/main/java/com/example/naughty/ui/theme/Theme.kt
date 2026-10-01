package com.example.naughty.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore by preferencesDataStore(name = "theme_settings")
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

enum class ThemeMode { LIGHT, DARK, AUTO }

fun Context.themePreferenceFlow(): Flow<ThemeMode> =
    themeDataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[THEME_MODE_KEY] ?: ThemeMode.DARK.name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = PureWhiteSurface,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightAccent,
    secondary = LightTextSecondary,
    onSecondary = PureWhiteSurface,
    background = WarmLinenLight,
    onBackground = WarmTextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = WarmTextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = WarmTextSecondaryLight,
    outline = LightOutline,
    error = LightError,
    onError = PureWhiteSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = StealthFabWhite,
    onPrimary = StealthFabBlack,
    primaryContainer = StealthAppBadgePill,
    onPrimaryContainer = StealthAppBadgeText,
    secondary = StealthTextSecondary,
    onSecondary = StealthDarkBackground,
    background = StealthDarkBackground,
    onBackground = StealthTextPrimary,
    surface = StealthCardSurface,
    onSurface = StealthTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = StealthTextSecondary,
    outline = StealthCardBorder,
    error = DarkError,
    onError = StealthFabBlack
)

@Composable
fun NaughtyTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeMode by context.themePreferenceFlow().collectAsState(initial = ThemeMode.DARK)
    val systemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> systemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NaughtyTypography,
        content = content
    )
}

@Composable
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background == StealthDarkBackground
