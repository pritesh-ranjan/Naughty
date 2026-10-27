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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
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
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.isAppInDarkTheme

@Composable
fun MentionSuggestionSheet(
    query: String,
    onSelect: (MentionItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

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

    val surfaceColor = if (isDark) Color(0xFF141418) else Color(0xFFFAFAFC)
    val borderColor = if (isDark) Color(0xFF27272F) else Color(0xFFE4E4E7)

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = surfaceColor,
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: @Query indicator + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricCyan.copy(alpha = 0.16f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AlternateEmail,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (query.isBlank()) "Reference item" else "Referencing \"$query\"",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Pills: All, Contacts, Apps, Calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPill(
                    label = "All",
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    isDark = isDark
                )
                FilterPill(
                    label = "Contacts",
                    icon = Icons.Outlined.Person,
                    selected = selectedFilter == MentionType.CONTACT,
                    onClick = { selectedFilter = MentionType.CONTACT },
                    isDark = isDark,
                    activeColor = ElectricGreen
                )
                FilterPill(
                    label = "Apps",
                    icon = Icons.Outlined.Apps,
                    selected = selectedFilter == MentionType.APP,
                    onClick = { selectedFilter = MentionType.APP },
                    isDark = isDark,
                    activeColor = ElectricCyan
                )
                FilterPill(
                    label = "Calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    selected = selectedFilter == MentionType.CALENDAR,
                    onClick = { selectedFilter = MentionType.CALENDAR },
                    isDark = isDark,
                    activeColor = ElectricAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Request Banners if permission is missing
            val showContactsPrompt = (!hasContacts) && (selectedFilter == null || selectedFilter == MentionType.CONTACT)
            val showCalendarPrompt = (!hasCalendar) && (selectedFilter == null || selectedFilter == MentionType.CALENDAR)

            if (showContactsPrompt) {
                PermissionPromptRow(
                    text = "Grant Contacts access to search & reference contacts",
                    buttonText = "Grant",
                    isDark = isDark,
                    tint = ElectricGreen,
                    onGrant = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (showCalendarPrompt) {
                PermissionPromptRow(
                    text = "Grant Calendar access to search & reference events",
                    buttonText = "Grant",
                    isDark = isDark,
                    tint = ElectricAmber,
                    onGrant = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) }
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                        color = ElectricCyan
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            isDark = isDark,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    activeColor: Color = ElectricCyan
) {
    val bgColor = if (selected) {
        activeColor.copy(alpha = if (isDark) 0.22f else 0.14f)
    } else {
        if (isDark) Color(0xFF1E1E24) else Color(0xFFEDEDF0)
    }
    val contentColor = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
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
    isDark: Boolean,
    tint: Color,
    onGrant: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = if (isDark) 0.12f else 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = tint.copy(alpha = 0.22f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onGrant)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MentionItemRow(
    item: MentionItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val (typeColor, typeIcon, typeLabel) = when (item.type) {
        MentionType.CONTACT -> Triple(ElectricGreen, Icons.Outlined.Person, "Contact")
        MentionType.APP -> Triple(ElectricCyan, Icons.Outlined.Apps, "App")
        MentionType.CALENDAR -> Triple(ElectricAmber, Icons.Outlined.CalendarMonth, "Calendar")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = typeColor.copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = typeColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = typeLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = typeColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
