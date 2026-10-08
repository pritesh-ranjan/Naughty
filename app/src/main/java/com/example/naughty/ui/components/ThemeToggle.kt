package com.example.naughty.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.theme.ThemeMode

@Composable
fun ThemeToggle(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "themeBg"
            )
            val iconTint by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "themeIconTint"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bgColor)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (mode) {
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                        ThemeMode.AUTO -> Icons.Outlined.BrightnessAuto
                    },
                    contentDescription = mode.name,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
