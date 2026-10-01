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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.graphics.Color
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

@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (String) -> Unit,
    onTriggerSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val deletedCount by viewModel.deletedCount.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val isDark = isAppInDarkTheme()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    val currentTheme by context.themePreferenceFlow().collectAsState(initial = ThemeMode.LIGHT)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            var pullDistance = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    pullDistance += available.y
                    if (pullDistance > 45f) {
                        pullDistance = 0f
                        onTriggerSearch()
                    }
                } else if (available.y < -10f) {
                    pullDistance = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    pullDistance += available.y
                    if (pullDistance > 45f) {
                        pullDistance = 0f
                        onTriggerSearch()
                    }
                }
                return Offset.Zero
            }
        }
    }

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
                onThemeSelected = { newTheme ->
                    coroutineScope.launch {
                        context.themeDataStore.edit { preferences ->
                            preferences[THEME_MODE_KEY] = newTheme.name
                        }
                    }
                },
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .pointerInput(Unit) {
                            var totalDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDragY = 0f },
                                onDragEnd = {
                                    if (totalDragY > 35f) {
                                        totalDragY = 0f
                                        onTriggerSearch()
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0) {
                                        totalDragY += dragAmount
                                        if (totalDragY > 40f) {
                                            change.consume()
                                            totalDragY = 0f
                                            onTriggerSearch()
                                        }
                                    } else if (dragAmount < -5f) {
                                        totalDragY = 0f
                                    }
                                }
                            )
                        },
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

                    Text(
                        text = "NAUGHTY",
                        style = BrandTitleStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Spacer on right to balance the menu button and keep center title balanced
                    Spacer(modifier = Modifier.size(40.dp))
                }

            // Notes List
            if (uiState.notes.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                        .pointerInput(Unit) {
                            var totalDragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDragY = 0f },
                                onDragEnd = {
                                    if (totalDragY > 35f) onTriggerSearch()
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    totalDragY += dragAmount
                                    if (totalDragY > 40f) {
                                        change.consume()
                                        onTriggerSearch()
                                    }
                                }
                            )
                        },
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
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
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Create note \"$query\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
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
                                onClick = { onNoteClick(note.id) },
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
                        if (isDark) StealthIndicatorActive else MaterialTheme.colorScheme.onBackground
                    } else {
                        if (isDark) StealthIndicatorInactive else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    }
                    val targetWidth = if (isSelected) 20.dp else 5.dp
                    val animatedWidth by animateDpAsState(
                        targetValue = targetWidth,
                        animationSpec = tween(durationMillis = 200),
                        label = "dotWidth"
                    )

                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(animatedWidth)
                            .clip(RoundedCornerShape(2.5.dp))
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
                    .height(58.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) StealthDockBackground else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) StealthDockBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
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
                        tint = if (isDark) Color(0xFF757985) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
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
                                        color = if (isDark) Color(0xFF525560) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Right Media Action Icons: Photo camera, Checklist, White FAB plus button
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
                                tint = if (isDark) Color(0xFF8E929E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
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
                                tint = if (isDark) Color(0xFF8E929E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (isDark) StealthFabWhite else MaterialTheme.colorScheme.primary,
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
                                    tint = if (isDark) StealthFabBlack else MaterialTheme.colorScheme.onPrimary,
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
}
}
