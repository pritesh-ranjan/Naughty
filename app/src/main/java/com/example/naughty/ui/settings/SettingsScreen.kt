package com.example.naughty.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.components.FeatureShowcaseSheet
import com.example.naughty.ui.components.hasCalendarPermission
import com.example.naughty.ui.components.hasContactsPermission
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiCardGroup
import com.example.naughty.ui.oneui.components.OneUiConfirmDialog
import com.example.naughty.ui.oneui.components.OneUiHeaderStyle
import com.example.naughty.ui.oneui.components.OneUiLeading
import com.example.naughty.ui.oneui.components.OneUiListItem
import com.example.naughty.ui.oneui.components.OneUiScaffold
import com.example.naughty.ui.oneui.components.OneUiTextButton
import com.example.naughty.ui.oneui.components.OneUiTrailing
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.theme.THEME_MODE_KEY
import com.example.naughty.ui.theme.ThemeMode
import com.example.naughty.ui.theme.ctsNotificationPreferenceFlow
import com.example.naughty.ui.theme.setCtsNotificationEnabled
import com.example.naughty.ui.theme.themeDataStore
import com.example.naughty.ui.theme.themePreferenceFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentTheme by context.themePreferenceFlow().collectAsState(initial = ThemeMode.LIGHT)
    val isCtsNotificationEnabled by context.ctsNotificationPreferenceFlow().collectAsState(initial = true)
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val boundAppsCount by viewModel.boundAppsCount.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val deletedCount by viewModel.deletedCount.collectAsStateWithLifecycle()

    var showDeletedSheet by remember { mutableStateOf(false) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var showPurgeConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var noteToPermanentlyDelete by remember { mutableStateOf<NoteMetadata?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFeatureShowcaseSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasContacts by remember { mutableStateOf(hasContactsPermission(context)) }
    var hasCalendar by remember { mutableStateOf(hasCalendarPermission(context)) }
    var permissionToRevoke by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasContacts = hasContactsPermission(context)
                hasCalendar = hasCalendarPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContacts = isGranted
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendar = isGranted
    }

    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "2.3.5"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "20"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "20"
    }

    OneUiScaffold(
        title = "Settings",
        headerStyle = OneUiHeaderStyle.Expanded,
        onBack = onBack,
        modifier = modifier
    ) {
        // Profile-Style Workspace Card (Mirrors Samsung account card)
        item(key = "settings_workspace_profile") {
            OneUiCardGroup {
                item {
                    OneUiListItem(
                        title = "Naughty Workspace",
                        subtitle = "$activeCount active ${if (activeCount == 1) "note" else "notes"} • $boundAppsCount app ${if (boundAppsCount == 1) "binding" else "bindings"}",
                        leading = OneUiLeading.Avatar(initials = "N", background = OneUi.colors.accent),
                        trailing = OneUiTrailing.None
                    )
                }
            }
        }

        // Appearance Group
        item(key = "settings_appearance_group") {
            OneUiCardGroup(label = "Appearance") {
                item {
                    OneUiListItem(
                        title = "Light theme",
                        leading = OneUiLeading.RawIcon(Icons.Outlined.LightMode, tint = OneUi.colors.textPrimary),
                        trailing = if (currentTheme == ThemeMode.LIGHT) OneUiTrailing.Checkmark else OneUiTrailing.None,
                        onClick = {
                            coroutineScope.launch {
                                context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.LIGHT.name }
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Dark theme",
                        leading = OneUiLeading.RawIcon(Icons.Outlined.DarkMode, tint = OneUi.colors.textPrimary),
                        trailing = if (currentTheme == ThemeMode.DARK) OneUiTrailing.Checkmark else OneUiTrailing.None,
                        onClick = {
                            coroutineScope.launch {
                                context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.DARK.name }
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Follow system",
                        leading = OneUiLeading.RawIcon(Icons.Outlined.BrightnessAuto, tint = OneUi.colors.textPrimary),
                        trailing = if (currentTheme == ThemeMode.AUTO) OneUiTrailing.Checkmark else OneUiTrailing.None,
                        onClick = {
                            coroutineScope.launch {
                                context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.AUTO.name }
                            }
                        }
                    )
                }
            }
        }

        // Circle to Search Group
        item(key = "settings_cts_group") {
            OneUiCardGroup(label = "Circle to Search") {
                item {
                    OneUiListItem(
                        title = "Capture notes notification",
                        subtitle = "Prompt to save snippets after exiting Circle to Search",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Search, OneUi.colors.badge.blue),
                        trailing = OneUiTrailing.Switch(
                            checked = isCtsNotificationEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    context.setCtsNotificationEnabled(enabled)
                                }
                            }
                        )
                    )
                }
            }
        }

        // Permissions & Integrations Group
        item(key = "settings_permissions_group") {
            OneUiCardGroup(label = "Permissions & Integrations") {
                item {
                    OneUiListItem(
                        title = "Contacts access",
                        subtitle = if (hasContacts) "Granted • Reference contacts with @mention" else "Denied • Tap to allow @contact references",
                        subtitleColor = if (hasContacts) OneUi.colors.textSecondary else OneUi.colors.warning,
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Person, OneUi.colors.badge.green),
                        trailing = OneUiTrailing.CountBadge(
                            text = if (hasContacts) "Granted" else "Grant",
                            isHighlight = !hasContacts
                        ),
                        onClick = {
                            if (hasContacts) {
                                permissionToRevoke = "Contacts"
                            } else {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Calendar access",
                        subtitle = if (hasCalendar) "Granted • Reference events with @mention" else "Denied • Tap to allow @calendar references",
                        subtitleColor = if (hasCalendar) OneUi.colors.textSecondary else OneUi.colors.warning,
                        leading = OneUiLeading.IconBadge(Icons.Outlined.CalendarMonth, OneUi.colors.badge.orange),
                        trailing = OneUiTrailing.CountBadge(
                            text = if (hasCalendar) "Granted" else "Grant",
                            isHighlight = !hasCalendar
                        ),
                        onClick = {
                            if (hasCalendar) {
                                permissionToRevoke = "Calendar"
                            } else {
                                calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        }
                    )
                }
            }
        }

        // Notes & Storage Group
        item(key = "settings_storage_group") {
            OneUiCardGroup(label = "Notes & Storage") {
                item {
                    OneUiListItem(
                        title = "Deleted notes",
                        subtitle = "Trash • View, restore or permanently delete",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Delete, OneUi.colors.badge.gray),
                        trailing = if (deletedCount > 0) OneUiTrailing.CountBadge("$deletedCount", isHighlight = true) else OneUiTrailing.Chevron,
                        onClick = { showDeletedSheet = true }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Clean up deleted & blank notes",
                        subtitle = "Purge soft-deleted trash and empty drafts from database",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.DeleteForever, OneUi.colors.badge.gray),
                        trailing = OneUiTrailing.Chevron,
                        onClick = { showPurgeConfirm = true }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Clear all active notes",
                        subtitle = "Delete all active notes from workspace",
                        titleColor = OneUi.colors.destructive,
                        leading = OneUiLeading.IconBadge(Icons.Outlined.DeleteSweep, OneUi.colors.destructive),
                        onClick = { showClearAllConfirm = true }
                    )
                }
            }
        }

        // About Group
        item(key = "settings_about_group") {
            OneUiCardGroup(label = "About") {
                item {
                    OneUiListItem(
                        title = "Feature Showcase & Key Highlights",
                        subtitle = "Explore all key offline & context features",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.AutoAwesome, OneUi.colors.badge.purple),
                        trailing = OneUiTrailing.Chevron,
                        onClick = { showFeatureShowcaseSheet = true }
                    )
                }
                item {
                    OneUiListItem(
                        title = "About Naughty",
                        subtitle = "v$versionName (Build $versionCode) • 100% Offline",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Info, OneUi.colors.badge.teal),
                        trailing = OneUiTrailing.Chevron,
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Feature Showcase Sheet
    if (showFeatureShowcaseSheet) {
        FeatureShowcaseSheet(
            versionName = versionName,
            versionCode = versionCode.toLongOrNull() ?: 1L,
            isFirstLaunch = false,
            onDismiss = { showFeatureShowcaseSheet = false }
        )
    }

    // About & Privacy Sheet
    if (showAboutDialog) {
        AboutAndPrivacyDialog(
            versionName = versionName,
            versionCode = versionCode,
            onDismiss = { showAboutDialog = false }
        )
    }

    // Deleted Notes (Trash) Bottom Sheet
    if (showDeletedSheet) {
        OneUiBottomSheet(
            title = "Deleted Notes (${deletedNotes.size})",
            subtitle = if (deletedNotes.isNotEmpty()) "Tap a note to view, restore, or delete permanently" else "Trash is empty",
            onDismissRequest = { showDeletedSheet = false }
        ) {
            if (deletedNotes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OneUiTextButton(
                        text = "Empty Trash",
                        color = OneUi.colors.destructive,
                        onClick = { showEmptyTrashConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OneUiCardGroup {
                    deletedNotes.forEach { note ->
                        item(key = note.id) {
                            OneUiListItem(
                                title = note.title.ifBlank { "Untitled" },
                                subtitle = "In trash • Tap to view",
                                leading = OneUiLeading.RawIcon(Icons.Outlined.DeleteOutline, tint = OneUi.colors.textSecondary),
                                trailing = OneUiTrailing.Custom {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.restoreNote(note.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Restore,
                                                contentDescription = "Restore",
                                                tint = OneUi.colors.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { noteToPermanentlyDelete = note },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteForever,
                                                contentDescription = "Permanently delete",
                                                tint = OneUi.colors.destructive,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showDeletedSheet = false
                                    onNoteClick(note.id)
                                }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Trash is empty",
                        style = OneUi.type.cardBody,
                        color = OneUi.colors.textSecondary
                    )
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showEmptyTrashConfirm) {
        OneUiConfirmDialog(
            title = "Empty Trash?",
            message = "All deleted notes will be permanently destroyed. This action cannot be undone.",
            confirmLabel = "Empty Trash",
            destructive = true,
            onConfirm = {
                showEmptyTrashConfirm = false
                viewModel.emptyTrash()
            },
            onDismiss = { showEmptyTrashConfirm = false }
        )
    }

    if (showPurgeConfirm) {
        OneUiConfirmDialog(
            title = "Clean Up Notes?",
            message = "Purge soft-deleted trash and blank notes from your database to optimize storage.",
            confirmLabel = "Clean Up",
            destructive = false,
            onConfirm = {
                showPurgeConfirm = false
                viewModel.purgeDeletedAndBlankNotes()
            },
            onDismiss = { showPurgeConfirm = false }
        )
    }

    if (showClearAllConfirm) {
        OneUiConfirmDialog(
            title = "Clear All Active Notes?",
            message = "All active notes will be moved to Trash. You can restore them anytime.",
            confirmLabel = "Clear All",
            destructive = true,
            onConfirm = {
                showClearAllConfirm = false
                viewModel.clearAllNotes()
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }

    if (noteToPermanentlyDelete != null) {
        val target = noteToPermanentlyDelete!!
        OneUiConfirmDialog(
            title = "Permanently Delete?",
            message = "Delete \"${target.title.trim().ifBlank { "this note" }}\" permanently? This action cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.permanentlyDeleteNote(target.id)
                noteToPermanentlyDelete = null
            },
            onDismiss = { noteToPermanentlyDelete = null }
        )
    }

    if (permissionToRevoke != null) {
        val perm = permissionToRevoke!!
        OneUiConfirmDialog(
            title = "Revoke $perm Access?",
            message = "$perm permission is currently granted. To revoke it, open Android App Settings and toggle permissions.",
            confirmLabel = "Open App Settings",
            destructive = false,
            onConfirm = {
                permissionToRevoke = null
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { permissionToRevoke = null }
        )
    }
}
