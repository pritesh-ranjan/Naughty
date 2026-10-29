package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun OneUiIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = OneUi.colors.accent,
    backgroundColor: Color = tint,
    iconColor: Color = Color.White,
    iconTint: Color = iconColor,
    badgeSize: Dp = OneUi.dimens.iconBadgeSize,
    size: Dp = badgeSize,
    iconSize: Dp = OneUi.dimens.iconSize,
    contentDescription: String? = null
) {
    val finalBg = if (backgroundColor != tint) backgroundColor else tint
    val finalIconTint = if (iconTint != iconColor) iconTint else iconColor
    val finalSize = if (size != badgeSize) size else badgeSize
    Box(
        modifier = modifier
            .size(finalSize)
            .clip(CircleShape)
            .background(finalBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = finalIconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
