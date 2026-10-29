package com.example.naughty.ui.editor

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.mention.MentionItem
import com.example.naughty.data.mention.MentionRepository
import com.example.naughty.data.mention.MentionType
import com.example.naughty.ui.components.hasCalendarPermission
import com.example.naughty.ui.components.hasContactsPermission
import com.example.naughty.ui.oneui.components.OneUiIconBadge
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun MentionSuggestionSheet(
    query: String,
    onSelect: (MentionItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf<MentionType?>(null) }
    var results by remember { mutableStateOf<List<MentionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var hasContacts by remember { mutableStateOf(hasContactsPermission(context)) }
    var hasCalendar by remember { mutableStateOf(hasCalendarPermission(context)) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContacts = granted
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendar = granted
    }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(query, selectedFilter, hasContacts, hasCalendar) {
        isLoading = true
        results = MentionRepository.searchMentions(context, query, selectedFilter)
        isLoading = false
    }

    val sheetShape = OneUi.shapes.sheet

    Surface(
        shape = sheetShape,
        color = OneUi.colors.surfaceElevated,
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, sheetShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OneUi.dimens.horizontalMargin, vertical = 12.dp)
        ) {
            // Header: @Query indicator + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OneUiIconBadge(
                        icon = Icons.Outlined.AlternateEmail,
                        iconTint = OneUi.colors.accent,
                        backgroundColor = OneUi.colors.accent.copy(alpha = 0.16f),
                        size = 28.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (query.isBlank()) "Reference Item" else "Referencing \"$query\"",
                        style = OneUi.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OneUi.colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = OneUi.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills: All, Contacts, Apps, Calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "All",
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterPill(
                    label = "Contacts",
                    icon = Icons.Outlined.Person,
                    selected = selectedFilter == MentionType.CONTACT,
                    onClick = { selectedFilter = MentionType.CONTACT },
                    activeColor = OneUi.badgePalette.green
                )
                FilterPill(
                    label = "Apps",
                    icon = Icons.Outlined.Apps,
                    selected = selectedFilter == MentionType.APP,
                    onClick = { selectedFilter = MentionType.APP },
                    activeColor = OneUi.colors.accent
                )
                FilterPill(
                    label = "Calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    selected = selectedFilter == MentionType.CALENDAR,
                    onClick = { selectedFilter = MentionType.CALENDAR },
                    activeColor = OneUi.badgePalette.orange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Permission Request Banners if permission is missing
            val showContactsPrompt = (!hasContacts) && (selectedFilter == null || selectedFilter == MentionType.CONTACT)
            val showCalendarPrompt = (!hasCalendar) && (selectedFilter == null || selectedFilter == MentionType.CALENDAR)

            if (showContactsPrompt) {
                PermissionPromptRow(
                    text = "Grant Contacts access to search & reference contacts",
                    buttonText = "Grant",
                    tint = OneUi.badgePalette.green,
                    onGrant = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showCalendarPrompt) {
                PermissionPromptRow(
                    text = "Grant Calendar access to search & reference events",
                    buttonText = "Grant",
                    tint = OneUi.badgePalette.orange,
                    onGrant = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Results List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = OneUi.colors.accent
                    )
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "No items found" else "No matches for \"$query\"",
                        style = OneUi.typography.subhead,
                        color = OneUi.colors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    itemsIndexed(results, key = { index, item -> "${item.type}_${item.id}_$index" }) { _, item ->
                        MentionItemRow(
                            item = item,
                            onClick = { onSelect(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = OneUi.colors.accent
) {
    val bgColor = if (selected) {
        activeColor.copy(alpha = 0.18f)
    } else {
        OneUi.colors.surfaceVariant
    }
    val contentColor = if (selected) activeColor else OneUi.colors.textSecondary

    Surface(
        shape = OneUi.shapes.pill,
        color = bgColor,
        modifier = Modifier
            .clip(OneUi.shapes.pill)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = OneUi.typography.badge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PermissionPromptRow(
    text: String,
    buttonText: String,
    tint: Color,
    onGrant: () -> Unit
) {
    Surface(
        shape = OneUi.shapes.inner,
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = OneUi.typography.subhead,
                    fontSize = 12.sp,
                    color = OneUi.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = OneUi.shapes.pill,
                color = tint.copy(alpha = 0.22f),
                modifier = Modifier
                    .clip(OneUi.shapes.pill)
                    .clickable(onClick = onGrant)
            ) {
                Text(
                    text = buttonText,
                    style = OneUi.typography.badge,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MentionItemRow(
    item: MentionItem,
    onClick: () -> Unit
) {
    val (typeColor, typeIcon, typeLabel) = when (item.type) {
        MentionType.CONTACT -> Triple(OneUi.badgePalette.green, Icons.Outlined.Person, "Contact")
        MentionType.APP -> Triple(OneUi.colors.accent, Icons.Outlined.Apps, "App")
        MentionType.CALENDAR -> Triple(OneUi.badgePalette.orange, Icons.Outlined.CalendarMonth, "Calendar")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OneUi.shapes.inner)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneUiIconBadge(
            icon = typeIcon,
            iconTint = typeColor,
            backgroundColor = typeColor.copy(alpha = 0.16f),
            size = 34.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = OneUi.typography.body,
                fontWeight = FontWeight.SemiBold,
                color = OneUi.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    style = OneUi.typography.bodySmall,
                    color = OneUi.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = OneUi.shapes.pill,
            color = typeColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = typeLabel,
                style = OneUi.typography.badge,
                fontWeight = FontWeight.SemiBold,
                color = typeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
