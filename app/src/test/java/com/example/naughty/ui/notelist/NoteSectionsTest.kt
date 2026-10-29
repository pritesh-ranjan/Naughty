package com.example.naughty.ui.notelist

import com.example.naughty.data.local.NoteMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NoteSectionsTest {

    private fun createNote(
        id: String,
        title: String,
        modifiedAt: Long,
        isPinned: Boolean = false
    ): NoteMetadata {
        return NoteMetadata(
            id = id,
            title = title,
            createdAt = modifiedAt,
            modifiedAt = modifiedAt,
            isPinned = isPinned,
            fileName = "$id.md"
        )
    }

    @Test
    fun testEmptyNotesReturnsEmpty() {
        val result = NoteSections.bucket(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testPinnedNotesAlwaysFirst() {
        val now = System.currentTimeMillis()
        val note1 = createNote("1", "Regular note", now, isPinned = false)
        val note2 = createNote("2", "Pinned note", now - 100000, isPinned = true)

        val result = NoteSections.bucket(listOf(note1, note2), now)
        assertEquals("Pinned", result.first().first)
        assertEquals(1, result.first().second.size)
        assertEquals("2", result.first().second.first().id)
    }

    @Test
    fun testTodayAndYesterdayBucketing() {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -10)
        val older = cal.timeInMillis

        val noteToday = createNote("1", "Today note", now)
        val noteYesterday = createNote("2", "Yesterday note", yesterday)
        val noteOlder = createNote("3", "Older note", older)

        val result = NoteSections.bucket(listOf(noteToday, noteYesterday, noteOlder), now)
        val sectionTitles = result.map { it.first }

        assertTrue(sectionTitles.contains("Today"))
        assertTrue(sectionTitles.contains("Yesterday"))
    }
}
