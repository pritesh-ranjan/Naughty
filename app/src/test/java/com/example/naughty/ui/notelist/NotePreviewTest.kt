package com.example.naughty.ui.notelist

import com.example.naughty.data.local.NoteMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePreviewTest {

    private fun sampleNote(
        id: String = "1",
        title: String = "Test Title",
        isPinned: Boolean = false,
        isLocked: Boolean = false,
        cardType: String = "modular"
    ) = NoteMetadata(
        id = id,
        title = title,
        createdAt = 1000L,
        modifiedAt = 2000L,
        isPinned = isPinned,
        isLocked = isLocked,
        cardType = cardType,
        fileName = "$id.md"
    )

    @Test
    fun testTitleCleanup() {
        val noteUntitled = sampleNote(title = "Untitled")
        val preview1 = NotePreview.from(noteUntitled, "Some text")
        assertEquals("", preview1.title)

        val noteNewThought = sampleNote(title = "New thought")
        val preview2 = NotePreview.from(noteNewThought, "Some text")
        assertEquals("", preview2.title)

        val noteReal = sampleNote(title = "Shopping List")
        val preview3 = NotePreview.from(noteReal, "Apples\nBananas")
        assertEquals("Shopping List", preview3.title)
    }

    @Test
    fun testChecklistParsing() {
        val content = """
            - [ ] Buy milk
            - [x] Walk dog
            - [ ] Call Alice
            - [ ] Clean kitchen
            - [ ] Read book
        """.trimIndent()
        val note = sampleNote(cardType = "checklist")
        val preview = NotePreview.from(note, content)

        assertTrue(preview.isTodo)
        assertEquals(5, preview.todoTotalCount)
        assertEquals(1, preview.todoDoneCount)
        assertEquals(4, preview.checklistItems.size)
        assertEquals(1, preview.remainingChecklistCount)
        assertFalse(preview.checklistItems[0].isChecked)
        assertTrue(preview.checklistItems[1].isChecked)
    }

    @Test
    fun testLockedNoteHidesContent() {
        val content = "Super secret password: 12345\n![image](file://secret.jpg)"
        val note = sampleNote(isLocked = true)
        val preview = NotePreview.from(note, content)

        assertEquals("", preview.textSnippet)
        assertNull(preview.imageRef)
        assertNull(preview.voiceNoteRef)
    }
}
