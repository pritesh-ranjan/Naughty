package com.example.naughty

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.naughty.binding.DetectorForegroundService
import com.example.naughty.ui.binding.BindingSheet
import com.example.naughty.ui.components.FeatureShowcaseSheet
import com.example.naughty.ui.components.PermissionGate
import com.example.naughty.ui.components.ShareAudioOptionSheet
import com.example.naughty.ui.components.ShareImageOptionSheet
import com.example.naughty.ui.components.UniversalSearchOverlay
import com.example.naughty.ui.components.shouldShowFeatureShowcase
import com.example.naughty.ui.editor.NoteEditorScreen
import com.example.naughty.ui.navigation.NoteEditorKey
import com.example.naughty.ui.navigation.NoteListKey
import com.example.naughty.ui.navigation.TimelineDetailKey
import com.example.naughty.ui.navigation.TimelineListKey
import com.example.naughty.ui.notelist.NoteListScreen
import com.example.naughty.ui.theme.LAST_SEEN_VERSION_CODE_KEY
import com.example.naughty.ui.theme.NaughtyTheme
import com.example.naughty.ui.theme.getAppVersionCode
import com.example.naughty.ui.theme.getAppVersionName
import com.example.naughty.ui.theme.setLastSeenVersionCode
import com.example.naughty.ui.theme.themeDataStore
import com.example.naughty.ui.timeline.TimelineDetailScreen
import com.example.naughty.ui.timeline.TimelineListScreen
import com.example.naughty.util.AudioShareMode
import com.example.naughty.util.ImageShareMode
import com.example.naughty.util.PendingAudioShare
import com.example.naughty.util.PendingImageShare
import com.example.naughty.util.ShareIntentHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : FragmentActivity() {

    private var pendingNoteId by mutableStateOf<String?>(null)
    private var pendingTrackerId by mutableStateOf<String?>(null)
    private var pendingOpenTimeline by mutableStateOf(false)
    private var isBypassed by mutableStateOf(false)
    private var pendingImageShare by mutableStateOf<PendingImageShare?>(null)
    private var isProcessingImageShare by mutableStateOf(false)
    private var processingText by mutableStateOf("")

    private var pendingAudioShare by mutableStateOf<PendingAudioShare?>(null)
    private var isProcessingAudioShare by mutableStateOf(false)
    private var audioProcessingText by mutableStateOf("")

    private var showFeatureShowcase by mutableStateOf(false)
    private var showcaseIsFirstLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val currentCode = getAppVersionCode()
            val lastSeen = themeDataStore.data.map { it[LAST_SEEN_VERSION_CODE_KEY] }.first()
            if (shouldShowFeatureShowcase(lastSeen, currentCode)) {
                showcaseIsFirstLaunch = (lastSeen == null)
                showFeatureShowcase = true
            }
        }

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        } else {
            intent?.getStringExtra("noteId")?.let {
                pendingNoteId = it
                isBypassed = true
            }
            intent?.getStringExtra("trackerId")?.let {
                pendingTrackerId = it
                isBypassed = true
            }
            if (intent?.getStringExtra("action") == "timeline_tasks") {
                pendingOpenTimeline = true
                isBypassed = true
            }
            if (intent?.getStringExtra("action") == "create_note") {
                isBypassed = true
            }
        }

        setContent {
            NaughtyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate(
                        bypass = isBypassed || pendingNoteId != null || pendingTrackerId != null || pendingImageShare != null || pendingAudioShare != null || pendingOpenTimeline
                    ) {
                        NaughtyNavigation(
                            initialNoteId = pendingNoteId,
                            initialTrackerId = pendingTrackerId,
                            initialOpenTimeline = pendingOpenTimeline,
                            onNoteOpened = { pendingNoteId = null },
                            onTrackerOpened = { pendingTrackerId = null },
                            onTimelineOpened = { pendingOpenTimeline = false }
                        )

                        pendingImageShare?.let { imageShare ->
                            val app = applicationContext as? NaughtyApp ?: return@let
                            val repository = app.container.noteRepository

                            ShareImageOptionSheet(
                                imageUris = imageShare.uris,
                                isProcessing = isProcessingImageShare,
                                processingText = processingText,
                                onSelectMode = { mode ->
                                    isProcessingImageShare = true
                                    processingText = when (mode) {
                                        ImageShareMode.IMAGE_ONLY -> "Attaching image..."
                                        ImageShareMode.OCR_ONLY -> "Extracting text with OCR..."
                                        ImageShareMode.BOTH -> "Scanning text & attaching image..."
                                    }
                                    lifecycleScope.launch {
                                        val createdNote = ShareIntentHandler.processImageShare(
                                            context = this@MainActivity,
                                            share = imageShare,
                                            mode = mode,
                                            repository = repository
                                        )
                                        withContext(Dispatchers.Main) {
                                            isProcessingImageShare = false
                                            pendingImageShare = null
                                            if (createdNote != null) {
                                                val msg = when (mode) {
                                                    ImageShareMode.OCR_ONLY -> "Text extracted via OCR"
                                                    ImageShareMode.BOTH -> "Image & OCR text note created"
                                                    ImageShareMode.IMAGE_ONLY -> "Note created"
                                                }
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                pendingNoteId = createdNote.id
                                            }
                                        }
                                    }
                                },
                                onDismiss = {
                                    if (!isProcessingImageShare) {
                                        pendingImageShare = null
                                    }
                                }
                            )
                        }

                        pendingAudioShare?.let { audioShare ->
                            val app = applicationContext as? NaughtyApp ?: return@let
                            val repository = app.container.noteRepository

                            ShareAudioOptionSheet(
                                audioUris = audioShare.uris,
                                subject = audioShare.subject,
                                text = audioShare.text,
                                isProcessing = isProcessingAudioShare,
                                processingText = audioProcessingText,
                                onSelectMode = { mode ->
                                    isProcessingAudioShare = true
                                    audioProcessingText = when (mode) {
                                        AudioShareMode.ATTACH_ONLY -> "Attaching voice note..."
                                        AudioShareMode.TRANSCRIBE_ONLY -> "Transcribing speech offline..."
                                        AudioShareMode.BOTH -> "Transcribing & attaching voice note..."
                                    }
                                    lifecycleScope.launch {
                                        val createdNote = ShareIntentHandler.processAudioShare(
                                            context = this@MainActivity,
                                            share = audioShare,
                                            mode = mode,
                                            repository = repository
                                        )
                                        withContext(Dispatchers.Main) {
                                            isProcessingAudioShare = false
                                            pendingAudioShare = null
                                            if (createdNote != null) {
                                                val msg = when (mode) {
                                                    AudioShareMode.TRANSCRIBE_ONLY -> "Speech transcribed offline"
                                                    AudioShareMode.BOTH -> "Voice note & transcript saved"
                                                    AudioShareMode.ATTACH_ONLY -> "Voice note attached"
                                                }
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                pendingNoteId = createdNote.id
                                            }
                                        }
                                    }
                                },
                                onDismiss = {
                                    if (!isProcessingAudioShare) {
                                        pendingAudioShare = null
                                    }
                                }
                            )
                        }
                    }

                    val isHandlingShare = pendingImageShare != null || pendingAudioShare != null
                    if (showFeatureShowcase && !isHandlingShare) {
                        FeatureShowcaseSheet(
                            versionName = getAppVersionName(),
                            versionCode = getAppVersionCode(),
                            isFirstLaunch = showcaseIsFirstLaunch,
                            onDismiss = {
                                showFeatureShowcase = false
                                lifecycleScope.launch {
                                    setLastSeenVersionCode(getAppVersionCode())
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure background context engine is active and immune to cached freezing
        DetectorForegroundService.start(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        intent.getStringExtra("noteId")?.let { noteId ->
            isBypassed = true
            pendingNoteId = noteId
            return
        }
        intent.getStringExtra("trackerId")?.let { trackerId ->
            isBypassed = true
            pendingTrackerId = trackerId
            return
        }

        val actionExtra = intent.getStringExtra("action")
        if (actionExtra == "create_note") {
            isBypassed = true
            val app = applicationContext as? NaughtyApp ?: return
            val repository = app.container.noteRepository
            lifecycleScope.launch {
                val createdNote = repository.createNote(
                    title = "",
                    content = "",
                    cardType = "modular"
                )
                withContext(Dispatchers.Main) {
                    pendingNoteId = createdNote.id
                }
            }
            return
        } else if (actionExtra == "todo_note") {
            isBypassed = true
            val app = applicationContext as? NaughtyApp ?: return
            val repository = app.container.noteRepository
            lifecycleScope.launch {
                val official = repository.getOrCreateOfficialTodoNote()
                withContext(Dispatchers.Main) {
                    pendingNoteId = official.metadata.id
                }
            }
            return
        } else if (actionExtra == "timeline_tasks") {
            isBypassed = true
            pendingOpenTimeline = true
            return
        }

        if (ShareIntentHandler.isShareIntent(intent) && !ShareIntentHandler.isHandled(intent)) {
            isBypassed = true
            val app = applicationContext as? NaughtyApp ?: return
            val repository = app.container.noteRepository

            val imageShare = ShareIntentHandler.extractImageShare(intent)
            if (imageShare != null) {
                ShareIntentHandler.markAsHandled(intent)
                pendingImageShare = imageShare
                return
            }

            val audioShare = ShareIntentHandler.extractAudioShare(intent)
            if (audioShare != null) {
                ShareIntentHandler.markAsHandled(intent)
                pendingAudioShare = audioShare
                return
            }

            lifecycleScope.launch {
                val createdNote = ShareIntentHandler.processShareIntent(
                    context = this@MainActivity,
                    intent = intent,
                    repository = repository
                )
                if (createdNote != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Note created", Toast.LENGTH_SHORT).show()
                        pendingNoteId = createdNote.id
                    }
                }
            }
        }
    }
}

@Composable
fun NaughtyNavigation(
    initialNoteId: String? = null,
    initialTrackerId: String? = null,
    initialOpenTimeline: Boolean = false,
    onNoteOpened: () -> Unit = {},
    onTrackerOpened: () -> Unit = {},
    onTimelineOpened: () -> Unit = {}
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as NaughtyApp
    val container = app.container
    val backStack = rememberNavBackStack(NoteListKey)

    // Observe active notes and active bindings for search and swiping
    val activeNotes by container.noteRepository.getAllActive().collectAsState(initial = emptyList())
    val activeBindings by container.bindingRepository.getAllActive().collectAsState(initial = emptyList())
    val allTrackers by container.timelineRepository.observeTrackers().collectAsState(initial = emptyList())
    val allMilestones by container.timelineRepository.observeAllMilestones().collectAsState(initial = emptyList())
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

    // Handle opening note from notification or shortcut
    LaunchedEffect(initialNoteId) {
        if (!initialNoteId.isNullOrBlank()) {
            if (backStack.lastOrNull() != NoteEditorKey(initialNoteId)) {
                backStack.add(NoteEditorKey(initialNoteId))
            }
            onNoteOpened()
        }
    }

    // Handle opening timeline tracker from notification
    LaunchedEffect(initialTrackerId) {
        if (!initialTrackerId.isNullOrBlank()) {
            if (backStack.lastOrNull() != TimelineDetailKey(initialTrackerId)) {
                backStack.add(TimelineDetailKey(initialTrackerId))
            }
            onTrackerOpened()
        }
    }

    // Handle opening timeline list from shortcut
    LaunchedEffect(initialOpenTimeline) {
        if (initialOpenTimeline) {
            if (backStack.lastOrNull() != TimelineListKey) {
                backStack.add(TimelineListKey)
            }
            onTimelineOpened()
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
            transitionSpec = {
                val enter = slideInHorizontally(
                    initialOffsetX = { (it * 0.22f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing))
                val exit = slideOutHorizontally(
                    targetOffsetX = { -(it * 0.12f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                enter.togetherWith(exit)
            },
            popTransitionSpec = {
                val enter = slideInHorizontally(
                    initialOffsetX = { -(it * 0.12f).toInt() },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
                val exit = slideOutHorizontally(
                    targetOffsetX = { (it * 0.22f).toInt() },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                enter.togetherWith(exit)
            },
            predictivePopTransitionSpec = { _ ->
                val enter = fadeIn(animationSpec = tween(durationMillis = 200))
                val exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 180))
                enter.togetherWith(exit)
            },
            entryProvider = entryProvider {
                entry<NoteListKey> {
                    val viewModel = remember { container.noteListViewModel() }
                    NoteListScreen(
                        viewModel = viewModel,
                        onNoteClick = { noteId ->
                            backStack.add(NoteEditorKey(noteId))
                        },
                        onOpenTimeline = {
                            backStack.add(TimelineListKey)
                        },
                        onOpenSettings = {
                            backStack.add(com.example.naughty.ui.navigation.SettingsKey)
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

                entry<TimelineListKey> {
                    val viewModel = remember { container.timelineListViewModel() }
                    TimelineListScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        onTrackerClick = { trackerId ->
                            backStack.add(TimelineDetailKey(trackerId))
                        }
                    )
                }

                entry<TimelineDetailKey> { key ->
                    val viewModel = remember(key.trackerId) { container.timelineDetailViewModel(key.trackerId) }
                    TimelineDetailScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        }
                    )
                }

                entry<com.example.naughty.ui.navigation.SettingsKey> {
                    val viewModel = remember { container.settingsViewModel() }
                    com.example.naughty.ui.settings.SettingsScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        onNoteClick = { targetNoteId ->
                            backStack.add(NoteEditorKey(targetNoteId))
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
            trackers = allTrackers,
            milestones = allMilestones,
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
            onTrackerClick = { targetTrackerId ->
                backStack.add(TimelineDetailKey(targetTrackerId))
                showUniversalSearch = false
            },
            onDismiss = { showUniversalSearch = false }
        )
    }
}
