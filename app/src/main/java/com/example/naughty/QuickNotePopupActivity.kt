package com.example.naughty

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.naughty.ui.components.ClipSaveSheet
import com.example.naughty.ui.components.FullscreenImageViewer
import com.example.naughty.ui.components.VoiceNotePlayer
import com.example.naughty.ui.components.VoiceRecordingSheet
import com.example.naughty.ui.components.paperBackground
import com.example.naughty.ui.editor.BulletTextHelper
import com.example.naughty.ui.editor.ChecklistItemRow
import com.example.naughty.ui.editor.EditorBlock
import com.example.naughty.ui.editor.EditorBlockParser
import com.example.naughty.ui.editor.InlineReminder
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.editor.NoteEditorViewModel
import com.example.naughty.ui.editor.ReminderBottomSheet
import com.example.naughty.ui.editor.TextBlockItem
import com.example.naughty.ui.editor.ThemeSelectionSheet
import com.example.naughty.ui.notelist.AttachedImageThumbnail
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.oneui.theme.OneUiTheme
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.NaughtyTheme
import com.example.naughty.ui.theme.NoteThemeRegistry
import com.example.naughty.notification.ContextNotificationManager
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.BiometricAuthHelper
import com.example.naughty.util.ImageOptimizer
import com.example.naughty.util.ImageTextExtractor
import com.example.naughty.util.NoteLockSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Miniature popup window launched via app shortcuts ("New Note" and "Todo List").
 *
 * Runs in its own isolated task (taskAffinity="", excludeFromRecents=true)
 * with a translucent floating window over the launcher or background app.
 */
class QuickNotePopupActivity : FragmentActivity() {

    private var activeNoteId by mutableStateOf<String?>(null)
    private var isNewDraftNote by mutableStateOf(false)
    private var isTodoMode by mutableStateOf(false)
    @Volatile
    private var focusDeferred = CompletableDeferred<Unit>()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !focusDeferred.isCompleted) {
            focusDeferred.complete(Unit)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val rawAction = intent.action ?: intent.getStringExtra("action")
        if (rawAction == ACTION_DIRECT_SAVE_CTS) {
            handleDirectSaveCts(intent)
        } else if (rawAction == ACTION_REVIEW_CTS) {
            handleReviewCts(intent)
        }
    }

    private fun readClipboardText(): String? {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount <= 0) return null

            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i) ?: continue

                // 1. Try coerceToText: handles text, styled text, HTML, and file/content URIs
                val coerced = try {
                    item.coerceToText(this)?.toString()?.trim()
                } catch (_: Exception) {
                    null
                }
                if (!coerced.isNullOrBlank()) return coerced

                // 2. Direct text
                val directText = item.text?.toString()?.trim()
                if (!directText.isNullOrBlank()) return directText

                // 3. HTML text
                val html = item.htmlText?.trim()
                if (!html.isNullOrBlank()) {
                    val plainFromHtml = try {
                        android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
                    } catch (_: Exception) {
                        null
                    }
                    if (!plainFromHtml.isNullOrBlank()) return plainFromHtml
                }

                // 4. URI fallback
                val uri = item.uri?.toString()?.trim()
                if (!uri.isNullOrBlank()) return uri
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun awaitClipboardText(timeoutMs: Long = 2000L): String? {
        // 1. If already focused, try reading immediately
        if (hasWindowFocus()) {
            val direct = readClipboardText()
            if (!direct.isNullOrBlank()) return direct
        }

        // 2. Wait for window focus signal or timeout
        if (!hasWindowFocus()) {
            if (focusDeferred.isCompleted) {
                focusDeferred = CompletableDeferred()
            }
            try {
                withTimeoutOrNull(timeoutMs) {
                    focusDeferred.await()
                }
            } catch (_: Exception) {}
        }

        // 3. After window focus is gained, poll briefly (up to 800ms) for clipboard data
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 800L) {
            val text = readClipboardText()
            if (!text.isNullOrBlank()) return text
            delay(50L)
        }

        return readClipboardText()
    }

    private fun handleDirectSaveCts(intent: Intent?) {
        val app = applicationContext as NaughtyApp
        val repository = app.container.noteRepository
        val sourceApp = intent?.getStringExtra(EXTRA_SOURCE_APP) ?: "External App"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(ContextNotificationManager.CTS_NOTIFICATION_ID)

        setContent {
            Box(modifier = Modifier.fillMaxSize())
        }

        lifecycleScope.launch {
            val text = awaitClipboardText()
            if (text.isNullOrBlank()) {
                Toast.makeText(this@QuickNotePopupActivity, "No text found on clipboard", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            repository.appendCtsCard(sourceApp, text)
            Toast.makeText(this@QuickNotePopupActivity, "Saved to Circle to Search • $sourceApp", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleReviewCts(intent: Intent?) {
        val app = applicationContext as NaughtyApp
        val repository = app.container.noteRepository
        val sourceApp = intent?.getStringExtra(EXTRA_SOURCE_APP) ?: "External App"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(ContextNotificationManager.CTS_NOTIFICATION_ID)

        var clipTextState by mutableStateOf<String?>(null)
        var isLoading by mutableStateOf(true)

        setContent {
            NaughtyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    val text = clipTextState
                    if (text != null) {
                        ClipSaveSheet(
                            clipText = text,
                            sourceAppName = sourceApp,
                            onSaveToCtsNote = {
                                lifecycleScope.launch {
                                    repository.appendCtsCard(sourceApp, text)
                                    Toast.makeText(this@QuickNotePopupActivity, "Saved to Circle to Search • $sourceApp", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            },
                            onDismiss = { finish() }
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
            }
        }

        lifecycleScope.launch {
            val text = awaitClipboardText()
            isLoading = false
            if (text.isNullOrBlank()) {
                Toast.makeText(this@QuickNotePopupActivity, "No text found on clipboard", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            clipTextState = text
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = applicationContext as NaughtyApp
        val container = app.container
        val repository = container.noteRepository
        val rawAction = intent?.action ?: intent?.getStringExtra("action") ?: "quick_new_note"

        if (rawAction == ACTION_DIRECT_SAVE_CTS) {
            handleDirectSaveCts(intent)
            return
        }

        if (rawAction == ACTION_REVIEW_CTS) {
            handleReviewCts(intent)
            return
        }

        val action = intent?.getStringExtra("action") ?: rawAction
        isTodoMode = (action == "quick_todo_note")

        lifecycleScope.launch {
            if (isTodoMode) {
                val official = repository.getOrCreateOfficialTodoNote()
                activeNoteId = official.metadata.id
                isNewDraftNote = false
            } else {
                val draft = repository.createNote(
                    title = "",
                    content = "",
                    cardType = "modular"
                )
                activeNoteId = draft.id
                isNewDraftNote = true
            }
        }

        setContent {
            OneUiTheme {
                val noteId = activeNoteId
                if (noteId != null) {
                    val viewModel = remember(noteId) { container.noteEditorViewModel() }
                    QuickNotePopupScreen(
                        viewModel = viewModel,
                        noteId = noteId,
                        isNewDraft = isNewDraftNote,
                        isTodoMode = isTodoMode,
                        onExpandToFull = { targetId ->
                            viewModel.saveNow()
                            val fullIntent = Intent(this@QuickNotePopupActivity, MainActivity::class.java).apply {
                                putExtra("noteId", targetId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(fullIntent)
                            finish()
                        },
                        onDismiss = {
                            if (isNewDraftNote) {
                                viewModel.deleteIfEmpty { finish() }
                            } else {
                                viewModel.saveNow()
                                finish()
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricGreen)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DIRECT_SAVE_CTS = "com.example.naughty.ACTION_DIRECT_SAVE_CTS"
        const val ACTION_REVIEW_CTS = "com.example.naughty.ACTION_REVIEW_CTS"
        const val EXTRA_SOURCE_APP = "extra_source_app"
    }
}

@Composable
fun QuickNotePopupScreen(
    viewModel: NoteEditorViewModel,
    noteId: String,
    isNewDraft: Boolean,
    isTodoMode: Boolean,
    onExpandToFull: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val uiState by viewModel.uiState.collectAsState()
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var isUnlockedInSession by remember(noteId) { mutableStateOf(NoteLockSession.isUnlocked(noteId)) }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var lastFocusedBlockId by remember { mutableStateOf<String?>(null) }
    var focusedCursorOffset by remember { mutableIntStateOf(0) }
    var requestFocusBlockId by remember { mutableStateOf<String?>(null) }
    var requestFocusCursorPosition by remember { mutableStateOf<Int?>(null) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showAddBlockMenu by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var reminderTargetBlockId by remember { mutableStateOf<String?>(null) }
    var reminderInitialText by remember { mutableStateOf("") }
    var reminderInitialTime by remember { mutableStateOf<Long?>(null) }
    var isEditingNoteReminder by remember { mutableStateOf(false) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var voiceSheetTab by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
        if (isTodoMode) {
            val newTodoId = viewModel.ensureTrailingTodoItem()
            requestFocusBlockId = newTodoId
        }
    }

    DisposableEffect(noteId) {
        onDispose {
            viewModel.saveNow()
            NoteLockSession.lock(noteId)
        }
    }

    val currentTheme = remember(uiState.colorTheme, isDark) {
        NoteThemeRegistry.getTheme(uiState.colorTheme, isDark)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val optimizedFile = ImageOptimizer.optimizeAndSave(context, uri)
                if (optimizedFile != null) {
                    viewModel.addImage("file://${optimizedFile.absolutePath}")
                }
            }
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
                    if (extractedText.isNotBlank()) {
                        val targetId = focusedBlockId ?: lastFocusedBlockId
                        viewModel.insertExtractedText(extractedText, targetId)
                    } else {
                        Toast.makeText(context, "No text detected in image", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(context, "Failed to extract text", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    // Modal background scrim: tapping backdrop dismisses
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        val configuration = LocalConfiguration.current
        val cardMaxHeight = (configuration.screenHeightDp * 0.78f).dp
        val cardWidth = minOf(440.dp, (configuration.screenWidthDp - 28).dp)

        // Floating Card
        Surface(
            modifier = Modifier
                .width(cardWidth)
                .heightIn(min = 260.dp, max = cardMaxHeight)
                .padding(vertical = 12.dp)
                .imePadding()
                .shadow(24.dp, OneUi.shapes.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume clicks inside card */ },
            shape = OneUi.shapes.card,
            color = currentTheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .paperBackground(
                        paperPattern = currentTheme.paperPattern,
                        lineColor = currentTheme.gridLineColor
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Mode badge
                        val isTodo = isTodoMode || uiState.cardType == "todo"
                        Surface(
                            shape = OneUi.shapes.pill,
                            color = if (isTodo) OneUi.badgePalette.green.copy(alpha = 0.16f) else OneUi.colors.accent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (isTodo) "☑️ TODO LIST" else "✨ QUICK NOTE",
                                style = OneUi.typography.badge,
                                fontWeight = FontWeight.Bold,
                                color = if (isTodo) OneUi.badgePalette.green else OneUi.colors.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Right actions: Theme, Expand to full app, Done check
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { showThemeSheet = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Theme",
                                    tint = currentTheme.textPrimary.copy(alpha = 0.75f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { onExpandToFull(noteId) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInFull,
                                    contentDescription = "Open in app",
                                    tint = currentTheme.textPrimary.copy(alpha = 0.75f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDismiss() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Save and Close",
                                    tint = OneUi.badgePalette.green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Scrollable content area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // Title
                        BasicTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            textStyle = TextStyle(
                                fontFamily = currentTheme.titleFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                color = currentTheme.textPrimary
                            ),
                            cursorBrush = SolidColor(currentTheme.cursorColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                if (uiState.title.isEmpty()) {
                                    Text(
                                        text = if (isTodoMode) "Todo" else "Headline",
                                        fontFamily = currentTheme.titleFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = currentTheme.textSecondary.copy(alpha = 0.45f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (uiState.isRawMarkdownMode) {
                            BasicTextField(
                                value = rawTextFieldValue,
                                onValueChange = { newValue ->
                                    val transformed = BulletTextHelper.handleValueChange(rawTextFieldValue, newValue) ?: newValue
                                    rawTextFieldValue = transformed
                                    if (transformed.text != uiState.content) {
                                        viewModel.updateContent(transformed.text)
                                    }
                                },
                                visualTransformation = reminderVisualTransformation,
                                textStyle = TextStyle(
                                    fontFamily = currentTheme.fontFamily,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = currentTheme.textPrimary
                                ),
                                cursorBrush = SolidColor(currentTheme.cursorColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp)
                            )
                        } else {
                            val blocks = uiState.blocks
                            val showEmptyPlaceholder = blocks.size == 1 && (blocks[0] as? EditorBlock.Text)?.text.isNullOrEmpty()

                            blocks.forEach { block ->
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
                                                requestedCursorPosition = if (requestFocusBlockId == block.id) requestFocusCursorPosition else null,
                                                onFocusConsumed = {
                                                    requestFocusBlockId = null
                                                    requestFocusCursorPosition = null
                                                },
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

                            if (blocks.any { it is EditorBlock.Checklist } || isTodoMode || uiState.cardType == "todo") {
                                Text(
                                    text = "+ Add todo item",
                                    fontFamily = currentTheme.fontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = currentTheme.accent,
                                    modifier = Modifier
                                        .clickable {
                                            val lastChecklist = blocks.lastOrNull { it is EditorBlock.Checklist }
                                            val newId = viewModel.insertChecklistAfter(lastChecklist?.id ?: blocks.lastOrNull()?.id ?: "")
                                            requestFocusBlockId = newId
                                        }
                                        .padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }

                        // Attached images
                        val attachedImages = remember(uiState.content) {
                            val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
                            regex.findAll(uiState.content).map { it.groupValues[1] }.toList()
                        }
                        attachedImages.forEach { imageUri ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                                        .padding(6.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Attached voice notes
                        val attachedVoiceNotes = remember(uiState.content) {
                            EditorBlockParser.extractVoiceNotes(uiState.content)
                        }
                        attachedVoiceNotes.forEach { audioUri ->
                            Spacer(modifier = Modifier.height(8.dp))
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bottom Notes Bar inside Popup
                    val toolbarScrollState = rememberScrollState()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .height(48.dp)
                            .shadow(elevation = 8.dp, shape = OneUi.shapes.pill),
                        shape = OneUi.shapes.pill,
                        color = OneUi.colors.surfaceElevated
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val containerWidth = maxWidth
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .horizontalScroll(toolbarScrollState)
                                        .widthIn(min = containerWidth)
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // (+) Add block menu
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { showAddBlockMenu = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = "Add block",
                                                tint = OneUi.colors.accent,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showAddBlockMenu,
                                            onDismissRequest = { showAddBlockMenu = false },
                                            shape = OneUi.shapes.menu,
                                            containerColor = OneUi.colors.surfaceElevated
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
                                                    if (uiState.isRawMarkdownMode) {
                                                        val (updatedText, newCursor) = viewModel.toggleOrAddBulletInRaw(rawTextFieldValue.text, rawTextFieldValue.selection.start)
                                                        rawTextFieldValue = TextFieldValue(text = updatedText, selection = TextRange(newCursor))
                                                    } else {
                                                        val targetId = focusedBlockId ?: lastFocusedBlockId
                                                        val (blockId, newCursor) = viewModel.toggleOrAddBulletItem(targetId, focusedCursorOffset)
                                                        requestFocusBlockId = blockId
                                                        requestFocusCursorPosition = newCursor
                                                    }
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
                                                    reminderTargetBlockId = null
                                                    reminderInitialText = uiState.title.ifBlank { "Reminder" }
                                                    reminderInitialTime = null
                                                    isEditingNoteReminder = false
                                                    showReminderSheet = true
                                                }
                                            )
                                        }
                                    }

                                    // Aa Format
                                    IconButton(
                                        onClick = { viewModel.toggleRawMarkdownMode() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(
                                            text = "Aa",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Checkbox / Todo button
                                    IconButton(
                                        onClick = {
                                            val targetId = focusedBlockId ?: lastFocusedBlockId
                                            val newId = viewModel.addChecklistItemAtCursor(targetId, focusedCursorOffset)
                                            requestFocusBlockId = newId
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckBox,
                                            contentDescription = "Add todo item",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Bullet list button
                                    IconButton(
                                        onClick = {
                                            if (uiState.isRawMarkdownMode) {
                                                val (updatedText, newCursor) = viewModel.toggleOrAddBulletInRaw(rawTextFieldValue.text, rawTextFieldValue.selection.start)
                                                rawTextFieldValue = TextFieldValue(text = updatedText, selection = TextRange(newCursor))
                                            } else {
                                                val targetId = focusedBlockId ?: lastFocusedBlockId
                                                val (blockId, newCursor) = viewModel.toggleOrAddBulletItem(targetId, focusedCursorOffset)
                                                requestFocusBlockId = blockId
                                                requestFocusCursorPosition = newCursor
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                            contentDescription = "Bullet list",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Image button
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = "Attach image",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Text from image (OCR)
                                    IconButton(
                                        onClick = { ocrImagePickerLauncher.launch("image/*") },
                                        enabled = !isExtractingText,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (isExtractingText) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
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

                                    // Voice Note
                                    IconButton(
                                        onClick = {
                                            voiceSheetTab = 0
                                            showVoiceSheet = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mic,
                                            contentDescription = "Voice note",
                                            tint = currentTheme.accent,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Link button
                                    IconButton(
                                        onClick = { viewModel.updateContent(uiState.content + "\nhttps://") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Link,
                                            contentDescription = "Link",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Bell Reminder button
                                    IconButton(
                                        onClick = {
                                            val targetId = focusedBlockId ?: lastFocusedBlockId
                                            val focusedChecklist = uiState.blocks.find { it.id == targetId } as? EditorBlock.Checklist
                                            if (focusedChecklist != null) {
                                                reminderTargetBlockId = focusedChecklist.id
                                                reminderInitialText = focusedChecklist.text
                                                reminderInitialTime = focusedChecklist.reminder?.timestampMillis
                                                isEditingNoteReminder = false
                                                showReminderSheet = true
                                            } else {
                                                reminderTargetBlockId = null
                                                reminderInitialText = uiState.title.ifBlank { "Note Reminder" }
                                                reminderInitialTime = uiState.reminderTime
                                                isEditingNoteReminder = true
                                                showReminderSheet = true
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.hasActiveReminders) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                            contentDescription = "Reminder",
                                            tint = if (uiState.hasActiveReminders) currentTheme.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Undo
                                    IconButton(
                                        onClick = { viewModel.undo() },
                                        enabled = uiState.canUndo,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                                            contentDescription = "Undo",
                                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Redo
                                    IconButton(
                                        onClick = { viewModel.redo() },
                                        enabled = uiState.canRedo,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                                            contentDescription = "Redo",
                                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Theme Selection Sheet
    if (showThemeSheet) {
        ThemeSelectionSheet(
            currentThemeId = uiState.colorTheme,
            onSelectTheme = { themeId ->
                viewModel.updateColorTheme(themeId)
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    // Reminder Bottom Sheet
    if (showReminderSheet) {
        ReminderBottomSheet(
            initialTimeMillis = reminderInitialTime ?: uiState.reminderTime,
            targetContextText = reminderInitialText,
            onDismiss = {
                showReminderSheet = false
                reminderTargetBlockId = null
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
                if (reminderTargetBlockId != null) {
                    viewModel.setChecklistReminder(
                        blockId = reminderTargetBlockId!!,
                        timeMillis = timeMillis,
                        isCalendar = isCalendar,
                        context = context
                    )
                } else {
                    viewModel.setNoteReminder(timeMillis, isCalendar, context)
                }
                showReminderSheet = false
                reminderTargetBlockId = null
                reminderInitialText = ""
                reminderInitialTime = null
                isEditingNoteReminder = false
            }
        )
    }

    // Fullscreen image viewer
    if (fullscreenImageUri != null) {
        FullscreenImageViewer(
            imageUri = fullscreenImageUri!!,
            onDismiss = { fullscreenImageUri = null }
        )
    }

    // Voice recording sheet
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
