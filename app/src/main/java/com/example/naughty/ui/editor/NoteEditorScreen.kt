package com.example.naughty.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import com.example.naughty.ui.theme.NoteTheme
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import com.example.naughty.util.BiometricAuthHelper
import com.example.naughty.util.BiometricStatus
import com.example.naughty.util.NoteLockSession
import com.example.naughty.util.findFragmentActivity
import android.widget.Toast
import com.example.naughty.util.ImageTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.notelist.AttachedImageThumbnail
import com.example.naughty.ui.theme.BrandSubtitleStyle
import com.example.naughty.ui.theme.BrandTitleStyle
import com.example.naughty.ui.theme.SubtitleColorDark
import com.example.naughty.ui.theme.SubtitleColorLight
import com.example.naughty.ui.theme.getNoteColorPalette
import com.example.naughty.ui.theme.NoteThemeRegistry
import com.example.naughty.ui.components.paperBackground
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.ui.components.FullscreenImageViewer
import com.example.naughty.ui.editor.ThemeSelectionSheet
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import com.example.naughty.ui.components.VoiceNotePlayer
import com.example.naughty.ui.components.VoiceRecordingSheet
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.ui.editor.InlineReminder
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.editor.ReminderBottomSheet
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.io.File

private data class SelectedReminderContext(
    val selectedText: String,
    val range: TextRange,
    val isRawMode: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    noteId: String,
    onBack: () -> Unit,
    onBindingClick: () -> Unit,
    hasNext: Boolean = false,
    hasPrevious: Boolean = false,
    onNavigateNext: (() -> Unit)? = null,
    onNavigatePrevious: (() -> Unit)? = null,
    onTriggerSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var isUnlockedInSession by remember(noteId) { mutableStateOf(NoteLockSession.isUnlocked(noteId)) }
    var showNoSecurityDialog by remember { mutableStateOf(false) }

    DisposableEffect(noteId) {
        onDispose {
            NoteLockSession.lock(noteId)
        }
    }

    LaunchedEffect(uiState.isLocked, isUnlockedInSession, noteId) {
        if (uiState.isLocked && !isUnlockedInSession && activity != null) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Unlock Note",
                subtitle = "Authenticate to view and edit this note",
                onSuccess = {
                    NoteLockSession.unlock(noteId)
                    isUnlockedInSession = true
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAddBlockMenu by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var reminderTargetBlockId by remember { mutableStateOf<String?>(null) }
    var activeSelectionContext by remember { mutableStateOf<SelectedReminderContext?>(null) }
    var lastSelectionContext by remember { mutableStateOf<SelectedReminderContext?>(null) }
    var reminderInitialText by remember { mutableStateOf("") }
    var reminderInitialTime by remember { mutableStateOf<Long?>(null) }
    var isEditingNoteReminder by remember { mutableStateOf(false) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var voiceSheetTab by remember { mutableIntStateOf(0) }

    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var lastFocusedBlockId by remember { mutableStateOf<String?>(null) }
    var focusedCursorOffset by remember { mutableIntStateOf(0) }
    var requestFocusBlockId by remember { mutableStateOf<String?>(null) }

    var rawTextFieldValue by remember { mutableStateOf(TextFieldValue(uiState.content)) }
    LaunchedEffect(uiState.content) {
        if (rawTextFieldValue.text != uiState.content) {
            val s = rawTextFieldValue.selection.start.coerceAtMost(uiState.content.length)
            val e = rawTextFieldValue.selection.end.coerceAtMost(uiState.content.length)
            rawTextFieldValue = rawTextFieldValue.copy(
                text = uiState.content,
                selection = TextRange(s, e)
            )
        }
    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
                val imageFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.addImage("file://${imageFile.absolutePath}")
            } catch (_: Exception) {}
        }
    }

    var isExtractingText by remember { mutableStateOf(false) }

    val ocrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtractingText = true
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    ImageTextExtractor.extractText(context, uri)
                }
                isExtractingText = false
                result.onSuccess { extractedText ->
                    if (extractedText.isBlank()) {
                        Toast.makeText(context, "No text detected in image", Toast.LENGTH_SHORT).show()
                    } else {
                        val targetId = focusedBlockId ?: lastFocusedBlockId
                        viewModel.insertExtractedText(extractedText, targetId)
                        Toast.makeText(context, "Text attached to note", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(context, "Failed to extract text from image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveNow() }
    }

    val palette = getNoteColorPalette(uiState.colorTheme, isDark)
    val currentTheme = remember(uiState.colorTheme, isDark) {
        NoteThemeRegistry.getTheme(uiState.colorTheme, isDark)
    }

    val reminderVisualTransformation = remember(currentTheme.accent) {
        VisualTransformation { text ->
            val annotatedString = buildAnnotatedString {
                append(text.text)
                val regex = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")
                for (match in regex.findAll(text.text)) {
                    addStyle(
                        style = SpanStyle(
                            color = currentTheme.accent,
                            fontWeight = FontWeight.Bold,
                            background = currentTheme.accent.copy(alpha = 0.14f)
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
            TransformedText(annotatedString, OffsetMapping.Identity)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.canvasBackground)
            .pointerInput(noteId, hasNext, hasPrevious) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var isHorizontalSwipe = false
                    var isVerticalScroll = false
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (!change.pressed) {
                            if (isHorizontalSwipe) {
                                val minSwipeDistance = touchSlop * 2.2f
                                if (totalX < -minSwipeDistance && hasNext) {
                                    viewModel.saveNow()
                                    onNavigateNext?.invoke()
                                } else if (totalX > minSwipeDistance && hasPrevious) {
                                    viewModel.saveNow()
                                    onNavigatePrevious?.invoke()
                                }
                            }
                            break
                        }

                        val dragX = change.position.x - change.previousPosition.x
                        val dragY = change.position.y - change.previousPosition.y
                        totalX += dragX
                        totalY += dragY

                        val absX = kotlin.math.abs(totalX)
                        val absY = kotlin.math.abs(totalY)

                        if (!isHorizontalSwipe && !isVerticalScroll) {
                            if (absY > touchSlop && absY > absX) {
                                isVerticalScroll = true
                            } else if (absX > touchSlop * 1.2f && absX > absY * 1.3f) {
                                isHorizontalSwipe = true
                            }
                        }

                        if (isHorizontalSwipe) {
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.saveNow()
                        onBack()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = currentTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

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
                        color = currentTheme.textPrimary
                    )
                }

                // Right Actions: App Binding (🔗), Share, Palette, More
                if (!uiState.isLocked || isUnlockedInSession) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isLocked) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Locked Note",
                                    tint = ElectricAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onBindingClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = "Bind App",
                                tint = currentTheme.textPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        var showShareMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(
                                onClick = { showShareMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    tint = currentTheme.textPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false },
                                modifier = Modifier.background(if (isDark) AmoledBlack else CrystalWhite)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share as text") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.shareNoteAsText(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share as Markdown (.md)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Article,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.shareNoteAsFile(context)
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = { showThemeSheet = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = "Color Theme",
                                tint = currentTheme.textPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More",
                                    tint = currentTheme.textPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (isDark) AmoledBlack else CrystalWhite)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isPinned) "Unpin note" else "Pin note") },
                                    onClick = {
                                        viewModel.togglePin()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isLocked) "Unlock note (remove lock)" else "Lock note") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (uiState.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (uiState.isLocked) {
                                            activity?.let { act ->
                                                BiometricAuthHelper.authenticate(
                                                    activity = act,
                                                    title = "Unlock Note",
                                                    subtitle = "Authenticate to remove lock from this note",
                                                    onSuccess = {
                                                        viewModel.setNoteLocked(false)
                                                        Toast.makeText(context, "Note unlocked", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { errMsg ->
                                                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        } else {
                                            val status = BiometricAuthHelper.canAuthenticate(context)
                                            when (status) {
                                                BiometricStatus.AVAILABLE -> {
                                                    activity?.let { act ->
                                                        BiometricAuthHelper.authenticate(
                                                            activity = act,
                                                            title = "Lock Note",
                                                            subtitle = "Authenticate to secure this note with system lock",
                                                            onSuccess = {
                                                                viewModel.setNoteLocked(true)
                                                                NoteLockSession.unlock(noteId)
                                                                isUnlockedInSession = true
                                                                Toast.makeText(context, "Note locked", Toast.LENGTH_SHORT).show()
                                                            },
                                                            onError = { errMsg ->
                                                                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                                BiometricStatus.NOT_ENROLLED -> {
                                                    showNoSecurityDialog = true
                                                }
                                                else -> {
                                                    Toast.makeText(context, "Screen lock or biometric is unavailable on this device.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bind to application") },
                                    onClick = {
                                        showMenu = false
                                        onBindingClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share as text") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.shareNoteAsText(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share as Markdown (.md)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Article,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.shareNoteAsFile(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isRawMarkdownMode) "Card View" else "Raw Markdown") },
                                    onClick = {
                                        viewModel.toggleRawMarkdownMode()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Archive note") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.archiveNote { onBack() }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete note", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.deleteNote { onBack() }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            if (uiState.isLocked && !isUnlockedInSession) {
                LockedNoteBarrier(
                    title = uiState.title.ifBlank { "Locked note" },
                    currentTheme = currentTheme,
                    isDark = isDark,
                    onUnlockClick = {
                        activity?.let { act ->
                            BiometricAuthHelper.authenticate(
                                activity = act,
                                title = "Unlock Note",
                                subtitle = "Authenticate to view and edit this note",
                                onSuccess = {
                                    NoteLockSession.unlock(noteId)
                                    isUnlockedInSession = true
                                },
                                onError = { errMsg ->
                                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            } else {
                // Scrollable Content Canvas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                if (uiState.isRawMarkdownMode) {
                    // Raw Markdown Edit mode
                    BasicTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        textStyle = TextStyle(
                            fontFamily = currentTheme.titleFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            color = currentTheme.textPrimary
                        ),
                        cursorBrush = SolidColor(currentTheme.cursorColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (uiState.title.isEmpty()) {
                                Text(
                                    "Headline",
                                    fontFamily = currentTheme.titleFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp,
                                    color = currentTheme.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BasicTextField(
                        value = rawTextFieldValue,
                        onValueChange = { newValue ->
                            rawTextFieldValue = newValue
                            if (!newValue.selection.collapsed) {
                                val sel = newValue.text.substring(newValue.selection.min, newValue.selection.max).trim()
                                if (sel.isNotBlank()) {
                                    lastSelectionContext = SelectedReminderContext(sel, newValue.selection, isRawMode = true)
                                }
                            }
                            if (newValue.text != uiState.content) {
                                viewModel.updateContent(newValue.text)
                            }
                        },
                        visualTransformation = reminderVisualTransformation,
                        textStyle = TextStyle(
                            fontFamily = currentTheme.fontFamily,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = currentTheme.textPrimary
                        ),
                        cursorBrush = SolidColor(currentTheme.cursorColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                } else {
                    // Aesthetic Modular Card View (Anti-note style)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = currentTheme.surface,
                        border = BorderStroke(1.dp, currentTheme.border)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paperBackground(
                                    paperPattern = currentTheme.paperPattern,
                                    lineColor = currentTheme.gridLineColor
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                // Title with watercolor highlight
                                BasicTextField(
                                    value = uiState.title,
                                    onValueChange = { viewModel.updateTitle(it) },
                                    textStyle = TextStyle(
                                        fontFamily = currentTheme.titleFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 22.sp,
                                        lineHeight = 28.sp,
                                        color = currentTheme.textPrimary
                                    ),
                                    cursorBrush = SolidColor(currentTheme.cursorColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        if (uiState.title.isEmpty()) {
                                            Text(
                                                text = "Headline",
                                                fontFamily = currentTheme.titleFontFamily,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = currentTheme.textSecondary.copy(alpha = 0.45f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                // Bound Apps Section & Reminder Chip: displayed neatly underneath title
                                val boundApps = uiState.boundApps
                                val hasReminder = uiState.reminderTime != null && uiState.reminderTime!! > System.currentTimeMillis()
                                if (boundApps.isNotEmpty() || hasReminder) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (boundApps.isNotEmpty()) {
                                            boundApps.forEach { appName ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = ElectricCyan.copy(alpha = 0.12f),
                                                    border = BorderStroke(0.6.dp, ElectricCyan.copy(alpha = 0.35f)),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { onBindingClick() }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Link,
                                                            contentDescription = null,
                                                            tint = ElectricCyan,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = appName,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = ElectricCyan
                                                        )
                                                    }
                                                }
                                            }
                                            // Additional + chip to add more bindings
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.Transparent,
                                                border = BorderStroke(0.6.dp, ElectricCyan.copy(alpha = 0.35f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { onBindingClick() }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("+", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // Note-level reminder badge chip
                                        if (hasReminder) {
                                            val noteReminder = remember(uiState.reminderTime) {
                                                InlineReminder(
                                                    rawTag = "",
                                                    displayText = InlineReminderParser.formatTimestamp(uiState.reminderTime!!),
                                                    timestampMillis = uiState.reminderTime!!,
                                                    startIndex = 0,
                                                    endIndex = 0
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = currentTheme.accent.copy(alpha = 0.14f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        activeSelectionContext = null
                                                        reminderTargetBlockId = null
                                                        reminderInitialText = uiState.title.ifBlank { "" }
                                                        reminderInitialTime = uiState.reminderTime
                                                        isEditingNoteReminder = true
                                                        showReminderSheet = true
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("🔔", fontSize = 11.5.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = noteReminder.displayText,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = currentTheme.accent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Unified Document Blocks Canvas
                            val blocks = uiState.blocks
                            val showEmptyPlaceholder = blocks.size == 1 && (blocks[0] as? EditorBlock.Text)?.text.isNullOrEmpty()

                            blocks.forEachIndexed { index, block ->
                                key(block.id) {
                                    when (block) {
                                        is EditorBlock.Checklist -> {
                                            ChecklistItemRow(
                                                block = block,
                                                currentTheme = currentTheme,
                                                isRequestedFocus = requestFocusBlockId == block.id,
                                                onFocusConsumed = { requestFocusBlockId = null },
                                                onFocusChanged = { isFocused ->
                                                    if (isFocused) {
                                                        focusedBlockId = block.id
                                                        lastFocusedBlockId = block.id
                                                    } else if (focusedBlockId == block.id) {
                                                        focusedBlockId = null
                                                    }
                                                },
                                                onCheckedChange = { viewModel.toggleChecklistBlock(block.id) },
                                                onTextChange = { newText ->
                                                    viewModel.updateChecklistText(block.id, newText)
                                                },
                                                onNext = { currentText ->
                                                    val newId = viewModel.handleEnterOnChecklist(block.id, currentText)
                                                    requestFocusBlockId = newId
                                                },
                                                onBackspaceOnEmpty = {
                                                    val targetFocusId = viewModel.deleteBlock(block.id)
                                                    if (targetFocusId != null) {
                                                        requestFocusBlockId = targetFocusId
                                                    }
                                                },
                                                onDelete = {
                                                    val targetFocusId = viewModel.deleteBlock(block.id)
                                                    if (targetFocusId != null) {
                                                        requestFocusBlockId = targetFocusId
                                                    }
                                                },
                                                onReminderClick = {
                                                    activeSelectionContext = null
                                                    reminderTargetBlockId = block.id
                                                    reminderInitialText = block.text
                                                    reminderInitialTime = block.reminder?.timestampMillis
                                                    isEditingNoteReminder = false
                                                    showReminderSheet = true
                                                }
                                            )
                                        }
                                        is EditorBlock.Text -> {
                                            TextBlockItem(
                                                block = block,
                                                currentTheme = currentTheme,
                                                showPlaceholder = showEmptyPlaceholder,
                                                isRequestedFocus = requestFocusBlockId == block.id,
                                                onFocusConsumed = { requestFocusBlockId = null },
                                                onFocusChanged = { isFocused, cursorOffset ->
                                                    if (isFocused) {
                                                        focusedBlockId = block.id
                                                        lastFocusedBlockId = block.id
                                                        focusedCursorOffset = cursorOffset
                                                    } else if (focusedBlockId == block.id) {
                                                        focusedBlockId = null
                                                    }
                                                },
                                                onTextChange = { newText ->
                                                    viewModel.updateTextBlock(block.id, newText)
                                                },
                                                onReminderClick = { reminder ->
                                                    activeSelectionContext = null
                                                    reminderTargetBlockId = null
                                                    reminderInitialText = reminder.displayText
                                                    reminderInitialTime = reminder.timestampMillis
                                                    isEditingNoteReminder = false
                                                    showReminderSheet = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (blocks.any { it is EditorBlock.Checklist }) {
                                Text(
                                    text = "+ Add checklist item",
                                    fontFamily = currentTheme.fontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = currentTheme.accent,
                                    modifier = Modifier
                                        .clickable {
                                            val lastChecklist = blocks.lastOrNull { it is EditorBlock.Checklist }
                                            val newId = viewModel.insertChecklistAfter(lastChecklist?.id ?: blocks.last().id)
                                            requestFocusBlockId = newId
                                        }
                                        .padding(top = 6.dp, bottom = 4.dp)
                                )
                            }
                            }
                        }
                    }

                    // Attached Images Section
                    val attachedImages = remember(uiState.content) {
                        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
                        regex.findAll(uiState.content).map { it.groupValues[1] }.toList()
                    }

                    attachedImages.forEach { imageUri ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { fullscreenImageUri = imageUri }
                        ) {
                            AttachedImageThumbnail(
                                imageRef = imageUri,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(imageUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Attached Voice Notes Section (WhatsApp-style Opus player)
                    val attachedVoiceNotes = remember(uiState.content) {
                        EditorBlockParser.extractVoiceNotes(uiState.content)
                    }

                    attachedVoiceNotes.forEach { audioUri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        VoiceNotePlayer(
                            audioUriString = audioUri,
                            accentColor = currentTheme.accent,
                            onDelete = { viewModel.removeVoiceNote(audioUri) },
                            onInsertTranscript = { transcript ->
                                viewModel.insertExtractedText(transcript, focusedBlockId ?: lastFocusedBlockId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Space for floating editor toolbar
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }

        // Floating Bottom Editor Toolbar
        if (!uiState.isLocked || isUnlockedInSession) {
            Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                .height(54.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(27.dp)),
            shape = RoundedCornerShape(27.dp),
            color = if (isDark) AmoledBlack else CrystalWhite,
            border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // (+) Add block button
                Box {
                    IconButton(
                        onClick = { showAddBlockMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add block",
                            tint = if (isDark) ElectricGreen else AmoledBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAddBlockMenu,
                        onDismissRequest = { showAddBlockMenu = false },
                        modifier = Modifier.background(if (isDark) AmoledBlack else CrystalWhite)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Checklist Item") },
                            leadingIcon = { Icon(Icons.Outlined.CheckBox, null) },
                            onClick = {
                                val targetId = focusedBlockId ?: lastFocusedBlockId
                                val newId = viewModel.addChecklistItemAtCursor(targetId, focusedCursorOffset)
                                requestFocusBlockId = newId
                                showAddBlockMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bullet Point") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, null) },
                            onClick = {
                                viewModel.addBulletItem("New note")
                                showAddBlockMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attach Photo") },
                            leadingIcon = { Icon(Icons.Outlined.Image, null) },
                            onClick = {
                                showAddBlockMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attach Text from Photo") },
                            leadingIcon = { Icon(Icons.Outlined.DocumentScanner, null) },
                            onClick = {
                                showAddBlockMenu = false
                                ocrImagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Record Voice Note") },
                            leadingIcon = { Icon(Icons.Outlined.Mic, null) },
                            onClick = {
                                showAddBlockMenu = false
                                voiceSheetTab = 0
                                showVoiceSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voice to Text (Offline)") },
                            leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, null) },
                            onClick = {
                                showAddBlockMenu = false
                                voiceSheetTab = 1
                                showVoiceSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reminder (Bell)") },
                            leadingIcon = { Icon(Icons.Outlined.Notifications, null) },
                            onClick = {
                                showAddBlockMenu = false
                                val currentSel = when {
                                    uiState.isRawMarkdownMode && !rawTextFieldValue.selection.collapsed -> {
                                        val t = rawTextFieldValue.text.substring(rawTextFieldValue.selection.min, rawTextFieldValue.selection.max).trim()
                                        if (t.isNotBlank()) SelectedReminderContext(t, rawTextFieldValue.selection, isRawMode = true) else null
                                    }
                                    else -> lastSelectionContext
                                }

                                if (currentSel != null && currentSel.selectedText.isNotBlank()) {
                                    activeSelectionContext = currentSel
                                    reminderInitialText = currentSel.selectedText
                                } else {
                                    activeSelectionContext = null
                                    reminderInitialText = ""
                                }
                                reminderTargetBlockId = null
                                reminderInitialTime = null
                                isEditingNoteReminder = false
                                showReminderSheet = true
                            }
                        )
                    }
                }

                // Aa Format / Raw Markdown button
                IconButton(
                    onClick = { viewModel.toggleRawMarkdownMode() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Checkbox button
                IconButton(
                    onClick = {
                        val targetId = focusedBlockId ?: lastFocusedBlockId
                        val newId = viewModel.addChecklistItemAtCursor(targetId, focusedCursorOffset)
                        requestFocusBlockId = newId
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckBox,
                        contentDescription = "Add checkbox",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Bullet list button
                IconButton(
                    onClick = { viewModel.addBulletItem("New point") },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = "Bullet list",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Image button: launches real image picker
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Attach image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Text from image button: launches local OCR image picker
                IconButton(
                    onClick = { ocrImagePickerLauncher.launch("image/*") },
                    enabled = !isExtractingText,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isExtractingText) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = currentTheme.accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = "Attach text from image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Voice Note / Speech to Text button
                IconButton(
                    onClick = {
                        voiceSheetTab = 0
                        showVoiceSheet = true
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice note & dictation",
                        tint = currentTheme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Link button
                IconButton(
                    onClick = { viewModel.updateContent(uiState.content + "\nhttps://") },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Link",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Bell Icon (Reminder button)
                IconButton(
                    onClick = {
                        val targetId = focusedBlockId ?: lastFocusedBlockId
                        val focusedChecklist = uiState.blocks.find { it.id == targetId } as? EditorBlock.Checklist
                        if (focusedChecklist != null) {
                            activeSelectionContext = null
                            reminderTargetBlockId = focusedChecklist.id
                            reminderInitialText = focusedChecklist.text
                            reminderInitialTime = focusedChecklist.reminder?.timestampMillis
                            isEditingNoteReminder = false
                            showReminderSheet = true
                        } else {
                            activeSelectionContext = null
                            reminderTargetBlockId = null
                            reminderInitialText = uiState.title.ifBlank { "Note Reminder" }
                            reminderInitialTime = uiState.reminderTime
                            isEditingNoteReminder = true
                            showReminderSheet = true
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    if (uiState.hasActiveReminders) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Active Reminder",
                                tint = currentTheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(currentTheme.accent)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Set Reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.canUndo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = uiState.canRedo,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
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

        // Antinote Theme Selection Sheet
        if (showThemeSheet) {
            ThemeSelectionSheet(
                currentThemeId = uiState.colorTheme,
                onSelectTheme = { themeId ->
                    viewModel.updateColorTheme(themeId)
                },
                onDismiss = { showThemeSheet = false }
            )
        }

        // Reminder Bottom Sheet (Edit Menu)
        if (showReminderSheet) {
            ReminderBottomSheet(
                initialTimeMillis = reminderInitialTime ?: uiState.reminderTime,
                targetContextText = reminderInitialText,
                onDismiss = {
                    showReminderSheet = false
                    reminderTargetBlockId = null
                    activeSelectionContext = null
                    reminderInitialText = ""
                    reminderInitialTime = null
                    isEditingNoteReminder = false
                },
                onDeleteReminder = if (isEditingNoteReminder) {
                    { viewModel.cancelNoteReminder() }
                } else if (reminderTargetBlockId != null) {
                    { viewModel.removeChecklistReminder(reminderTargetBlockId!!) }
                } else null,
                onSetReminder = { timeMillis, isCalendar, reminderText ->
                    when {
                        reminderTargetBlockId != null -> {
                            viewModel.setChecklistReminder(
                                blockId = reminderTargetBlockId!!,
                                timeMillis = timeMillis,
                                isCalendar = isCalendar,
                                context = context
                            )
                        }
                        else -> {
                            viewModel.setNoteReminder(timeMillis, isCalendar, context)
                        }
                    }

                    showReminderSheet = false
                    reminderTargetBlockId = null
                    activeSelectionContext = null
                    lastSelectionContext = null
                    reminderInitialText = ""
                    reminderInitialTime = null
                    isEditingNoteReminder = false
                }
            )
        }

        // Fullscreen image viewer overlay
        if (fullscreenImageUri != null) {
            FullscreenImageViewer(
                imageUri = fullscreenImageUri!!,
                onDismiss = { fullscreenImageUri = null }
            )
        }

        // Voice Recording Bottom Sheet (Opus recording & offline speech-to-text)
        if (showVoiceSheet) {
            VoiceRecordingSheet(
                initialTab = voiceSheetTab,
                accentColor = currentTheme.accent,
                onSaveVoiceNote = { audioFile, transcript ->
                    viewModel.addVoiceNote("file://${audioFile.absolutePath}", transcript)
                },
                onInsertText = { text ->
                    viewModel.insertExtractedText(text, focusedBlockId ?: lastFocusedBlockId)
                },
                onDismiss = { showVoiceSheet = false }
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(
    block: EditorBlock.Checklist,
    currentTheme: NoteTheme,
    isRequestedFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onNext: (String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    onDelete: () -> Unit,
    onReminderClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(block.id) {
        mutableStateOf(
            TextFieldValue(
                text = block.text,
                selection = TextRange(block.text.length)
            )
        )
    }

    LaunchedEffect(block.text) {
        if (textFieldValue.text != block.text) {
            val sel = textFieldValue.selection.start.coerceAtMost(block.text.length)
            textFieldValue = textFieldValue.copy(
                text = block.text,
                selection = TextRange(sel)
            )
        }
    }

    LaunchedEffect(isRequestedFocus) {
        if (isRequestedFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            onFocusConsumed()
        }
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (block.isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = if (block.isChecked) "Completed task" else "Incomplete task",
            tint = if (block.isChecked) currentTheme.textSecondary else currentTheme.accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(21.dp)
                .clickable { onCheckedChange(!block.isChecked) }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != block.text) {
                        onTextChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Backspace && event.type == KeyEventType.KeyDown) {
                            if (textFieldValue.text.isEmpty()) {
                                onBackspaceOnEmpty()
                                true
                            } else {
                                false
                            }
                        } else if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyDown) {
                            if (event.isShiftPressed) {
                                false
                            } else {
                                onNext(textFieldValue.text)
                                true
                            }
                        } else {
                            false
                        }
                    }
                    .onKeyEvent { event ->
                        if (event.key == Key.Backspace && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                            if (textFieldValue.text.isEmpty()) {
                                if (event.type == KeyEventType.KeyDown) {
                                    onBackspaceOnEmpty()
                                }
                                true
                            } else {
                                false
                            }
                        } else if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                            if (event.isShiftPressed) {
                                false
                            } else {
                                if (event.type == KeyEventType.KeyDown) {
                                    onNext(textFieldValue.text)
                                }
                                true
                            }
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontFamily = currentTheme.fontFamily,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    color = if (block.isChecked) currentTheme.textSecondary else currentTheme.textPrimary,
                    textDecoration = if (block.isChecked) TextDecoration.LineThrough else null
                ),
                cursorBrush = SolidColor(currentTheme.cursorColor),
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { onNext(textFieldValue.text) },
                    onDone = { onNext(textFieldValue.text) }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "To-do",
                                fontFamily = currentTheme.fontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = currentTheme.textSecondary.copy(alpha = 0.45f),
                                fontSize = 17.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (block.reminder != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = currentTheme.accent.copy(alpha = 0.14f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onReminderClick?.invoke() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text("🔔", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = block.reminder.displayText,
                            fontFamily = currentTheme.fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = currentTheme.accent
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 1.dp)
        ) {
            if (block.reminder == null) {
                IconButton(
                    onClick = { onReminderClick?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Add reminder to task",
                        tint = currentTheme.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Delete task",
                    tint = currentTheme.textSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TextBlockItem(
    block: EditorBlock.Text,
    currentTheme: NoteTheme,
    showPlaceholder: Boolean,
    isRequestedFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onFocusChanged: (Boolean, Int) -> Unit,
    onTextChange: (String) -> Unit,
    onReminderClick: ((InlineReminder) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember(block.id) {
        mutableStateOf(
            TextFieldValue(
                text = block.text,
                selection = TextRange(block.text.length)
            )
        )
    }

    LaunchedEffect(block.text) {
        if (textFieldValue.text != block.text) {
            val sel = textFieldValue.selection.start.coerceAtMost(block.text.length)
            textFieldValue = textFieldValue.copy(
                text = block.text,
                selection = TextRange(sel)
            )
        }
    }

    LaunchedEffect(isRequestedFocus) {
        if (isRequestedFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            onFocusConsumed()
        }
    }

    val reminders = remember(block.text) {
        InlineReminderParser.parseReminders(block.text)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (reminders.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                reminders.forEach { reminder ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = currentTheme.accent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReminderClick?.invoke(reminder) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Text("🔔", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = reminder.displayText,
                                fontFamily = currentTheme.fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = currentTheme.accent
                            )
                        }
                    }
                }
            }
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onFocusChanged(true, newValue.selection.start)
                if (newValue.text != block.text) {
                    onTextChange(newValue.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused, textFieldValue.selection.start)
                }
                .padding(vertical = 4.dp),
            textStyle = TextStyle(
                fontFamily = currentTheme.fontFamily,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = currentTheme.textPrimary
            ),
            cursorBrush = SolidColor(currentTheme.cursorColor),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty() && showPlaceholder) {
                    Text(
                        text = "Start writing or use the toolbar below...",
                        fontFamily = currentTheme.fontFamily,
                        fontSize = 14.5.sp,
                        color = currentTheme.textSecondary.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun LockedNoteBarrier(
    title: String,
    currentTheme: NoteTheme,
    isDark: Boolean,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDark) AmoledBlack else currentTheme.border.copy(alpha = 0.5f),
                border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else currentTheme.border),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = ElectricAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = currentTheme.textPrimary,
                fontSize = 20.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This note is secured with system lock",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color(0xFF888888) else currentTheme.textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onUnlockClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) ElectricGreen else AmoledBlack,
                    contentColor = if (isDark) AmoledBlack else CrystalWhite
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock Note",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


