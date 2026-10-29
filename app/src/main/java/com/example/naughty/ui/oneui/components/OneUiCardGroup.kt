package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.theme.OneUi

sealed interface OneUiLeading {
    data class IconBadge(
        val icon: ImageVector,
        val tint: Color,
        val iconColor: Color = Color.White
    ) : OneUiLeading

    data class RawIcon(
        val icon: ImageVector,
        val tint: Color? = null,
        val size: Dp = 22.dp
    ) : OneUiLeading

    data class Avatar(
        val initials: String,
        val background: Color = Color(0xFF6B7280)
    ) : OneUiLeading
}

sealed interface OneUiTrailing {
    data object None : OneUiTrailing
    data object Chevron : OneUiTrailing
    data object Checkmark : OneUiTrailing
    data class Switch(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val split: Boolean = false
    ) : OneUiTrailing
    data class CountBadge(
        val text: String,
        val isHighlight: Boolean = false
    ) : OneUiTrailing
    data class Text(
        val text: String,
        val color: Color? = null
    ) : OneUiTrailing
    data class Custom(
        val content: @Composable () -> Unit
    ) : OneUiTrailing
}

interface OneUiGroupScope {
    fun item(key: Any? = null, content: @Composable () -> Unit)
}

private class OneUiGroupScopeImpl : OneUiGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()
    override fun item(key: Any?, content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
fun OneUiCardGroup(
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color = OneUi.colors.surface,
    content: OneUiGroupScope.() -> Unit
) {
    val scope = OneUiGroupScopeImpl().apply(content)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label.uppercase(),
                style = OneUi.type.groupLabel,
                color = OneUi.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(
                    start = OneUi.dimens.screenGutter + 4.dp,
                    end = OneUi.dimens.screenGutter + 4.dp,
                    bottom = 8.dp,
                    top = 10.dp
                )
            )
        }

        Surface(
            shape = OneUi.shapes.card,
            color = containerColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OneUi.dimens.screenGutter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                scope.items.forEachIndexed { index, itemContent ->
                    itemContent()
                    if (index < scope.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = OneUi.dimens.dividerInsetWithIcon),
                            thickness = 0.5.dp,
                            color = OneUi.colors.divider
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OneUiListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = OneUi.colors.textSecondary,
    titleColor: Color = OneUi.colors.textPrimary,
    leading: OneUiLeading? = null,
    trailing: OneUiTrailing = OneUiTrailing.None,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = OneUi.dimens.rowMinHeight)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
        .padding(horizontal = 16.dp, vertical = 13.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (leading != null) {
                when (leading) {
                    is OneUiLeading.IconBadge -> {
                        OneUiIconBadge(
                            icon = leading.icon,
                            tint = leading.tint,
                            iconColor = leading.iconColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    is OneUiLeading.RawIcon -> {
                        Icon(
                            imageVector = leading.icon,
                            contentDescription = null,
                            tint = leading.tint ?: OneUi.colors.textSecondary,
                            modifier = Modifier.size(leading.size)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    is OneUiLeading.Avatar -> {
                        Box(
                            modifier = Modifier
                                .size(OneUi.dimens.iconBadgeSize)
                                .clip(CircleShape)
                                .background(leading.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = leading.initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OneUi.type.rowTitle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = OneUi.type.rowSubtitle,
                        color = subtitleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        when (trailing) {
            is OneUiTrailing.None -> {}
            is OneUiTrailing.Chevron -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = OneUi.colors.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
            is OneUiTrailing.Checkmark -> {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = OneUi.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            is OneUiTrailing.CountBadge -> {
                Surface(
                    shape = OneUi.shapes.pill,
                    color = if (trailing.isHighlight) OneUi.colors.accent.copy(alpha = 0.16f) else OneUi.colors.divider,
                    modifier = Modifier.clip(OneUi.shapes.pill)
                ) {
                    Text(
                        text = trailing.text,
                        style = OneUi.type.caption,
                        fontWeight = FontWeight.Bold,
                        color = if (trailing.isHighlight) OneUi.colors.accent else OneUi.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
            is OneUiTrailing.Text -> {
                Text(
                    text = trailing.text,
                    style = OneUi.type.rowSubtitle,
                    color = trailing.color ?: OneUi.colors.textSecondary
                )
            }
            is OneUiTrailing.Switch -> {
                if (trailing.split) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(OneUi.colors.divider)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OneUiSwitch(
                            checked = trailing.checked,
                            onCheckedChange = trailing.onCheckedChange
                        )
                    }
                } else {
                    OneUiSwitch(
                        checked = trailing.checked,
                        onCheckedChange = trailing.onCheckedChange
                    )
                }
            }
            is OneUiTrailing.Custom -> {
                trailing.content()
            }
        }
    }
}
