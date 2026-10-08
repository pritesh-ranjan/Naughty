package com.example.naughty.data.repository

import com.example.naughty.data.file.MarkdownFileManager
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.local.NoteMetadataDao
import com.example.naughty.ui.theme.NoteThemeRegistry
import kotlinx.coroutines.flow.Flow
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
    private val fileManager: MarkdownFileManager
) {

    fun getAllActive(): Flow<List<NoteMetadata>> = dao.getAllActive()

    fun getAllArchived(): Flow<List<NoteMetadata>> = dao.getAllArchived()

    fun getAllDeleted(): Flow<List<NoteMetadata>> = dao.getAllDeleted()

    fun countActive(): Flow<Int> = dao.countActive()

    fun countArchived(): Flow<Int> = dao.countArchived()

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

        val metadata = NoteMetadata(
            id = id,
            title = title.ifBlank { "Untitled" },
            createdAt = now,
            modifiedAt = now,
            isPinned = isPinned,
            fileName = fileName,
            tag = tag,
            colorTheme = assignedTheme,
            cardType = cardType,
            isLocked = isLocked
        )

        fileManager.writeNote(fileName, content)
        dao.insert(metadata)
        return metadata
    }

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

        val updated = existing.copy(
            title = title ?: existing.title,
            tag = tag ?: existing.tag,
            colorTheme = colorTheme ?: existing.colorTheme,
            cardType = cardType ?: existing.cardType,
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

    suspend fun toggleArchive(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isArchived = !existing.isArchived, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun unarchiveNote(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isArchived = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun archiveNote(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isArchived = true, isDeleted = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: String) {
        // Soft delete note to Trash/Deleted
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = true, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun restoreNote(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = false, isArchived = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun permanentlyDeleteNote(id: String) {
        val existing = dao.getById(id) ?: return
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

    suspend fun permanentlyDeleteAllArchived() {
        val archived = dao.getAllArchivedDirect()
        for (item in archived) {
            fileManager.deleteNote(item.fileName)
        }
        dao.deletePermanentlyWhereArchived()
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
}
