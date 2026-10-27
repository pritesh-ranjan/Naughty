package com.example.naughty.ui.theme

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.getValue

val Context.themeDataStore by preferencesDataStore(name = "theme_settings")
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
val CTS_NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("cts_notification_enabled")
val LAST_SEEN_VERSION_CODE_KEY = longPreferencesKey("last_seen_version_code")

enum class ThemeMode { LIGHT, DARK, AUTO }

fun Context.themePreferenceFlow(): Flow<ThemeMode> =
    themeDataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[THEME_MODE_KEY] ?: ThemeMode.DARK.name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

fun Context.ctsNotificationPreferenceFlow(): Flow<Boolean> =
    themeDataStore.data.map { prefs ->
        prefs[CTS_NOTIFICATION_ENABLED_KEY] ?: true
    }

suspend fun Context.setCtsNotificationEnabled(enabled: Boolean) {
    themeDataStore.edit { prefs ->
        prefs[CTS_NOTIFICATION_ENABLED_KEY] = enabled
    }
}

fun Context.lastSeenVersionCodeFlow(): Flow<Long?> =
    themeDataStore.data.map { prefs ->
        prefs[LAST_SEEN_VERSION_CODE_KEY]
    }

suspend fun Context.setLastSeenVersionCode(versionCode: Long) {
    themeDataStore.edit { prefs ->
        prefs[LAST_SEEN_VERSION_CODE_KEY] = versionCode
    }
}

fun Context.getAppVersionCode(): Long {
    return try {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
    } catch (_: Exception) {
        1L
    }
}

fun Context.getAppVersionName(): String {
    return try {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        pInfo.versionName ?: "2.3.19"
    } catch (_: Exception) {
        "2.3.19"
    }
}

private val LightColorScheme = lightColorScheme(
    primary = AmoledBlack,
    onPrimary = CrystalWhite,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = Color(0xFF047857),
    secondary = LightTextSecondary,
    onSecondary = CrystalWhite,
    background = CrystalWhite,
    onBackground = WarmTextPrimaryLight,
    surface = CrystalWhite,
    onSurface = WarmTextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = WarmTextSecondaryLight,
    outline = LightOutline,
    error = LightError,
    onError = CrystalWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = AmoledBlack,
    primaryContainer = Color(0xFF0F2E20),
    onPrimaryContainer = ElectricGreen,
    secondary = StealthTextSecondary,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = StealthTextPrimary,
    surface = StealthCardSurface,
    onSurface = StealthTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = StealthTextSecondary,
    outline = StealthCardBorder,
    error = DarkError,
    onError = AmoledBlack
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
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background == AmoledBlack
