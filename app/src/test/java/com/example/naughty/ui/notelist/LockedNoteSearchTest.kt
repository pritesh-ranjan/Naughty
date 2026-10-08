package com.example.naughty.ui.notelist

import com.example.naughty.data.local.NoteMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedNoteSearchTest {

    private fun filterNotes(
        notes: List<NoteMetadata>,
        rawContents: Map<String, String>,
        query: String
    ): List<NoteMetadata> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return notes
        return notes.filter { note ->
            val content = rawContents[note.id] ?: ""
            note.title.lowercase().contains(q) || content.lowercase().contains(q)
        }
    }

    @Test
    fun testLockedNoteSearchByTitle() {
        val lockedNote = NoteMetadata(
            id = "locked-1",
            title = "Secret Passwords",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "locked-1.md",
            isLocked = true
        )
        val regularNote = NoteMetadata(
            id = "reg-1",
            title = "Grocery List",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "reg-1.md",
            isLocked = false
        )

        val notes = listOf(lockedNote, regularNote)
        val contents = mapOf(
            "locked-1" to "Banking pin is 1234",
            "reg-1" to "Apples, Milk, Bread"
        )

        val results = filterNotes(notes, contents, "passwords")
        assertEquals(1, results.size)
        assertEquals("locked-1", results[0].id)
        assertTrue(results[0].isLocked)
    }

    @Test
    fun testLockedNoteSearchByContent() {
        val lockedNote = NoteMetadata(
            id = "locked-1",
            title = "Personal Vault",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "locked-1.md",
            isLocked = true
        )
        val regularNote = NoteMetadata(
            id = "reg-1",
            title = "Workout Routine",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "reg-1.md",
            isLocked = false
        )

        val notes = listOf(lockedNote, regularNote)
        val contents = mapOf(
            "locked-1" to "Hidden safe combination is alpha-bravo-42",
            "reg-1" to "Pushups and running"
        )

        // Search by a word that only exists in the locked note's hidden content
        val results = filterNotes(notes, contents, "alpha-bravo")
        assertEquals(1, results.size)
        assertEquals("locked-1", results[0].id)
        assertTrue(results[0].isLocked)
    }

    @Test
    fun testLockedNoteSearchCaseInsensitive() {
        val lockedNote = NoteMetadata(
            id = "locked-1",
            title = "Confidential Plan",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "locked-1.md",
            isLocked = true
        )
        val notes = listOf(lockedNote)
        val contents = mapOf("locked-1" to "Top Secret Project Phoenix Launch")

        val resultsTitle = filterNotes(notes, contents, "cOnFiDeNtIaL")
        assertEquals(1, resultsTitle.size)

        val resultsContent = filterNotes(notes, contents, "pHoEnIx")
        assertEquals(1, resultsContent.size)
    }

    @Test
    fun testNonMatchingQueryReturnsEmpty() {
        val lockedNote = NoteMetadata(
            id = "locked-1",
            title = "Secret Notes",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "locked-1.md",
            isLocked = true
        )
        val notes = listOf(lockedNote)
        val contents = mapOf("locked-1" to "Only private details here")

        val results = filterNotes(notes, contents, "nonexistent keyword xyz")
        assertTrue(results.isEmpty())
    }
}
