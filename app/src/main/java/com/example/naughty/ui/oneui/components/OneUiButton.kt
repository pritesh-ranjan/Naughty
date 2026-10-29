package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun OneUiFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = OneUi.colors.accent,
    contentColor: Color = OneUi.colors.onAccent
) {
    Surface(
        modifier = modifier
            .clip(OneUi.shapes.button)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = OneUi.shapes.button,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = OneUi.type.rowTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun OneUiTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = OneUi.colors.surfaceElevated,
    contentColor: Color = OneUi.colors.textPrimary
) {
    Surface(
        modifier = modifier
            .clip(OneUi.shapes.button)
            .clickable(onClick = onClick),
        shape = OneUi.shapes.button,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = OneUi.type.caption,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun OneUiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OneUi.colors.accent
) {
    Box(
        modifier = modifier
            .clip(OneUi.shapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OneUi.type.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = color
        )
    }
}

@Composable
fun OneUiFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 56.dp,
    containerColor: Color = OneUi.colors.accent,
    iconColor: Color = OneUi.colors.onAccent
) {
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
