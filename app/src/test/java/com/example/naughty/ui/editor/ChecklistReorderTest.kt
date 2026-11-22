package com.example.naughty.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistReorderTest {

    @Test
    fun testCanReorderChecklists_requiresAtLeastTwoTodos() {
        assertFalse(canReorderChecklists(emptyList()))
        assertFalse(canReorderChecklists(listOf(EditorBlock.Text(id = "t1", text = "Hello"))))
        assertFalse(
            canReorderChecklists(
                listOf(EditorBlock.Checklist(id = "c1", isChecked = false, text = "Single task"))
            )
        )
        assertFalse(
            canReorderChecklists(
                listOf(
                    EditorBlock.Text(id = "t1", text = "Header"),
                    EditorBlock.Checklist(id = "c1", isChecked = true, text = "Single task"),
                    EditorBlock.Text(id = "t2", text = "Footer")
                )
            )
        )

        assertTrue(
            canReorderChecklists(
                listOf(
                    EditorBlock.Checklist(id = "c1", isChecked = false, text = "First"),
                    EditorBlock.Checklist(id = "c2", isChecked = true, text = "Second")
                )
            )
        )
        assertTrue(
            canReorderChecklists(
                listOf(
                    EditorBlock.Checklist(id = "c1", isChecked = false, text = "First"),
                    EditorBlock.Text(id = "t1", text = "Note body"),
                    EditorBlock.Checklist(id = "c2", isChecked = false, text = "Second"),
                    EditorBlock.Checklist(id = "c3", isChecked = true, text = "Third")
                )
            )
        )
    }

    @Test
    fun testReorderChecklistBlock_rejectsInvalidMoves() {
        val singleChecklist = listOf(
            EditorBlock.Checklist(id = "c1", isChecked = false, text = "Only")
        )
        assertNull(reorderChecklistBlock(singleChecklist, 0, 0))

        val mixed = listOf(
            EditorBlock.Checklist(id = "c1", isChecked = false, text = "One"),
            EditorBlock.Text(id = "t1", text = "Middle"),
            EditorBlock.Checklist(id = "c2", isChecked = true, text = "Two")
        )
        assertNull(reorderChecklistBlock(mixed, 0, 0))
        assertNull(reorderChecklistBlock(mixed, -1, 2))
        assertNull(reorderChecklistBlock(mixed, 0, 3))
        assertNull(reorderChecklistBlock(mixed, 0, 1))
        assertNull(reorderChecklistBlock(mixed, 1, 2))
    }

    @Test
    fun testReorderChecklistBlock_preservesStateAndRoundtripsMarkdown() {
        val reminder = InlineReminder(
            rawTag = InlineReminderParser.createReminderTag(1_750_000_000_000L, "Tomorrow"),
            displayText = "Tomorrow",
            timestampMillis = 1_750_000_000_000L,
            startIndex = 0,
            endIndex = 0
        )
        val c1 = EditorBlock.Checklist(id = "c1", isChecked = false, text = "Task 1", reminder = reminder)
        val c2 = EditorBlock.Checklist(id = "c2", isChecked = true, text = "Task 2")
        val c3 = EditorBlock.Checklist(id = "c3", isChecked = false, text = "Task 3")

        // Downward 0 -> 1
        val downOne = reorderChecklistBlock(listOf(c1, c2, c3), 0, 1)
        assertNotNull(downOne)
        assertEquals(listOf("c2", "c1", "c3"), downOne!!.map { it.id })
        val movedC1 = downOne[1] as EditorBlock.Checklist
        assertFalse(movedC1.isChecked)
        assertEquals("Task 1", movedC1.text)
        assertEquals(1_750_000_000_000L, movedC1.reminder?.timestampMillis)

        // Downward 0 -> 2
        val downTwo = reorderChecklistBlock(listOf(c1, c2, c3), 0, 2)
        assertNotNull(downTwo)
        assertEquals(listOf("c2", "c3", "c1"), downTwo!!.map { it.id })

        // Upward 2 -> 0
        val upTwo = reorderChecklistBlock(listOf(c1, c2, c3), 2, 0)
        assertNotNull(upTwo)
        assertEquals(listOf("c3", "c1", "c2"), upTwo!!.map { it.id })

        // Upward 2 -> 1
        val upOne = reorderChecklistBlock(listOf(c1, c2, c3), 2, 1)
        assertNotNull(upOne)
        assertEquals(listOf("c1", "c3", "c2"), upOne!!.map { it.id })

        // Across intervening Text block + markdown roundtrip
        val t1 = EditorBlock.Text(id = "t1", text = "Section break")
        val acrossText = reorderChecklistBlock(listOf(c1, t1, c2), 0, 2)
        assertNotNull(acrossText)
        assertEquals(listOf("t1", "c2", "c1"), acrossText!!.map { it.id })

        val markdown = EditorBlockParser.toMarkdown(acrossText)
        val reparsed = EditorBlockParser.parse(markdown)
        assertEquals(3, reparsed.size)
        assertTrue(reparsed[0] is EditorBlock.Text)
        assertEquals("Section break", (reparsed[0] as EditorBlock.Text).text)
        assertTrue((reparsed[1] as EditorBlock.Checklist).isChecked)
        assertEquals("Task 2", (reparsed[1] as EditorBlock.Checklist).text)
        assertFalse((reparsed[2] as EditorBlock.Checklist).isChecked)
        assertEquals("Task 1", (reparsed[2] as EditorBlock.Checklist).text)
        assertEquals(1_750_000_000_000L, (reparsed[2] as EditorBlock.Checklist).reminder?.timestampMillis)
    }

    @Test
    fun testFindTargetChecklistSwapIndex_crossingAndMixedHeightNoOscillation() {
        val c1 = EditorBlock.Checklist(id = "c1", isChecked = false, text = "Short")
        val t1 = EditorBlock.Text(id = "t1", text = "Spacer")
        val c2 = EditorBlock.Checklist(id = "c2", isChecked = false, text = "Tall")

        // Single checklist returns null
        assertNull(
            findTargetChecklistSwapIndex(
                blocks = listOf(c1, t1),
                currentIndex = 0,
                draggedCenterY = 200f,
                boundsById = mapOf("c1" to BlockLayoutBounds(0f, 40f), "t1" to BlockLayoutBounds(40f, 30f))
            )
        )

        // c1 (top=0, height=40 -> center=20), t1 (top=40, height=30), c2 (top=70, height=120 -> center=130)
        val initialBlocks = listOf(c1, t1, c2)
        val initialBounds = mapOf(
            "c1" to BlockLayoutBounds(0f, 40f),
            "t1" to BlockLayoutBounds(40f, 30f),
            "c2" to BlockLayoutBounds(70f, 120f)
        )

        // Before crossing c2's centerY (130f) -> no swap
        assertNull(findTargetChecklistSwapIndex(initialBlocks, 0, 125f, initialBounds))

        // Dragging down past c2's centerY (130f) -> swaps across t1 to index 2
        val targetDown = findTargetChecklistSwapIndex(initialBlocks, 0, 131f, initialBounds)
        assertEquals(2, targetDown)

        // Simulate post-swap layout where c2 (height=120) is at index 1 (below t1), and c1 (height=40) is at index 2!
        // Order after removeAt(0) + add(2, c1): [t1, c2, c1]
        // t1: 0..30, c2: 30..150 (center=90), c1: 150..190 (center=170)
        val postSwapBlocks = reorderChecklistBlock(initialBlocks, 0, 2)!!
        val postSwapBounds = mapOf(
            "t1" to BlockLayoutBounds(0f, 30f),
            "c2" to BlockLayoutBounds(30f, 120f), // center = 90
            "c1" to BlockLayoutBounds(150f, 40f)
        )
        // Pointer hasn't moved (draggedCenterY still 131f > c2.centerY 90f), so MUST NOT oscillate back!
        assertNull(findTargetChecklistSwapIndex(postSwapBlocks, 2, 131f, postSwapBounds))

        // Now drag upward past c2's new centerY (90f) -> swaps back up to index 1
        val targetUp = findTargetChecklistSwapIndex(postSwapBlocks, 2, 89f, postSwapBounds)
        assertEquals(1, targetUp)
    }
}
