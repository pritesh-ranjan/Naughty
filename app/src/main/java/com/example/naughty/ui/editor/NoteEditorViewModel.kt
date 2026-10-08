package com.example.naughty.ui.editor

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.data.repository.BindingRepository
import com.example.naughty.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.naughty.notification.ReminderManager
import com.example.naughty.ui.theme.NoteThemeRegistry

data class EditorUiState(
    val noteId: String = "",
    val title: String = "",
    val content: String = "",
    val blocks: List<EditorBlock> = emptyList(),
    val fileName: String = "",
    val boundApp: String? = null,
    val boundApps: List<String> = emptyList(),
    val colorTheme: String = "",
    val cardType: String = "modular",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isRawMarkdownMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val reminderTime: Long? = null,
    val hasActiveReminders: Boolean = false,
    val isLocked: Boolean = false
)

class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
    private val bindingRepository: BindingRepository,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var bindingJob: Job? = null
    private val undoHistory = mutableListOf<String>()
    private val redoHistory = mutableListOf<String>()

    fun loadNote(noteId: String) {
        bindingJob?.cancel()
        bindingJob = viewModelScope.launch {
            bindingRepository.getByNoteId(noteId).collect { bindings ->
                val activeApps = bindings.filter { it.isActive }.map { it.appLabel }
                _uiState.value = _uiState.value.copy(
                    boundApps = activeApps,
                    boundApp = activeApps.firstOrNull()
                )
            }
        }

        viewModelScope.launch {
            val note = noteRepository.getNote(noteId)
            if (note != null) {
                undoHistory.clear()
                redoHistory.clear()
                undoHistory.add(note.content)

                val hasInline = InlineReminderParser.findUpcomingReminders(note.content).isNotEmpty()
                val hasNoteReminder = note.metadata.reminderTime != null && note.metadata.reminderTime > System.currentTimeMillis()

                val parsedBlocks = EditorBlockParser.parse(note.content)
                _uiState.value = _uiState.value.copy(
                    noteId = noteId,
                    title = if (note.metadata.title.equals("Untitled", ignoreCase = true) || note.metadata.title.equals("New thought", ignoreCase = true)) "" else note.metadata.title,
                    content = note.content,
                    blocks = parsedBlocks,
                    fileName = note.metadata.fileName,
                    colorTheme = note.metadata.colorTheme.ifBlank { NoteThemeRegistry.getRandomThemeId() },
                    cardType = note.metadata.cardType.ifBlank { "modular" },
                    isPinned = note.metadata.isPinned,
                    isArchived = note.metadata.isArchived,
                    isLocked = note.metadata.isLocked,
                    isLoading = false,
                    canUndo = false,
                    canRedo = false,
                    reminderTime = note.metadata.reminderTime,
                    hasActiveReminders = hasInline || hasNoteReminder
                )
            } else {
                _uiState.value = EditorUiState(
                    noteId = noteId,
                    blocks = listOf(EditorBlock.Text(text = "")),
                    isLoading = false
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun updateColorTheme(colorTheme: String) {
        _uiState.value = _uiState.value.copy(colorTheme = colorTheme)
        scheduleAutoSave()
    }

    fun updateContent(newContent: String) {
        val current = _uiState.value.content
        if (newContent != current) {
            undoHistory.add(current)
            if (undoHistory.size > 30) undoHistory.removeAt(0)
            redoHistory.clear()
        }
        val parsedBlocks = EditorBlockParser.parse(newContent)
        val hasInline = InlineReminderParser.findUpcomingReminders(newContent).isNotEmpty()
        val hasNote = _uiState.value.reminderTime != null && _uiState.value.reminderTime!! > System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            content = newContent,
            blocks = parsedBlocks,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = false,
            hasActiveReminders = hasInline || hasNote
        )
        scheduleAutoSave()
    }

    fun setNoteReminder(timeMillis: Long, isCalendar: Boolean, context: Context? = null) {
        val state = _uiState.value
        _uiState.value = state.copy(
            reminderTime = timeMillis,
            hasActiveReminders = true
        )
        viewModelScope.launch {
            noteRepository.setReminder(state.noteId, timeMillis)
        }
        val cleanSnippet = state.content.take(150).replace("\n", " ").trim()
        reminderManager.scheduleReminder(
            noteId = state.noteId,
            reminderId = state.noteId.hashCode(),
            title = state.title.ifBlank { "Untitled Note" },
            contentSnippet = cleanSnippet,
            triggerAtMillis = timeMillis
        )
        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = state.title.ifBlank { "Note Reminder" },
                description = state.content,
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancelNoteReminder() {
        val state = _uiState.value
        _uiState.value = state.copy(
            reminderTime = null,
            hasActiveReminders = InlineReminderParser.findUpcomingReminders(state.content).isNotEmpty()
        )
        reminderManager.cancelReminder(state.noteId.hashCode())
        viewModelScope.launch {
            noteRepository.clearReminder(state.noteId)
        }
    }

    fun addOrUpdateInlineReminder(
        lineText: String,
        updatedReminderText: String? = null,
        timeMillis: Long,
        isCalendar: Boolean,
        context: Context? = null
    ) {
        val currentContent = _uiState.value.content
        val isChecked = lineText.contains("- [x]", ignoreCase = true)
        val prefix = if (lineText.trim().startsWith("- [")) {
            if (isChecked) "- [x] " else "- [ ] "
        } else ""

        val rawClean = InlineReminderParser.removeReminderFromLine(
            lineText.replace(Regex("""^[-*]\s*\[[ xX]?\]\s*"""), "")
        ).trim()
        val textToUse = updatedReminderText?.trim()?.ifBlank { null } ?: rawClean
        val tag = InlineReminderParser.createReminderTag(timeMillis)
        val updatedLine = "$prefix$textToUse $tag"

        val updatedContent = if (currentContent.contains(lineText)) {
            currentContent.replace(lineText, updatedLine)
        } else {
            currentContent.trimEnd() + "\n" + updatedLine
        }
        updateContent(updatedContent)

        val oldReminder = InlineReminderParser.extractFirstReminder(lineText)
        if (oldReminder != null && oldReminder.timestampMillis != timeMillis) {
            reminderManager.cancelReminder(oldReminder.timestampMillis.hashCode())
        }

        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = textToUse.ifBlank { _uiState.value.title.ifBlank { "Task Reminder" } },
            contentSnippet = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = textToUse.ifBlank { _uiState.value.title.ifBlank { "Task Reminder" } },
                description = "Task in note: ${_uiState.value.title.ifBlank { "Untitled" }}",
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun addChecklistItemWithReminder(
        itemText: String,
        timeMillis: Long,
        isCalendar: Boolean,
        context: Context? = null
    ) {
        val tag = InlineReminderParser.createReminderTag(timeMillis)
        val line = "- [ ] ${itemText.trim()} $tag"
        val current = _uiState.value.content
        val updated = if (current.isBlank()) {
            line
        } else {
            val lines = current.lines().toMutableList()
            val lastCheckIndex = lines.indexOfLast { it.trim().startsWith("- [") }
            if (lastCheckIndex >= 0) {
                lines.add(lastCheckIndex + 1, line)
                lines.joinToString("\n")
            } else {
                "$current\n$line"
            }
        }
        updateContent(updated)

        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = itemText.ifBlank { "Task Reminder" },
            contentSnippet = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = itemText.ifBlank { "Task Reminder" },
                description = "Task in note: ${_uiState.value.title.ifBlank { "Untitled" }}",
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun insertInlineReminderText(
        reminderText: String,
        timeMillis: Long,
        isCalendar: Boolean,
        context: Context? = null
    ) {
        val currentContent = _uiState.value.content
        val tag = InlineReminderParser.createReminderTag(timeMillis)
        val newEntry = "${reminderText.trim()} $tag"
        val updatedContent = if (currentContent.isBlank()) {
            newEntry
        } else {
            "${currentContent.trimEnd()}\n$newEntry"
        }
        updateContent(updatedContent)

        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = reminderText.ifBlank { _uiState.value.title.ifBlank { "Reminder" } },
            contentSnippet = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = reminderText.ifBlank { _uiState.value.title.ifBlank { "Reminder" } },
                description = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun scheduleStandaloneReminder(
        title: String,
        timeMillis: Long,
        isCalendar: Boolean,
        context: Context? = null
    ) {
        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = title.ifBlank { _uiState.value.title.ifBlank { "Reminder" } },
            contentSnippet = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = title.ifBlank { _uiState.value.title.ifBlank { "Reminder" } },
                description = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun removeInlineReminder(lineText: String, timeMillis: Long) {
        val currentContent = _uiState.value.content
        val cleanedLine = InlineReminderParser.removeReminderFromLine(lineText, timeMillis)
        val updatedContent = currentContent.replace(lineText, cleanedLine)
        updateContent(updatedContent)
        reminderManager.cancelReminder(timeMillis.hashCode())
    }

    fun insertReminderAtEnd(timeMillis: Long, isCalendar: Boolean, context: Context? = null) {
        val currentContent = _uiState.value.content
        val tag = InlineReminderParser.createReminderTag(timeMillis)
        val updatedContent = if (currentContent.isBlank()) tag else "$currentContent $tag"
        updateContent(updatedContent)

        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = _uiState.value.title.ifBlank { "Note Reminder" },
            contentSnippet = currentContent.take(150),
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = _uiState.value.title.ifBlank { "Note Reminder" },
                description = currentContent,
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun toggleRawMarkdownMode() {
        _uiState.value = _uiState.value.copy(
            isRawMarkdownMode = !_uiState.value.isRawMarkdownMode
        )
    }

    private fun updateBlocksInternal(newBlocks: List<EditorBlock>) {
        val attachedImages = EditorBlockParser.extractImages(_uiState.value.content)
        val attachedVoiceNotes = EditorBlockParser.extractVoiceNotes(_uiState.value.content)
        val newContent = EditorBlockParser.toMarkdown(newBlocks, attachedImages, attachedVoiceNotes)
        val current = _uiState.value.content
        if (newContent != current) {
            undoHistory.add(current)
            if (undoHistory.size > 30) undoHistory.removeAt(0)
            redoHistory.clear()
        }
        val hasInline = InlineReminderParser.findUpcomingReminders(newContent).isNotEmpty()
        val hasNote = _uiState.value.reminderTime != null && _uiState.value.reminderTime!! > System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            blocks = newBlocks,
            content = newContent,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = false,
            hasActiveReminders = hasInline || hasNote
        )
        scheduleAutoSave()
    }

    fun addChecklistItemAtCursor(currentBlockId: String?, cursorOffset: Int = 0): String {
        val currentBlocks = _uiState.value.blocks.toMutableList()
        val newChecklistId = java.util.UUID.randomUUID().toString()

        if (currentBlockId == null || currentBlocks.isEmpty()) {
            val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
            currentBlocks.add(newBlock)
            updateBlocksInternal(currentBlocks)
            return newChecklistId
        }

        val blockIndex = currentBlocks.indexOfFirst { it.id == currentBlockId }
        if (blockIndex == -1) {
            val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
            currentBlocks.add(newBlock)
            updateBlocksInternal(currentBlocks)
            return newChecklistId
        }

        val targetBlock = currentBlocks[blockIndex]
        when (targetBlock) {
            is EditorBlock.Checklist -> {
                val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
                currentBlocks.add(blockIndex + 1, newBlock)
                updateBlocksInternal(currentBlocks)
                return newChecklistId
            }
            is EditorBlock.Text -> {
                val text = targetBlock.text
                if (text.isBlank()) {
                    val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
                    currentBlocks[blockIndex] = newBlock
                    updateBlocksInternal(currentBlocks)
                    return newChecklistId
                }

                val safeOffset = cursorOffset.coerceIn(0, text.length)
                val lineStart = text.lastIndexOf('\n', (safeOffset - 1).coerceAtLeast(0)).let {
                    if (it == -1) 0 else it + 1
                }
                val lineEnd = text.indexOf('\n', safeOffset).let {
                    if (it == -1) text.length else it
                }

                val beforeLine = if (lineStart > 0) text.substring(0, lineStart).trimEnd('\n') else ""
                val currentLine = text.substring(lineStart, lineEnd).trim()
                val afterLine = if (lineEnd < text.length) text.substring(lineEnd + 1).trimStart('\n') else ""

                val replacementBlocks = mutableListOf<EditorBlock>()
                if (beforeLine.isNotEmpty()) {
                    replacementBlocks.add(EditorBlock.Text(id = targetBlock.id, text = beforeLine))
                }

                val checklistBlock = EditorBlock.Checklist(
                    id = newChecklistId,
                    isChecked = false,
                    text = currentLine
                )
                replacementBlocks.add(checklistBlock)

                if (afterLine.isNotEmpty()) {
                    replacementBlocks.add(EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = afterLine))
                }

                currentBlocks.removeAt(blockIndex)
                currentBlocks.addAll(blockIndex, replacementBlocks)
                updateBlocksInternal(currentBlocks)
                return newChecklistId
            }
        }
    }

    fun handleEnterOnChecklist(blockId: String, currentText: String? = null): String {
        val currentBlocks = _uiState.value.blocks.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return insertChecklistAfter(blockId)

        val targetBlock = currentBlocks[index] as? EditorBlock.Checklist
            ?: return insertChecklistAfter(blockId)

        val effectiveText = (currentText ?: targetBlock.text).trim()
        if (effectiveText.isNotEmpty()) {
            if (targetBlock.text != (currentText ?: targetBlock.text)) {
                currentBlocks[index] = targetBlock.copy(text = currentText ?: targetBlock.text)
            }
            val newChecklistId = java.util.UUID.randomUUID().toString()
            val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
            currentBlocks.add(index + 1, newBlock)
            updateBlocksInternal(currentBlocks)
            return newChecklistId
        }

        // Empty checklist item: Enter should exit the checklist to a new text line!
        if (targetBlock.reminder != null) {
            reminderManager.cancelReminder(targetBlock.reminder.timestampMillis.hashCode())
        }

        val nextBlock = currentBlocks.getOrNull(index + 1)
        if (nextBlock is EditorBlock.Text) {
            currentBlocks.removeAt(index)
            updateBlocksInternal(currentBlocks)
            return nextBlock.id
        }

        val newTextBlock = EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = "")
        currentBlocks[index] = newTextBlock
        updateBlocksInternal(currentBlocks)
        return newTextBlock.id
    }

    fun insertChecklistAfter(blockId: String): String {
        val currentBlocks = _uiState.value.blocks.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        val newChecklistId = java.util.UUID.randomUUID().toString()
        val newBlock = EditorBlock.Checklist(id = newChecklistId, isChecked = false, text = "")
        if (index >= 0) {
            currentBlocks.add(index + 1, newBlock)
        } else {
            currentBlocks.add(newBlock)
        }
        updateBlocksInternal(currentBlocks)
        return newChecklistId
    }

    fun updateChecklistText(blockId: String, newText: String) {
        val currentBlocks = _uiState.value.blocks.map { block ->
            if (block is EditorBlock.Checklist && block.id == blockId) {
                block.copy(text = newText)
            } else {
                block
            }
        }
        updateBlocksInternal(currentBlocks)
    }

    fun toggleChecklistBlock(blockId: String) {
        val currentBlocks = _uiState.value.blocks.map { block ->
            if (block is EditorBlock.Checklist && block.id == blockId) {
                block.copy(isChecked = !block.isChecked)
            } else {
                block
            }
        }
        updateBlocksInternal(currentBlocks)
    }

    fun updateTextBlock(blockId: String, newText: String) {
        val currentBlocks = _uiState.value.blocks.map { block ->
            if (block is EditorBlock.Text && block.id == blockId) {
                block.copy(text = newText)
            } else {
                block
            }
        }
        updateBlocksInternal(currentBlocks)
    }

    fun deleteBlock(blockId: String): String? {
        val currentBlocks = _uiState.value.blocks.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return null

        val removed = currentBlocks.removeAt(index)
        if (removed is EditorBlock.Checklist && removed.reminder != null) {
            reminderManager.cancelReminder(removed.reminder.timestampMillis.hashCode())
        }

        val focusTargetId: String? = when {
            index > 0 -> currentBlocks[index - 1].id
            currentBlocks.isNotEmpty() -> currentBlocks[0].id
            else -> null
        }

        val mergedBlocks = mutableListOf<EditorBlock>()
        for (block in currentBlocks) {
            val last = mergedBlocks.lastOrNull()
            if (last is EditorBlock.Text && block is EditorBlock.Text) {
                mergedBlocks[mergedBlocks.lastIndex] = last.copy(
                    text = if (last.text.isBlank()) block.text else if (block.text.isBlank()) last.text else "${last.text}\n${block.text}"
                )
            } else {
                mergedBlocks.add(block)
            }
        }

        if (mergedBlocks.isEmpty()) {
            val emptyText = EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = "")
            mergedBlocks.add(emptyText)
            updateBlocksInternal(mergedBlocks)
            return emptyText.id
        }

        updateBlocksInternal(mergedBlocks)
        return focusTargetId
    }

    fun setChecklistReminder(blockId: String, timeMillis: Long, isCalendar: Boolean, context: Context? = null) {
        val tag = InlineReminderParser.createReminderTag(timeMillis)
        val reminder = InlineReminder(
            rawTag = tag,
            displayText = InlineReminderParser.formatTimestamp(timeMillis),
            timestampMillis = timeMillis,
            startIndex = 0,
            endIndex = 0
        )
        val currentBlocks = _uiState.value.blocks.map { block ->
            if (block is EditorBlock.Checklist && block.id == blockId) {
                block.copy(reminder = reminder)
            } else {
                block
            }
        }
        updateBlocksInternal(currentBlocks)

        val targetTask = _uiState.value.blocks.find { it.id == blockId } as? EditorBlock.Checklist
        val taskText = targetTask?.text?.ifBlank { "Task Reminder" } ?: "Task Reminder"
        reminderManager.scheduleReminder(
            noteId = _uiState.value.noteId,
            reminderId = timeMillis.hashCode(),
            title = taskText,
            contentSnippet = "Note: ${_uiState.value.title.ifBlank { "Untitled" }}",
            triggerAtMillis = timeMillis
        )

        if (isCalendar && context != null) {
            val calIntent = ReminderManager.createCalendarIntent(
                title = taskText,
                description = "Task in note: ${_uiState.value.title.ifBlank { "Untitled" }}",
                startTimeMillis = timeMillis
            )
            try {
                context.startActivity(calIntent)
            } catch (_: Exception) {}
        }
    }

    fun removeChecklistReminder(blockId: String) {
        val target = _uiState.value.blocks.find { it.id == blockId } as? EditorBlock.Checklist
        if (target?.reminder != null) {
            reminderManager.cancelReminder(target.reminder.timestampMillis.hashCode())
        }
        val currentBlocks = _uiState.value.blocks.map { block ->
            if (block is EditorBlock.Checklist && block.id == blockId) {
                block.copy(reminder = null)
            } else {
                block
            }
        }
        updateBlocksInternal(currentBlocks)
    }

    fun toggleChecklistItem(line: String) {
        val block = _uiState.value.blocks.find {
            it is EditorBlock.Checklist && line.contains(it.text)
        }
        if (block != null) {
            toggleChecklistBlock(block.id)
        } else {
            val currentContent = _uiState.value.content
            val isChecked = line.contains("- [x]", ignoreCase = true)
            val afterCheck = line.replaceFirst(Regex("""^-\s*\[[ xX]\]\s*"""), "")
            val replacement = if (isChecked) "- [ ] $afterCheck" else "- [x] $afterCheck"
            val updated = currentContent.replace(line, replacement)
            updateContent(updated)
        }
    }

    fun addChecklistItem(itemText: String = "") {
        val newId = addChecklistItemAtCursor(null, 0)
        if (itemText.isNotBlank()) {
            updateChecklistText(newId, itemText)
        }
    }

    fun updateChecklistItem(oldLine: String, newText: String) {
        val block = _uiState.value.blocks.find {
            it is EditorBlock.Checklist && (oldLine.contains(it.text) || it.text.isEmpty())
        }
        if (block != null) {
            updateChecklistText(block.id, newText)
        } else {
            val currentContent = _uiState.value.content
            if (!currentContent.contains(oldLine)) return
            val isChecked = oldLine.contains("- [x]", ignoreCase = true)
            val reminder = InlineReminderParser.extractFirstReminder(oldLine)
            val checkPrefix = if (isChecked) "- [x] " else "- [ ] "
            val reminderSuffix = if (reminder != null) " ${reminder.rawTag}" else ""
            val newLine = checkPrefix + newText + reminderSuffix
            val updated = currentContent.replace(oldLine, newLine)
            updateContent(updated)
        }
    }

    fun deleteChecklistItem(line: String) {
        val block = _uiState.value.blocks.find {
            it is EditorBlock.Checklist && line.contains(it.text)
        }
        if (block != null) {
            deleteBlock(block.id)
        } else {
            val currentContent = _uiState.value.content
            val reminder = InlineReminderParser.extractFirstReminder(line)
            if (reminder != null) {
                reminderManager.cancelReminder(reminder.timestampMillis.hashCode())
            }
            val lines = currentContent.lines().toMutableList()
            val index = lines.indexOf(line)
            if (index >= 0) {
                lines.removeAt(index)
                val updated = lines.joinToString("\n")
                updateContent(updated)
            }
        }
    }

    fun addBulletItem(itemText: String, blockHeader: String? = null) {
        if (itemText.isBlank()) return
        val currentContent = _uiState.value.content
        val newLine = "• $itemText"

        val updated = if (blockHeader != null && currentContent.contains(blockHeader)) {
            val headerIndex = currentContent.indexOf(blockHeader)
            val nextLineIndex = currentContent.indexOf("\n", headerIndex).let {
                if (it == -1) currentContent.length else it
            }
            currentContent.substring(0, nextLineIndex) + "\n" + newLine + currentContent.substring(nextLineIndex)
        } else {
            currentContent.trimEnd() + "\n" + newLine
        }
        updateContent(updated)
    }

    fun insertExtractedText(text: String, targetBlockId: String? = null) {
        if (text.isBlank()) return
        val cleanText = text.trim()

        if (_uiState.value.isRawMarkdownMode) {
            val current = _uiState.value.content
            val updated = if (current.isBlank()) {
                cleanText
            } else {
                current.trimEnd() + "\n\n" + cleanText
            }
            updateContent(updated)
            return
        }

        val attachedImages = EditorBlockParser.extractImages(_uiState.value.content)
        val attachedVoiceNotes = EditorBlockParser.extractVoiceNotes(_uiState.value.content)
        val currentBlocks = _uiState.value.blocks.toMutableList()

        if (currentBlocks.isEmpty() || (currentBlocks.size == 1 && (currentBlocks[0] as? EditorBlock.Text)?.text.isNullOrBlank())) {
            currentBlocks.clear()
            currentBlocks.add(EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = cleanText))
        } else {
            val targetIndex = if (targetBlockId != null) currentBlocks.indexOfFirst { it.id == targetBlockId } else -1
            if (targetIndex != -1) {
                val targetBlock = currentBlocks[targetIndex]
                if (targetBlock is EditorBlock.Text && targetBlock.text.isBlank()) {
                    currentBlocks[targetIndex] = EditorBlock.Text(id = targetBlock.id, text = cleanText)
                } else {
                    if (targetBlock is EditorBlock.Text) {
                        currentBlocks[targetIndex] = EditorBlock.Text(id = targetBlock.id, text = targetBlock.text.trimEnd())
                    }
                    currentBlocks.add(targetIndex + 1, EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = cleanText))
                }
            } else {
                val lastBlock = currentBlocks.lastOrNull()
                if (lastBlock is EditorBlock.Text) {
                    if (lastBlock.text.isBlank()) {
                        currentBlocks[currentBlocks.lastIndex] = EditorBlock.Text(id = lastBlock.id, text = cleanText)
                    } else {
                        currentBlocks[currentBlocks.lastIndex] = EditorBlock.Text(id = lastBlock.id, text = lastBlock.text.trimEnd())
                        currentBlocks.add(EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = cleanText))
                    }
                } else {
                    currentBlocks.add(EditorBlock.Text(id = java.util.UUID.randomUUID().toString(), text = cleanText))
                }
            }
        }

        val newContent = EditorBlockParser.toMarkdown(currentBlocks, attachedImages, attachedVoiceNotes)
        updateContent(newContent)
    }

    fun addImage(imageUri: String) {
        if (imageUri.isBlank()) return
        val currentContent = _uiState.value.content
        val imageStr = "\n\n![Image]($imageUri)\n"
        updateContent(currentContent.trimEnd() + imageStr)
    }

    fun removeImage(imageUri: String) {
        val currentContent = _uiState.value.content
        val regex = Regex("!\\[.*?\\]\\(" + Regex.escape(imageUri) + "\\)\n?")
        val updated = currentContent.replace(regex, "").trim()
        updateContent(updated)
    }

    fun addVoiceNote(audioUri: String, transcript: String? = null) {
        if (audioUri.isBlank()) return
        val currentContent = _uiState.value.content
        val audioStr = "\n\n[🎤 Voice Note]($audioUri)\n"
        val updated = currentContent.trimEnd() + audioStr
        updateContent(updated)
        if (!transcript.isNullOrBlank()) {
            insertExtractedText(transcript)
        }
    }

    fun removeVoiceNote(audioUri: String) {
        val currentContent = _uiState.value.content
        val regex = Regex("""!?\[(?:🎤\s*)?Voice Note.*?\]\(""" + Regex.escape(audioUri) + """\)\n?""")
        val updated = currentContent.replace(regex, "").trim()
        updateContent(updated)
    }

    fun undo() {
        if (undoHistory.isNotEmpty()) {
            val previous = undoHistory.removeAt(undoHistory.lastIndex)
            redoHistory.add(_uiState.value.content)
            _uiState.value = _uiState.value.copy(
                content = previous,
                blocks = EditorBlockParser.parse(previous),
                canUndo = undoHistory.isNotEmpty(),
                canRedo = true
            )
            scheduleAutoSave()
        }
    }

    fun redo() {
        if (redoHistory.isNotEmpty()) {
            val next = redoHistory.removeAt(redoHistory.lastIndex)
            undoHistory.add(_uiState.value.content)
            _uiState.value = _uiState.value.copy(
                content = next,
                blocks = EditorBlockParser.parse(next),
                canUndo = true,
                canRedo = redoHistory.isNotEmpty()
            )
            scheduleAutoSave()
        }
    }

    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = newPinned)
        viewModelScope.launch {
            noteRepository.togglePin(_uiState.value.noteId)
        }
    }

    fun setNoteLocked(locked: Boolean) {
        _uiState.value = _uiState.value.copy(isLocked = locked)
        viewModelScope.launch {
            noteRepository.toggleLock(_uiState.value.noteId, locked)
        }
    }

    fun archiveNote(onDone: () -> Unit) {
        viewModelScope.launch {
            noteRepository.toggleArchive(_uiState.value.noteId)
            onDone()
        }
    }

    fun deleteNote(onDone: () -> Unit) {
        viewModelScope.launch {
            noteRepository.deleteNote(_uiState.value.noteId)
            onDone()
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1200)
            save()
        }
    }

    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch { save() }
    }

    private suspend fun save() {
        val state = _uiState.value
        if (state.noteId.isBlank()) return

        _uiState.value = state.copy(isSaving = true)

        noteRepository.updateNote(
            id = state.noteId,
            title = state.title.trim(),
            content = state.content,
            colorTheme = state.colorTheme,
            cardType = state.cardType
        )

        _uiState.value = _uiState.value.copy(isSaving = false)
    }

    fun shareNoteAsText(context: Context) {
        val title = _uiState.value.title.trim().ifBlank { "Untitled" }
        val content = _uiState.value.content.trim()
        val textToShare = if (content.isBlank()) {
            title
        } else if (title.isBlank() || title.equals("Untitled", ignoreCase = true) || title.equals("New thought", ignoreCase = true)) {
            content
        } else {
            "$title\n\n$content"
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, textToShare)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Note")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareNoteAsFile(context: Context) {
        saveNow()
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = state.fileName.ifBlank {
                val note = noteRepository.getNote(state.noteId)
                note?.metadata?.fileName ?: "${state.noteId}.md"
            }
            val file = noteRepository.getNoteFile(fileName)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.writeText(state.content, Charsets.UTF_8)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, state.title.ifBlank { "Untitled" })
                clipData = android.content.ClipData.newRawUri("Note file", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Markdown File (.md)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
        }
    }
}
