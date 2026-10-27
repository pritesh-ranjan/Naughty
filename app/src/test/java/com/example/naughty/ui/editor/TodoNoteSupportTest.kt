package com.example.naughty.ui.editor

import com.example.naughty.data.local.NoteMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TodoNoteSupportTest {

    private val candidateTitles = setOf("todo", "to-do", "todos", "todo list", "to-do list")

    private fun isCandidateTodoTitle(title: String): Boolean {
        return candidateTitles.contains(title.trim().lowercase())
    }

    @Test
    fun testCandidateTodoTitleMatching() {
        assertTrue(isCandidateTodoTitle("Todo"))
        assertTrue(isCandidateTodoTitle("todo"))
        assertTrue(isCandidateTodoTitle("TODO"))
        assertTrue(isCandidateTodoTitle("  To-Do  "))
        assertTrue(isCandidateTodoTitle("todos"))
        assertTrue(isCandidateTodoTitle("Todo List"))
        assertTrue(isCandidateTodoTitle("to-do list"))

        assertFalse(isCandidateTodoTitle("Grocery List"))
        assertFalse(isCandidateTodoTitle("Meeting with Tod"))
        assertFalse(isCandidateTodoTitle("Tomorrow"))
        assertFalse(isCandidateTodoTitle("Todo tomorrow"))
    }

    @Test
    fun testConvertPlainTextBlocksToTodos() {
        val blocks = listOf(
            EditorBlock.Text(
                id = "text-1",
                text = "Buy milk\n\nCall electrician\nPay rent"
            ),
            EditorBlock.Checklist(
                id = "check-1",
                isChecked = true,
                text = "File taxes"
            )
        )

        // Simulate conversion logic from NoteEditorViewModel
        val newBlocks = mutableListOf<EditorBlock>()
        for (block in blocks) {
            when (block) {
                is EditorBlock.Checklist -> newBlocks.add(block)
                is EditorBlock.Text -> {
                    val lines = block.text.lines()
                    for (line in lines) {
                        if (line.isNotBlank()) {
                            newBlocks.add(
                                EditorBlock.Checklist(
                                    id = UUID.randomUUID().toString(),
                                    isChecked = false,
                                    text = line.trim()
                                )
                            )
                        }
                    }
                }
            }
        }

        assertEquals(4, newBlocks.size)
        assertTrue(newBlocks.all { it is EditorBlock.Checklist })

        val item1 = newBlocks[0] as EditorBlock.Checklist
        val item2 = newBlocks[1] as EditorBlock.Checklist
        val item3 = newBlocks[2] as EditorBlock.Checklist
        val item4 = newBlocks[3] as EditorBlock.Checklist

        assertEquals("Buy milk", item1.text)
        assertFalse(item1.isChecked)

        assertEquals("Call electrician", item2.text)
        assertFalse(item2.isChecked)

        assertEquals("Pay rent", item3.text)
        assertFalse(item3.isChecked)

        assertEquals("File taxes", item4.text)
        assertTrue(item4.isChecked)
    }

    @Test
    fun testEnsureTrailingTodoItem_whenEmptyChecklistExistsAtEnd() {
        val blocks = mutableListOf<EditorBlock>(
            EditorBlock.Checklist(id = "task-1", isChecked = false, text = "Do dishes"),
            EditorBlock.Checklist(id = "empty-task", isChecked = false, text = "   ")
        )

        val lastBlock = blocks.lastOrNull()
        val focusedId = if (lastBlock is EditorBlock.Checklist && lastBlock.text.isBlank()) {
            lastBlock.id
        } else {
            val newId = UUID.randomUUID().toString()
            blocks.add(EditorBlock.Checklist(id = newId, isChecked = false, text = ""))
            newId
        }

        assertEquals("empty-task", focusedId)
        assertEquals(2, blocks.size)
    }

    @Test
    fun testEnsureTrailingTodoItem_whenNoEmptyChecklistAtEnd() {
        val blocks = mutableListOf<EditorBlock>(
            EditorBlock.Checklist(id = "task-1", isChecked = false, text = "Do dishes")
        )

        val lastBlock = blocks.lastOrNull()
        val focusedId = if (lastBlock is EditorBlock.Checklist && lastBlock.text.isBlank()) {
            lastBlock.id
        } else {
            val newId = "new-task-id"
            blocks.add(EditorBlock.Checklist(id = newId, isChecked = false, text = ""))
            newId
        }

        assertEquals("new-task-id", focusedId)
        assertEquals(2, blocks.size)
        assertEquals("", (blocks[1] as EditorBlock.Checklist).text)
    }

    @Test
    fun testTodoNoteCardTypeFiltering() {
        val todoNote = NoteMetadata(
            id = "todo-1",
            title = "Todo",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "todo-1.md",
            cardType = "todo"
        )
        val regularNote = NoteMetadata(
            id = "note-1",
            title = "Thoughts",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "note-1.md",
            cardType = "modular"
        )
        val checklistNote = NoteMetadata(
            id = "check-1",
            title = "Weekend Tasks",
            createdAt = 1000L,
            modifiedAt = 1000L,
            fileName = "check-1.md",
            cardType = "checklist"
        )

        val allNotes = listOf(todoNote, regularNote, checklistNote)

        // Filter for Tasks tab (page 1)
        val tasksTab = allNotes.filter { note ->
            note.cardType == "todo" || note.cardType == "checklist"
        }
        assertEquals(2, tasksTab.size)
        assertTrue(tasksTab.any { it.id == "todo-1" })
        assertTrue(tasksTab.any { it.id == "check-1" })
        assertFalse(tasksTab.any { it.id == "note-1" })

        // Filter for Notes tab (page 2)
        val notesTab = allNotes.filter { note ->
            note.cardType != "todo" && note.cardType != "checklist"
        }
        assertEquals(1, notesTab.size)
        assertEquals("note-1", notesTab[0].id)
    }
}
