package com.example.naughty.data.repository

import com.example.naughty.data.file.MarkdownFileManager
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.local.NoteMetadataDao
import com.example.naughty.ui.theme.NoteThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NoteWithContent(
    val metadata: NoteMetadata,
    val content: String
)

class NoteRepository(
    private val dao: NoteMetadataDao,
    private val fileManager: MarkdownFileManager,
    private val bindingDao: com.example.naughty.data.local.AppBindingDao? = null
) {

    fun getAllActive(): Flow<List<NoteMetadata>> = dao.getAllActive()

    fun getAllDeleted(): Flow<List<NoteMetadata>> = dao.getAllDeleted()

    fun countActive(): Flow<Int> = dao.countActive()

    fun countDeleted(): Flow<Int> = dao.countDeleted()

    suspend fun getNote(id: String): NoteWithContent? {
        val metadata = dao.getById(id) ?: return null
        val content = fileManager.readNote(metadata.fileName)
        return NoteWithContent(metadata, content)
    }

    suspend fun createNote(
        title: String = "",
        content: String = "",
        tag: String = "",
        colorTheme: String? = null,
        cardType: String = "modular",
        isPinned: Boolean = false,
        isLocked: Boolean = false
    ): NoteMetadata {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val datePrefix = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val slug = title.ifBlank { "untitled" }
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
        val fileName = "${datePrefix}_${slug}_${id.take(8)}.md"

        val assignedTheme = if (!colorTheme.isNullOrBlank()) {
            colorTheme
        } else {
            NoteThemeRegistry.getRandomThemeId()
        }

        val effectiveCardType = if (cardType == "modular" && title.trim().equals("todo", ignoreCase = true)) {
            val existing = dao.getOfficialTodoNote()
            if (existing == null) "todo" else cardType
        } else {
            cardType
        }

        val metadata = NoteMetadata(
            id = id,
            title = title.ifBlank { "Untitled" },
            createdAt = now,
            modifiedAt = now,
            isPinned = isPinned,
            fileName = fileName,
            tag = tag,
            colorTheme = assignedTheme,
            cardType = effectiveCardType,
            isLocked = isLocked
        )

        fileManager.writeNote(fileName, content)
        dao.insert(metadata)
        return metadata
    }

    suspend fun adoptExistingTodoNoteIfPresent(): NoteMetadata? {
        val existingOfficial = dao.getOfficialTodoNote()
        if (existingOfficial != null) return existingOfficial

        val candidate = dao.findCandidateTodoNoteByTitle() ?: return null
        val updated = candidate.copy(
            cardType = "todo",
            modifiedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        return updated
    }

    suspend fun getOrCreateOfficialTodoNote(): NoteWithContent {
        val official = adoptExistingTodoNoteIfPresent()
        if (official != null) {
            val content = fileManager.readNote(official.fileName)
            return NoteWithContent(official, content)
        }

        val created = createNote(
            title = "Todo",
            content = "- [ ] ",
            cardType = "todo",
            isPinned = true
        )
        return NoteWithContent(created, "- [ ] ")
    }

    suspend fun getOrCreateDedicatedCtsNote(): NoteWithContent {
        val existing = dao.getDedicatedCtsNote()
        if (existing != null) {
            if (existing.cardType != "circle_to_search" || existing.isPinned) {
                val updated = existing.copy(cardType = "circle_to_search", isPinned = false)
                dao.update(updated)
                val content = fileManager.readNote(updated.fileName)
                return NoteWithContent(updated, content)
            }
            val content = fileManager.readNote(existing.fileName)
            return NoteWithContent(existing, content)
        }

        val created = createNote(
            title = "Circle to Search",
            content = "",
            cardType = "circle_to_search",
            isPinned = false
        )
        return NoteWithContent(created, "")
    }

    suspend fun isClipAlreadyRecorded(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        val note = getOrCreateDedicatedCtsNote()
        return note.content.contains(trimmed)
    }

    suspend fun appendCtsCard(sourceAppName: String, text: String): NoteMetadata {
        val ctsNote = getOrCreateDedicatedCtsNote()
        val existingContent = ctsNote.content.trim()
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val cardHeader = "### 🌐 $sourceAppName • $timeStr"
        val snippet = text.trim()

        val newContent = if (existingContent.isBlank()) {
            "$cardHeader\n$snippet"
        } else {
            "$existingContent\n\n---\n$cardHeader\n$snippet"
        }

        fileManager.writeNote(ctsNote.metadata.fileName, newContent)
        val updated = ctsNote.metadata.copy(modifiedAt = System.currentTimeMillis())
        dao.update(updated)
        return updated
    }

    fun countCtsCards(): Flow<Int> = dao.getAllActive().map { notes ->
        val ctsNote = notes.firstOrNull { it.title.trim().equals("circle to search", ignoreCase = true) }
        if (ctsNote == null) {
            0
        } else {
            val content = fileManager.readNote(ctsNote.fileName).trim()
            if (content.isBlank()) 0 else {
                val headerMatches = Regex("""^###\s+🌐""", RegexOption.MULTILINE).findAll(content).count()
                if (headerMatches > 0) headerMatches else 1
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateNote(
        id: String,
        title: String? = null,
        content: String? = null,
        tag: String? = null,
        colorTheme: String? = null,
        cardType: String? = null
    ) {
        val existing = dao.getById(id) ?: return
        val now = System.currentTimeMillis()
        val newTitle = title ?: existing.title

        val effectiveCardType = if (cardType == null && newTitle.trim().equals("todo", ignoreCase = true) && existing.cardType == "modular") {
            val official = dao.getOfficialTodoNote()
            if (official == null || official.id == id) "todo" else existing.cardType
        } else {
            cardType ?: existing.cardType
        }

        val updated = existing.copy(
            title = newTitle,
            tag = tag ?: existing.tag,
            colorTheme = colorTheme ?: existing.colorTheme,
            cardType = effectiveCardType,
            modifiedAt = now
        )

        if (content != null) {
            fileManager.writeNote(existing.fileName, content)
        }

        dao.update(updated)
    }

    suspend fun setReminder(id: String, reminderTime: Long?) {
        dao.updateReminderTime(id, reminderTime)
    }

    suspend fun clearReminder(id: String) {
        dao.updateReminderTime(id, null)
    }

    suspend fun getNotesWithPendingReminders(): List<NoteMetadata> {
        return dao.getNotesWithPendingReminders()
    }

    suspend fun togglePin(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isPinned = !existing.isPinned, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun toggleLock(id: String, isLocked: Boolean) {
        dao.updateLockStatus(id, isLocked)
    }

    suspend fun deleteNote(id: String) {
        // Soft delete note to Trash/Deleted
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = true, isArchived = false, modifiedAt = System.currentTimeMillis()))
        bindingDao?.deleteByNoteId(id)
    }

    suspend fun restoreNote(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = false, isArchived = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun permanentlyDeleteNote(id: String) {
        val existing = dao.getById(id) ?: return
        bindingDao?.deleteByNoteId(id)
        fileManager.deleteNote(existing.fileName)
        dao.delete(existing)
    }

    suspend fun permanentlyDeleteAllDeleted() {
        val deleted = dao.getAllDeletedDirect()
        for (item in deleted) {
            fileManager.deleteNote(item.fileName)
        }
        dao.deletePermanentlyWhereDeleted()
    }

    suspend fun deleteAllNotes() {
        fileManager.deleteAllNotes()
        dao.deleteAll()
    }

    suspend fun getNoteMetadata(id: String): NoteMetadata? {
        return dao.getById(id)
    }

    fun getNoteFile(fileName: String): File {
        return fileManager.getNoteFile(fileName)
    }

    suspend fun getNoteContent(id: String): String {
        val metadata = dao.getById(id) ?: return ""
        return fileManager.readNote(metadata.fileName)
    }

    /**
     * Purges all soft-deleted notes and completely empty/blank notes from database and disk.
     * Returns the total count of purged notes.
     */
    suspend fun purgeDeletedAndBlankNotes(): Int {
        var count = 0
        // 1. Permanently delete all soft-deleted notes
        val deleted = dao.getAllDeletedDirect()
        for (note in deleted) {
            fileManager.deleteNote(note.fileName)
            bindingDao?.deleteByNoteId(note.id)
            dao.delete(note)
            count++
        }

        // 2. Permanently delete any active notes that are completely blank (no title and no content)
        val active = dao.getAllActiveDirect()
        for (note in active) {
            val content = fileManager.readNote(note.fileName).trim()
            val title = note.title.trim()
            val isBlankTitle = title.isEmpty() || title.equals("Untitled", ignoreCase = true)
            val isBlankContent = content.isEmpty()
            if (isBlankTitle && isBlankContent) {
                fileManager.deleteNote(note.fileName)
                bindingDao?.deleteByNoteId(note.id)
                dao.delete(note)
                count++
            }
        }
        return count
    }

    /**
     * Counts how many deleted or blank notes exist in the database.
     */
    suspend fun countDeletedAndBlankNotes(): Int {
        val deletedCount = dao.getAllDeletedDirect().size
        var blankCount = 0
        val active = dao.getAllActiveDirect()
        for (note in active) {
            val content = fileManager.readNote(note.fileName).trim()
            val title = note.title.trim()
            val isBlankTitle = title.isEmpty() || title.equals("Untitled", ignoreCase = true)
            if (isBlankTitle && content.isEmpty()) {
                blankCount++
            }
        }
        return deletedCount + blankCount
    }
}

