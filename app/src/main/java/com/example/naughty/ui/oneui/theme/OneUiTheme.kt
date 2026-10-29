package com.example.naughty.ui.oneui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalOneUiColors = staticCompositionLocalOf { OneUiDark }
val LocalOneUiTypography = staticCompositionLocalOf { OneUiType }

object OneUi {
    val colors: OneUiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOneUiColors.current

    val type: OneUiTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOneUiTypography.current

    val typography: OneUiTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOneUiTypography.current

    val badgePalette: OneUiBadgePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalOneUiColors.current.badge

    val shapes: OneUiShapes = OneUiShapes
    val dimens: OneUiDimens = OneUiDimens
}

@Composable
fun OneUiTheme(
    isDark: Boolean = com.example.naughty.ui.theme.isAppInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (isDark) OneUiDark else OneUiLight

    CompositionLocalProvider(
        LocalOneUiColors provides colors,
        LocalOneUiTypography provides OneUiType
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = OneUiType.toMaterial(),
            shapes = OneUiShapes.toMaterialShapes(),
            content = content
        )
    }
}
