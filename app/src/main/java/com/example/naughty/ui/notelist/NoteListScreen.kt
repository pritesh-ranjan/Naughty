package com.example.naughty.ui.notelist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.naughty.util.BiometricAuthHelper
import com.example.naughty.util.BiometricStatus
import com.example.naughty.util.NoteLockSession
import com.example.naughty.util.findFragmentActivity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.Color
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.StealthDockBackground
import com.example.naughty.ui.theme.StealthDockBorder
import com.example.naughty.ui.theme.StealthFabBlack
import com.example.naughty.ui.theme.StealthFabWhite
import com.example.naughty.ui.theme.StealthIndicatorActive
import com.example.naughty.ui.theme.StealthIndicatorInactive
import com.example.naughty.ui.theme.StealthTextMuted
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.example.naughty.ui.components.SettingsDrawerContent
import com.example.naughty.ui.components.FullscreenImageViewer
import com.example.naughty.ui.theme.BrandSubtitleStyle
import com.example.naughty.ui.theme.BrandTitleStyle
import com.example.naughty.ui.theme.SubtitleColorDark
import com.example.naughty.ui.theme.SubtitleColorLight
import com.example.naughty.ui.theme.THEME_MODE_KEY
import com.example.naughty.ui.theme.ThemeMode
import com.example.naughty.ui.theme.themeDataStore
import com.example.naughty.ui.theme.themePreferenceFlow
import com.example.naughty.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (String) -> Unit,
    onOpenTimeline: () -> Unit = {},
    onTriggerSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val deletedCount by viewModel.deletedCount.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val trackerCount by viewModel.trackerCount.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val isDark = isAppInDarkTheme()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    var actionNote by remember { mutableStateOf<com.example.naughty.data.local.NoteMetadata?>(null) }
    var showNoSecurityDialog by remember { mutableStateOf(false) }

    val currentTheme by context.themePreferenceFlow().collectAsState(initial = ThemeMode.LIGHT)


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
                    val imageFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        imageFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.createNote(
                        title = "Photo note",
                        content = "![image](file://${imageFile.absolutePath})\n\n",
                        cardType = "modular"
                    ) { newNoteId ->
                        onNoteClick(newNoteId)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawerContent(
                currentTheme = currentTheme,
                notesCount = activeCount,
                boundAppsCount = uiState.boundApps.values.flatten().distinct().size,
                archivedCount = archivedCount,
                deletedCount = deletedCount,
                archivedNotes = archivedNotes,
                deletedNotes = deletedNotes,
                timelineCount = trackerCount,
                onThemeSelected = { newTheme ->
                    coroutineScope.launch {
                        context.themeDataStore.edit { preferences ->
                            preferences[THEME_MODE_KEY] = newTheme.name
                        }
                    }
                },
                onOpenTimeline = onOpenTimeline,
                onNoteClick = { noteId ->
                    onNoteClick(noteId)
                },
                onRestoreNote = { id -> viewModel.restoreNote(id) },
                onUnarchiveNote = { id -> viewModel.unarchiveNote(id) },
                onPermanentlyDeleteNote = { id -> viewModel.permanentlyDeleteNote(id) },
                onEmptyTrash = { viewModel.emptyTrash() },
                onPermanentlyDeleteAllArchived = { viewModel.permanentlyDeleteAllArchived() },
                onClearAllNotes = {
                    viewModel.clearAllNotes()
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
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
                // Top Bar with Hamburger Menu & Naughty Brand Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { coroutineScope.launch { drawerState.open() } },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Centered Brand Title - Swipe down or tap to open universal search
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                var totalDragY = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { totalDragY = 0f },
                                    onDragEnd = {
                                        if (totalDragY > 50f) {
                                            totalDragY = 0f
                                            onTriggerSearch()
                                        }
                                    },
                                    onDragCancel = { totalDragY = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        if (dragAmount > 0) {
                                            totalDragY += dragAmount
                                            if (totalDragY > 60f) {
                                                change.consume()
                                                totalDragY = 0f
                                                onTriggerSearch()
                                            }
                                        } else if (dragAmount < -10f) {
                                            totalDragY = 0f
                                        }
                                    }
                                )
                            }
                            .clickable { onTriggerSearch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NAUGHTY",
                            style = BrandTitleStyle,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Spacer on right to balance the menu button and keep center title balanced
                    Spacer(modifier = Modifier.size(40.dp))
                }

                // Timeline Tasks Horizontal Banner Ribbon
                AnimatedVisibility(
                    visible = trackerCount > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTimeline() },
                            color = if (isDark) Color(0xFF091419) else Color(0xFFEAF5FA)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timeline,
                                        contentDescription = null,
                                        tint = if (isDark) ElectricCyan else Color(0xFF0083A0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$trackerCount ${if (trackerCount == 1) "timeline task" else "timeline tasks"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) CrystalWhite else Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "View timeline tasks",
                                    tint = if (isDark) ElectricCyan.copy(alpha = 0.75f) else Color(0xFF0083A0),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

            // Notes List
            if (uiState.notes.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No matches found" else "A quiet, clean canvas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.searchQuery.isNotBlank()) {
                            val query = uiState.searchQuery.trim()
                            val ctaColor = if (isDark) ElectricGreen else AmoledBlack
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isDark) ElectricGreen.copy(alpha = 0.15f) else AmoledBlack.copy(alpha = 0.08f),
                                border = BorderStroke(0.6.dp, if (isDark) ElectricGreen.copy(alpha = 0.4f) else AmoledBlack.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        viewModel.onSearchQueryChange("")
                                        viewModel.createNote(
                                            title = query,
                                            content = "",
                                            cardType = "modular"
                                        ) { onNoteClick(it) }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = ctaColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Create note \"$query\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ctaColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Jot anything below to begin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    items(
                        items = uiState.notes,
                        key = { it.id }
                    ) { note ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            NoteCard(
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
                                },
                                onContentChange = { updatedContent ->
                                    viewModel.updateNoteContent(note.id, updatedContent)
                                },
                                onTogglePin = { viewModel.togglePin(note.id) },
                                onImageClick = { imageUri -> fullscreenImageUri = imageUri }
                            )
                        }
                    }

                    // Bottom padding to clear floating capsule and dots
                    item {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }
        }

        // Bottom Controls: Pagination Dots + Floating Pill Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4 Pagination Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                for (i in 0 until 4) {
                    val isSelected = uiState.selectedPageIndex == i
                    val dotColor = if (isSelected) {
                        if (isDark) ElectricGreen else AmoledBlack
                    } else {
                        if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                    }
                    val targetWidth = if (isSelected) 22.dp else 4.dp
                    val animatedWidth by animateDpAsState(
                        targetValue = targetWidth,
                        animationSpec = tween(durationMillis = 200),
                        label = "dotWidth"
                    )

                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(animatedWidth)
                            .clip(RoundedCornerShape(2.dp))
                            .background(dotColor)
                            .clickable { viewModel.onSelectPage(i) }
                    )
                }
            }

            // Floating Capsule Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = if (isDark) AmoledBlack else CrystalWhite,
                border = BorderStroke(
                    width = 0.6.dp,
                    color = if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search or Jot",
                        tint = if (isDark) ElectricGreen else AmoledBlack,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onTriggerSearch() }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isDark) CrystalWhite else AmoledBlack
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isDark) ElectricGreen else AmoledBlack),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    val query = uiState.searchQuery.trim()
                                    viewModel.onSearchQueryChange("")
                                    viewModel.createNote(
                                        title = query,
                                        content = "",
                                        cardType = "modular"
                                    ) { newId ->
                                        onNoteClick(newId)
                                    }
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Jot anything...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onSearchQueryChange("")
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Right Media Action Icons: Photo camera, Checklist, Popping FAB plus button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Attach image",
                                tint = if (isDark) CrystalWhite.copy(alpha = 0.85f) else AmoledBlack.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val query = uiState.searchQuery.trim()
                                if (query.isNotBlank()) {
                                    viewModel.onSearchQueryChange("")
                                }
                                viewModel.createNote(
                                    title = if (query.isNotBlank()) query else "",
                                    content = "- [ ] ",
                                    cardType = "checklist"
                                ) { onNoteClick(it) }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckBox,
                                contentDescription = "New checklist",
                                tint = if (isDark) CrystalWhite.copy(alpha = 0.85f) else AmoledBlack.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (isDark) ElectricGreen else AmoledBlack,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        val query = uiState.searchQuery.trim()
                                        if (query.isNotBlank()) {
                                            viewModel.onSearchQueryChange("")
                                            viewModel.createNote(
                                                title = query,
                                                content = "",
                                                cardType = "modular"
                                            ) { onNoteClick(it) }
                                        } else {
                                            viewModel.createNote(
                                                title = "",
                                                content = "",
                                                cardType = "modular"
                                            ) { onNoteClick(it) }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Add note",
                                    tint = if (isDark) AmoledBlack else CrystalWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (fullscreenImageUri != null) {
        FullscreenImageViewer(
            imageUri = fullscreenImageUri!!,
            onDismiss = { fullscreenImageUri = null }
        )
    }

    if (actionNote != null) {
        val targetNote = actionNote!!
        ModalBottomSheet(
            onDismissRequest = { actionNote = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = if (isDark) AmoledBlack else CrystalWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = targetNote.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CrystalWhite else AmoledBlack,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lock / Unlock Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
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
                                                subtitle = "Authenticate to secure this note with system lock",
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
                                        subtitle = "Authenticate to remove lock from this note",
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
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (targetNote.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = ElectricAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (targetNote.isLocked) "Unlock note (remove lock)" else "Lock note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                }

                // Pin / Unpin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.togglePin(targetNote.id)
                            actionNote = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = if (targetNote.isPinned) ElectricGreen else (if (isDark) CrystalWhite.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (targetNote.isPinned) "Unpin note" else "Pin note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                }

                // Archive
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.archiveNote(targetNote.id)
                            actionNote = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = if (isDark) CrystalWhite.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Archive note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                }

                // Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.deleteNote(targetNote.id)
                            actionNote = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = ElectricPink,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ElectricPink
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

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
}
}
