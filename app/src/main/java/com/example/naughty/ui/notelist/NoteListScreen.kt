package com.example.naughty.ui.notelist

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.components.ClipSaveSheet
import com.example.naughty.ui.oneui.components.OneUiBottomDock
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiCardGroup
import com.example.naughty.ui.oneui.components.OneUiChipRow
import com.example.naughty.ui.oneui.components.OneUiConfirmDialog
import com.example.naughty.ui.oneui.components.OneUiEmptyState
import com.example.naughty.ui.oneui.components.OneUiHeaderStyle
import com.example.naughty.ui.oneui.components.OneUiLeading
import com.example.naughty.ui.oneui.components.OneUiListItem
import com.example.naughty.ui.oneui.components.OneUiScaffold
import com.example.naughty.ui.oneui.components.OneUiSectionHeader
import com.example.naughty.ui.oneui.components.OneUiTrailing
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.util.BiometricAuthHelper
import com.example.naughty.util.BiometricStatus
import com.example.naughty.util.NoteLockSession
import com.example.naughty.util.findFragmentActivity
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (String) -> Unit,
    onOpenTimeline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onTriggerSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val trackerCount by viewModel.trackerCount.collectAsStateWithLifecycle()
    val ctsClipCount by viewModel.ctsClipCount.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) { context.findFragmentActivity() }

    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    var pendingClipText by remember { mutableStateOf<String?>(null) }
    var hasPendingClip by remember { mutableStateOf(false) }
    var showClipSaveSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasTextType = clipboardManager?.hasPrimaryClip() == true &&
                (clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                        clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true)
        if (hasTextType) {
            val clip = clipboardManager.primaryClip?.takeIf { it.itemCount > 0 }?.let { clipData ->
                val item = clipData.getItemAt(0)
                try {
                    item?.coerceToText(context)?.toString()?.trim()
                } catch (_: Exception) {
                    item?.text?.toString()?.trim()
                }
            }
            if (!clip.isNullOrBlank() && !viewModel.isClipRecorded(clip)) {
                hasPendingClip = true
                pendingClipText = clip
            } else {
                hasPendingClip = false
                pendingClipText = null
            }
        } else {
            hasPendingClip = false
            pendingClipText = null
        }
    }

    var actionNote by remember { mutableStateOf<NoteMetadata?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteMetadata?>(null) }
    var showNoSecurityDialog by remember { mutableStateOf(false) }

    val noteCountText = if (uiState.notes.size == 1) "1 note" else "${uiState.notes.size} notes"
    val sectionedNotes = remember(uiState.notes) { NoteSections.bucket(uiState.notes) }

    OneUiScaffold(
        title = "Notes",
        subtitle = noteCountText,
        headerStyle = OneUiHeaderStyle.Expanded,
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = OneUi.colors.textPrimary
                )
            }
        },
        bottomDock = {
            OneUiBottomDock(
                placeholder = "Search notes",
                onSearchClick = onTriggerSearch,
                onAddClick = {
                    viewModel.createNote(
                        title = "",
                        content = "",
                        cardType = "modular"
                    ) { newId ->
                        onNoteClick(newId)
                    }
                }
            )
        },
        listState = listState,
        modifier = modifier
    ) {
        // Filter Chips Row
        item(key = "home_filters") {
            OneUiChipRow(
                items = listOf("All", "Tasks", "Notes", "Pinned"),
                selectedIndex = uiState.selectedPageIndex,
                onSelect = { viewModel.onSelectPage(it) },
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Top Shortcut Island: Timeline & Circle to Search
        item(key = "home_shortcuts") {
            OneUiCardGroup {
                item {
                    OneUiListItem(
                        title = "Timeline Tasks",
                        subtitle = if (trackerCount > 0) "$trackerCount active ${if (trackerCount == 1) "task" else "tasks"}" else "Track multi-step goals",
                        leading = OneUiLeading.IconBadge(
                            icon = Icons.Outlined.Timeline,
                            tint = OneUi.colors.badge.teal
                        ),
                        trailing = OneUiTrailing.Chevron,
                        onClick = onOpenTimeline
                    )
                }
                item {
                    val ctsSubtitle = if (hasPendingClip) {
                        "New clip ready from clipboard • Tap to add"
                    } else if (ctsClipCount > 0) {
                        "$ctsClipCount ${if (ctsClipCount == 1) "clip" else "clips"} captured"
                    } else {
                        "Capture snippets from search"
                    }
                    OneUiListItem(
                        title = "Circle to Search",
                        subtitle = ctsSubtitle,
                        subtitleColor = if (hasPendingClip) OneUi.colors.warning else OneUi.colors.textSecondary,
                        leading = OneUiLeading.IconBadge(
                            icon = Icons.Outlined.Search,
                            tint = if (hasPendingClip) OneUi.colors.warning else OneUi.colors.badge.blue
                        ),
                        trailing = if (hasPendingClip) OneUiTrailing.CountBadge("NEW", isHighlight = true) else OneUiTrailing.Chevron,
                        onClick = {
                            if (hasPendingClip && pendingClipText != null) {
                                showClipSaveSheet = true
                            } else {
                                coroutineScope.launch {
                                    val ctsId = viewModel.getOrCreateCtsNoteId()
                                    onNoteClick(ctsId)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Empty State or Notes List Sectioned by Date
        if (uiState.notes.isEmpty() && !uiState.isLoading) {
            item(key = "home_empty_state") {
                OneUiEmptyState(
                    title = "No notes yet",
                    message = "Tap + to jot down notes, checklists, voice memos, or scan text",
                    icon = Icons.Outlined.Search
                )
            }
        } else {
            sectionedNotes.forEach { (sectionTitle, notes) ->
                item(key = "header_$sectionTitle") {
                    OneUiSectionHeader(title = sectionTitle)
                }
                items(
                    items = notes,
                    key = { it.id }
                ) { note ->
                    Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter)) {
                        NotePreviewCard(
                            note = note,
                            rawContent = uiState.rawContents[note.id] ?: "",
                            boundApps = uiState.boundApps[note.id] ?: emptyList(),
                            onClick = {
                                if (note.isLocked) {
                                    if (activity != null) {
                                        BiometricAuthHelper.authenticate(
                                            activity = activity,
                                            title = "Unlock Note",
                                            subtitle = "Authenticate to view and edit this note",
                                            onSuccess = {
                                                NoteLockSession.unlock(note.id)
                                                onNoteClick(note.id)
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                } else {
                                    onNoteClick(note.id)
                                }
                            },
                            onLongClick = {
                                actionNote = note
                            }
                        )
                    }
                }
            }
        }
    }

    // Long-Press Action Bottom Sheet
    if (actionNote != null) {
        val targetNote = actionNote!!
        OneUiBottomSheet(
            title = targetNote.title.ifBlank { "Untitled" },
            onDismissRequest = { actionNote = null }
        ) {
            OneUiCardGroup {
                // Pin / Unpin
                item {
                    OneUiListItem(
                        title = if (targetNote.isPinned) "Unpin note" else "Pin note",
                        leading = OneUiLeading.RawIcon(Icons.Filled.PushPin, tint = OneUi.colors.accent),
                        onClick = {
                            viewModel.togglePin(targetNote.id)
                            actionNote = null
                        }
                    )
                }

                // Lock / Unlock
                item {
                    OneUiListItem(
                        title = if (targetNote.isLocked) "Unlock note (remove lock)" else "Lock note",
                        leading = OneUiLeading.RawIcon(
                            if (targetNote.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                            tint = OneUi.colors.warning
                        ),
                        onClick = {
                            val toLock = !targetNote.isLocked
                            actionNote = null
                            if (toLock) {
                                val status = BiometricAuthHelper.canAuthenticate(context)
                                when (status) {
                                    BiometricStatus.AVAILABLE -> {
                                        if (activity != null) {
                                            BiometricAuthHelper.authenticate(
                                                activity = activity,
                                                title = "Lock Note",
                                                subtitle = "Authenticate to secure this note",
                                                onSuccess = {
                                                    viewModel.toggleLock(targetNote.id, true)
                                                    Toast.makeText(context, "Note locked", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            viewModel.toggleLock(targetNote.id, true)
                                        }
                                    }
                                    BiometricStatus.NOT_ENROLLED -> {
                                        showNoSecurityDialog = true
                                    }
                                    else -> {
                                        Toast.makeText(context, "Screen lock or biometric is unavailable on this device.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                if (activity != null) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = "Unlock Note",
                                        subtitle = "Authenticate to remove lock",
                                        onSuccess = {
                                            viewModel.toggleLock(targetNote.id, false)
                                            Toast.makeText(context, "Note unlocked", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    viewModel.toggleLock(targetNote.id, false)
                                }
                            }
                        }
                    )
                }

                // Delete
                item {
                    OneUiListItem(
                        title = "Delete note",
                        titleColor = OneUi.colors.destructive,
                        leading = OneUiLeading.RawIcon(Icons.Outlined.Delete, tint = OneUi.colors.destructive),
                        onClick = {
                            noteToDelete = targetNote
                            actionNote = null
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (noteToDelete != null) {
        val target = noteToDelete!!
        OneUiConfirmDialog(
            title = "Delete Note?",
            message = if (target.title.isNotBlank()) "Move \"${target.title.trim()}\" to Trash?" else "Move this note to Trash?",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteNote(target.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }

    // Screen Lock Required Dialog
    if (showNoSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showNoSecurityDialog = false },
            title = { Text("Screen Lock Required") },
            text = {
                Text("To lock notes with system security, please set up a PIN, pattern, password, or biometric in your device Settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoSecurityDialog = false
                        BiometricAuthHelper.openSecuritySettings(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoSecurityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clipboard CTS Clip Save Sheet
    if (showClipSaveSheet && pendingClipText != null) {
        ClipSaveSheet(
            clipText = pendingClipText!!,
            sourceAppName = "Clipboard",
            onSaveToCtsNote = {
                val text = pendingClipText!!
                showClipSaveSheet = false
                pendingClipText = null
                hasPendingClip = false
                coroutineScope.launch {
                    val ctsId = viewModel.appendCtsClip("Clipboard", text)
                    Toast.makeText(context, "Saved to Circle to Search", Toast.LENGTH_SHORT).show()
                    onNoteClick(ctsId)
                }
            },
            onDismiss = {
                showClipSaveSheet = false
                pendingClipText?.let { viewModel.markClipAsHandled(it) }
                hasPendingClip = false
                pendingClipText = null
            }
        )
    }
}
