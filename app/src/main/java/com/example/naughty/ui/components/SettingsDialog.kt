package com.example.naughty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.IconButton
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.BrandSubtitleStyle
import com.example.naughty.ui.theme.BrandTitleStyle
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.ThemeMode
import com.example.naughty.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.BorderStroke

@Composable
fun SettingsDrawerContent(
    currentTheme: ThemeMode,
    notesCount: Int,
    boundAppsCount: Int,
    archivedCount: Int = 0,
    deletedCount: Int = 0,
    archivedNotes: List<NoteMetadata> = emptyList(),
    deletedNotes: List<NoteMetadata> = emptyList(),
    timelineCount: Int = 0,
    onThemeSelected: (ThemeMode) -> Unit,
    onOpenTimeline: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onRestoreNote: (String) -> Unit = {},
    onUnarchiveNote: (String) -> Unit = {},
    onPermanentlyDeleteNote: (String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    onPermanentlyDeleteAllArchived: () -> Unit = {},
    onClearAllNotes: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showArchivedDialog by remember { mutableStateOf(false) }
    var showDeletedDialog by remember { mutableStateOf(false) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var showDeleteAllArchivedConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "2.1.6"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "9"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "9"
    }

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = if (isDark) AmoledBlack else CrystalWhite,
        drawerContentColor = if (isDark) CrystalWhite else AmoledBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            // Brand Monogram & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) ElectricGreen.copy(alpha = 0.15f) else AmoledBlack.copy(alpha = 0.08f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "N",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ElectricGreen else AmoledBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "NAUGHTY",
                        style = BrandTitleStyle,
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                    Text(
                        text = "Contextual Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Workspace Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = null,
                                tint = if (isDark) ElectricGreen else AmoledBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$notesCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ElectricGreen else AmoledBlack
                            )
                        }
                        Text(
                            text = "Active Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$boundAppsCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                        Text(
                            text = "Bound Apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            ThemeOptionRow(
                title = "Light theme",
                icon = Icons.Outlined.LightMode,
                selected = currentTheme == ThemeMode.LIGHT,
                onClick = { onThemeSelected(ThemeMode.LIGHT) }
            )

            ThemeOptionRow(
                title = "Dark theme",
                icon = Icons.Outlined.DarkMode,
                selected = currentTheme == ThemeMode.DARK,
                onClick = { onThemeSelected(ThemeMode.DARK) }
            )

            ThemeOptionRow(
                title = "Follow system",
                icon = Icons.Outlined.BrightnessAuto,
                selected = currentTheme == ThemeMode.AUTO,
                onClick = { onThemeSelected(ThemeMode.AUTO) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Track Section (Timeline Tasks)
            Text(
                text = "TRACK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onCloseDrawer()
                        onOpenTimeline()
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Timeline tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track progress of important tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (timelineCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ElectricCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$timelineCount",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Workspace Management
            Text(
                text = "WORKSPACE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Archived Notes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showArchivedDialog = true }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Archived notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "View, restore or permanently delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (archivedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ElectricAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$archivedCount",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = ElectricAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Deleted Notes (Trash)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDeletedDialog = true }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deleted notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Trash • View, restore or permanently delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (deletedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ElectricPink.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$deletedCount",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = ElectricPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Clear all active notes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showClearDialog = true }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    tint = ElectricPink,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Clear all active notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ElectricPink
                    )
                    Text(
                        text = "Delete all active notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // About & Transparency footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showAboutDialog = true }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About & Privacy",
                    tint = if (isDark) ElectricGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Naughty v$versionName • Build $versionCode",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "100% Offline • Architecture & Permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutAndPrivacyDialog(
            versionName = versionName,
            versionCode = versionCode,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showArchivedDialog) {
        AlertDialog(
            onDismissRequest = { showArchivedDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Archived Notes (${archivedNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (archivedNotes.isNotEmpty()) {
                        TextButton(
                            onClick = { showDeleteAllArchivedConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete All", fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                if (archivedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No archived notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(archivedNotes, key = { it.id }) { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showArchivedDialog = false
                                        onCloseDrawer()
                                        onNoteClick(note.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Tap to view • Restore or delete below",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onUnarchiveNote(note.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Unarchive,
                                            contentDescription = "Unarchive",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPermanentlyDeleteNote(note.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteForever,
                                            contentDescription = "Permanently Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchivedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeletedDialog) {
        AlertDialog(
            onDismissRequest = { showDeletedDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deleted Notes (${deletedNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (deletedNotes.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Empty Trash", fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                if (deletedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trash is empty",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedNotes, key = { it.id }) { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDeletedDialog = false
                                        onCloseDrawer()
                                        onNoteClick(note.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "In trash • Tap to view",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRestoreNote(note.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Restore,
                                            contentDescription = "Restore",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPermanentlyDeleteNote(note.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteForever,
                                            contentDescription = "Permanently Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeletedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash?") },
            text = { Text("All deleted notes will be permanently destroyed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyTrashConfirm = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAllArchivedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllArchivedConfirm = false },
            title = { Text("Delete All Archived Notes?") },
            text = { Text("All archived notes will be permanently destroyed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllArchivedConfirm = false
                        onPermanentlyDeleteAllArchived()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllArchivedConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all notes?") },
            text = { Text("This will permanently delete all notes and checklists. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAllNotes()
                        onCloseDrawer()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val activeBg = if (isDark) ElectricGreen.copy(alpha = 0.14f) else AmoledBlack.copy(alpha = 0.08f)
    val activeTint = if (isDark) ElectricGreen else AmoledBlack
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) activeTint else MaterialTheme.colorScheme.onSurface
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = activeTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AboutAndPrivacyDialog(
    versionName: String,
    versionCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    val hasUsageStats = remember { hasUsageStatsPermission(context) }
    val hasNotifications = remember { hasNotificationPermission(context) }
    val hasMicrophone = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    val canScheduleExactAlarm = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            am?.canScheduleExactAlarms() ?: true
        } else true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) ElectricGreen.copy(alpha = 0.15f) else AmoledBlack.copy(alpha = 0.08f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "N",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) ElectricGreen else AmoledBlack
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "About Naughty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v$versionName (Build $versionCode) • 100% Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Technical Architecture Specs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                    border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = if (isDark) ElectricGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TECHNICAL SPECS",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDark) ElectricGreen else MaterialTheme.colorScheme.primary
                            )
                        }

                        TechSpecRow("Engine", "100% Offline • Zero Network")
                        TechSpecRow("Storage", "Room SQLite • Markdown (.md)")
                        TechSpecRow("Audio", "Audx RNNoise • Opus 48 kHz")
                        TechSpecRow("Vision/Speech", "ML Kit OCR • SpeechRecognizer")
                        TechSpecRow("Security", "BiometricStrong (Hardware)")
                    }
                }

                // System Permissions Status Chips
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                    border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SYSTEM PERMISSIONS",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PermissionChip(
                                modifier = Modifier.weight(1f),
                                label = "Usage",
                                granted = hasUsageStats,
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            )
                            PermissionChip(
                                modifier = Modifier.weight(1f),
                                label = "Alerts",
                                granted = hasNotifications,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        })
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        })
                                    }
                                }
                            )
                            PermissionChip(
                                modifier = Modifier.weight(1f),
                                label = "Alarms",
                                granted = canScheduleExactAlarm,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        })
                                    }
                                }
                            )
                            PermissionChip(
                                modifier = Modifier.weight(1f),
                                label = "Mic",
                                granted = hasMicrophone,
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    })
                                }
                            )
                        }
                    }
                }

                // Credits & License
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                    border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = ElectricAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CREDITS & LICENSE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = ElectricAmber
                            )
                        }
                        Text(
                            text = "MIT License • Audx (RNNoise) • Markwon",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PermissionChip(
    modifier: Modifier = Modifier,
    label: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF16161A) else Color(0xFFECECEF),
        border = BorderStroke(
            0.5.dp,
            if (granted) (if (isDark) ElectricGreen.copy(alpha = 0.4f) else ElectricGreen.copy(alpha = 0.6f))
            else (if (isDark) Color(0xFF2C2C34) else Color(0xFFD4D4D8))
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (granted) ElectricGreen else ElectricPink)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TechSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

