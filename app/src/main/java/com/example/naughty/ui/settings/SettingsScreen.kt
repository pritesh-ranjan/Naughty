package com.example.naughty.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
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
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import com.example.naughty.ui.components.FeatureShowcaseSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.naughty.ui.components.hasCalendarPermission
import com.example.naughty.ui.components.hasContactsPermission
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.THEME_MODE_KEY
import com.example.naughty.ui.theme.ThemeMode
import com.example.naughty.ui.theme.ctsNotificationPreferenceFlow
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.ui.theme.setCtsNotificationEnabled
import com.example.naughty.ui.theme.themeDataStore
import com.example.naughty.ui.theme.themePreferenceFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()

    val currentTheme by context.themePreferenceFlow().collectAsState(initial = ThemeMode.LIGHT)
    val isCtsNotificationEnabled by context.ctsNotificationPreferenceFlow().collectAsState(initial = true)
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val boundAppsCount by viewModel.boundAppsCount.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val deletedCount by viewModel.deletedCount.collectAsStateWithLifecycle()

    var showDeletedDialog by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header: Back button + "Settings" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Workspace Summary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                    contentDescription = null,
                                    tint = if (isDark) ElectricGreen else AmoledBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$activeCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ElectricGreen else AmoledBlack
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
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
                                .height(32.dp)
                                .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$boundAppsCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bound Apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // APPEARANCE SECTION
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                ThemeOptionRow(
                    title = "Light theme",
                    icon = Icons.Outlined.LightMode,
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = {
                        coroutineScope.launch {
                            context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.LIGHT.name }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                ThemeOptionRow(
                    title = "Dark theme",
                    icon = Icons.Outlined.DarkMode,
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = {
                        coroutineScope.launch {
                            context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.DARK.name }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                ThemeOptionRow(
                    title = "Follow system",
                    icon = Icons.Outlined.BrightnessAuto,
                    selected = currentTheme == ThemeMode.AUTO,
                    onClick = {
                        coroutineScope.launch {
                            context.themeDataStore.edit { it[THEME_MODE_KEY] = ThemeMode.AUTO.name }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // CIRCLE TO SEARCH SECTION
                Text(
                    text = "CIRCLE TO SEARCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch {
                                context.setCtsNotificationEnabled(!isCtsNotificationEnabled)
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (isDark) ElectricCyan else Color(0xFF007799),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Capture notes notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prompt to save snippets after exiting Circle to Search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = isCtsNotificationEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                context.setCtsNotificationEnabled(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmoledBlack,
                            checkedTrackColor = ElectricGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // PERMISSIONS & INTEGRATIONS SECTION
                Text(
                    text = "PERMISSIONS & INTEGRATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Contacts Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (hasContacts) {
                                permissionToRevoke = "Contacts"
                            } else {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = if (isDark) ElectricGreen else Color(0xFF1B5E20),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contacts access",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasContacts) "Granted • Reference contacts with @mention" else "Denied • Tap to allow @contact references",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasContacts) MaterialTheme.colorScheme.onSurfaceVariant else ElectricAmber,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasContacts) {
                            if (isDark) ElectricGreen.copy(alpha = 0.16f) else Color(0xFFE8F5E9)
                        } else {
                            if (isDark) ElectricAmber.copy(alpha = 0.16f) else Color(0xFFFFF3E0)
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = if (hasContacts) "Granted" else "Grant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasContacts) {
                                if (isDark) ElectricGreen else Color(0xFF2E7D32)
                            } else {
                                if (isDark) ElectricAmber else Color(0xFFE65100)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Calendar Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (hasCalendar) {
                                permissionToRevoke = "Calendar"
                            } else {
                                calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = ElectricAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendar access",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasCalendar) "Granted • Reference events with @mention" else "Denied • Tap to allow @calendar references",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasCalendar) MaterialTheme.colorScheme.onSurfaceVariant else ElectricAmber,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasCalendar) {
                            if (isDark) ElectricGreen.copy(alpha = 0.16f) else Color(0xFFE8F5E9)
                        } else {
                            if (isDark) ElectricAmber.copy(alpha = 0.16f) else Color(0xFFFFF3E0)
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = if (hasCalendar) "Granted" else "Grant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCalendar) {
                                if (isDark) ElectricGreen else Color(0xFF2E7D32)
                            } else {
                                if (isDark) ElectricAmber else Color(0xFFE65100)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // NOTES & STORAGE SECTION
                Text(
                    text = "NOTES & STORAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Deleted Notes (Trash)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDeletedDialog = true }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
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

                // Clean up deleted & blank notes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPurgeConfirm = true }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clean up deleted & blank notes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Purge soft-deleted trash and empty drafts from database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Clear all active notes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showClearAllConfirm = true }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
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
                            text = "Delete all active notes from workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ABOUT SECTION
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFeatureShowcaseSheet = true }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Feature Showcase",
                        tint = if (isDark) ElectricGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Feature Showcase & Key Highlights",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Explore all key offline & context features",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAboutDialog = true }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About & Privacy",
                        tint = if (isDark) ElectricGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Naughty v$versionName • Build $versionCode",
                            style = MaterialTheme.typography.bodyMedium,
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showFeatureShowcaseSheet) {
        FeatureShowcaseSheet(
            versionName = versionName,
            versionCode = versionCode.toLongOrNull() ?: 1L,
            isFirstLaunch = false,
            onDismiss = { showFeatureShowcaseSheet = false }
        )
    }

    if (showAboutDialog) {
        AboutAndPrivacyDialog(
            versionName = versionName,
            versionCode = versionCode,
            onDismiss = { showAboutDialog = false }
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
                                        onClick = { viewModel.restoreNote(note.id) },
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
                                        onClick = { noteToPermanentlyDelete = note },
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
                        viewModel.emptyTrash()
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

    if (noteToPermanentlyDelete != null) {
        val target = noteToPermanentlyDelete!!
        AlertDialog(
            onDismissRequest = { noteToPermanentlyDelete = null },
            title = { Text("Permanently delete note?") },
            text = {
                val title = target.title.trim()
                if (title.isNotBlank()) {
                    Text("Are you sure you want to permanently delete \"$title\"? This action cannot be undone.")
                } else {
                    Text("Are you sure you want to permanently delete this note? This action cannot be undone.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteNote(target.id)
                        noteToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToPermanentlyDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear all active notes?") },
            text = { Text("This will permanently delete all notes and checklists. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirm = false
                        viewModel.clearAllNotes()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPurgeConfirm) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirm = false },
            title = { Text("Clean up database?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently purge all soft-deleted trash notes and empty blank drafts from the database. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeConfirm = false
                        viewModel.purgeDeletedAndBlankNotes { count ->
                            android.widget.Toast.makeText(
                                context,
                                if (count > 0) "Cleaned up $count deleted & blank notes" else "Database is clean (0 deleted/blank notes)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricPink)
                ) {
                    Text("Clean Up", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (permissionToRevoke != null) {
        val permName = permissionToRevoke!!
        AlertDialog(
            onDismissRequest = { permissionToRevoke = null },
            title = {
                Text(
                    text = "Manage $permName Permission",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "$permName access is currently granted to Naughty. To revoke this permission, Android requires changing it in the App Info settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        permissionToRevoke = null
                    }
                ) {
                    Text(
                        "Open App Settings",
                        color = if (isDark) ElectricCyan else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { permissionToRevoke = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
