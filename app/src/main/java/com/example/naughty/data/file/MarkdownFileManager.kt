package com.example.naughty.data.file

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MarkdownFileManager(context: Context) {

    private val notesDir = File(context.filesDir, "notes").also { it.mkdirs() }

    suspend fun writeNote(fileName: String, content: String) = withContext(Dispatchers.IO) {
        File(notesDir, fileName).writeText(content, Charsets.UTF_8)
    }

    suspend fun readNote(fileName: String): String = withContext(Dispatchers.IO) {
        val file = File(notesDir, fileName)
        if (file.exists()) file.readText(Charsets.UTF_8) else ""
    }

    suspend fun deleteNote(fileName: String) = withContext(Dispatchers.IO) {
        File(notesDir, fileName).delete()
    }

    suspend fun noteExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
        File(notesDir, fileName).exists()
    }

    suspend fun listNotes(): List<File> = withContext(Dispatchers.IO) {
        notesDir.listFiles { file -> file.extension == "md" }?.toList() ?: emptyList()
    }

    suspend fun deleteAllNotes() = withContext(Dispatchers.IO) {
        notesDir.listFiles()?.forEach { it.delete() }
    }

    fun getNoteFile(fileName: String): File {
        return File(notesDir, fileName)
    }
}
