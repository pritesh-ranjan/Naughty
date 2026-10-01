package com.example.naughty

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.naughty.ui.components.UniversalSearchOverlay
import com.example.naughty.ui.binding.BindingSheet
import com.example.naughty.ui.components.PermissionGate
import com.example.naughty.ui.editor.NoteEditorScreen
import com.example.naughty.ui.navigation.NoteEditorKey
import com.example.naughty.ui.navigation.NoteListKey
import com.example.naughty.ui.notelist.NoteListScreen
import com.example.naughty.ui.theme.NaughtyTheme

class MainActivity : ComponentActivity() {

    private var pendingNoteId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingNoteId = intent?.getStringExtra("noteId")

        setContent {
            NaughtyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate {
                        NaughtyNavigation(
                            initialNoteId = pendingNoteId,
                            onNoteOpened = { pendingNoteId = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("noteId")?.let { noteId ->
            pendingNoteId = noteId
        }
    }
}

@Composable
fun NaughtyNavigation(
    initialNoteId: String? = null,
    onNoteOpened: () -> Unit = {}
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as NaughtyApp
    val container = app.container
    val backStack = rememberNavBackStack(NoteListKey)

    // Observe active notes and active bindings for search and swiping
    val activeNotes by container.noteRepository.getAllActive().collectAsState(initial = emptyList())
    val activeBindings by container.bindingRepository.getAllActive().collectAsState(initial = emptyList())
    var noteContents by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Keep a stable browsing order for swiping in editor so auto-saving current note doesn't reshuffle the list
    var browsingNoteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val isEditingNote = backStack.lastOrNull() is NoteEditorKey
    LaunchedEffect(activeNotes, isEditingNote) {
        if (!isEditingNote || browsingNoteIds.isEmpty()) {
            browsingNoteIds = activeNotes.map { it.id }
        } else {
            val activeIdsSet = activeNotes.map { it.id }.toSet()
            val filtered = browsingNoteIds.filter { it in activeIdsSet }.toMutableList()
            for (note in activeNotes) {
                if (note.id !in filtered) {
                    filtered.add(0, note.id)
                }
            }
            browsingNoteIds = filtered
        }
    }

    LaunchedEffect(activeNotes) {
        val map = mutableMapOf<String, String>()
        for (note in activeNotes) {
            map[note.id] = container.noteRepository.getNoteContent(note.id)
        }
        noteContents = map
    }

    val boundAppsMap = remember(activeBindings) {
        activeBindings.associate { it.noteId to it.appLabel }
    }

    // Universal pull-down search overlay state
    var showUniversalSearch by remember { mutableStateOf(false) }

    // Handle opening note from notification
    LaunchedEffect(initialNoteId) {
        if (!initialNoteId.isNullOrBlank()) {
            if (backStack.lastOrNull() != NoteEditorKey(initialNoteId)) {
                backStack.add(NoteEditorKey(initialNoteId))
            }
            onNoteOpened()
        }
    }

    // Binding sheet state
    var showBindingSheet by remember { mutableStateOf(false) }
    var bindingNoteId by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {
                entry<NoteListKey> {
                    val viewModel = remember { container.noteListViewModel() }
                    NoteListScreen(
                        viewModel = viewModel,
                        onNoteClick = { noteId ->
                            backStack.add(NoteEditorKey(noteId))
                        },
                        onTriggerSearch = {
                            showUniversalSearch = true
                        }
                    )
                }

                entry<NoteEditorKey> { key ->
                    val viewModel = remember(key.noteId) { container.noteEditorViewModel() }
                    val currentIndex = browsingNoteIds.indexOf(key.noteId)
                    val hasNext = currentIndex in 0 until (browsingNoteIds.size - 1)
                    val hasPrevious = currentIndex > 0
                    NoteEditorScreen(
                        viewModel = viewModel,
                        noteId = key.noteId,
                        hasNext = hasNext,
                        hasPrevious = hasPrevious,
                        onNavigateNext = if (hasNext) {
                            {
                                val nextId = browsingNoteIds[currentIndex + 1]
                                if (backStack.isNotEmpty() && backStack.last() is NoteEditorKey) {
                                    backStack[backStack.lastIndex] = NoteEditorKey(nextId)
                                } else {
                                    backStack.add(NoteEditorKey(nextId))
                                }
                            }
                        } else null,
                        onNavigatePrevious = if (hasPrevious) {
                            {
                                val prevId = browsingNoteIds[currentIndex - 1]
                                if (backStack.isNotEmpty() && backStack.last() is NoteEditorKey) {
                                    backStack[backStack.lastIndex] = NoteEditorKey(prevId)
                                } else {
                                    backStack.add(NoteEditorKey(prevId))
                                }
                            }
                        } else null,
                        onTriggerSearch = {
                            showUniversalSearch = true
                        },
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        onBindingClick = {
                            bindingNoteId = key.noteId
                            showBindingSheet = true
                        }
                    )
                }
            }
        )

        // Binding sheet overlay
        if (showBindingSheet && bindingNoteId.isNotBlank()) {
            val bindingViewModel = remember(bindingNoteId) { container.bindingViewModel() }
            BindingSheet(
                viewModel = bindingViewModel,
                noteId = bindingNoteId,
                onDismiss = { showBindingSheet = false }
            )
        }

        // Universal pull-down search overlay (available across home and notes)
        UniversalSearchOverlay(
            isOpen = showUniversalSearch,
            notes = activeNotes,
            rawContents = noteContents,
            boundApps = boundAppsMap,
            onNoteClick = { targetNoteId ->
                val currentKey = backStack.lastOrNull()
                if (currentKey is NoteEditorKey) {
                    if (currentKey.noteId != targetNoteId) {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(NoteEditorKey(targetNoteId))
                    }
                } else {
                    backStack.add(NoteEditorKey(targetNoteId))
                }
                showUniversalSearch = false
            },
            onDismiss = { showUniversalSearch = false }
        )
    }
}
