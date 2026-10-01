package com.example.naughty.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorBlockTest {

    @Test
    fun testFiveTasksWithTwoReminders_isolationAndPersistence() {
        // Construct markdown with 5 tasks, where Task 2 and Task 4 have inline reminders
        val time1 = 1750000000000L
        val time2 = 1760000000000L
        val tag1 = InlineReminderParser.createReminderTag(time1, "Tomorrow, 9:00 AM")
        val tag2 = InlineReminderParser.createReminderTag(time2, "Next week")

        val initialMarkdown = """
            - [ ] Buy grocery supplies
            - [ ] Call doctor for appointment $tag1
            - [ ] Schedule team standup
            - [ ] Submit project report $tag2
            - [ ] Pay electric utility bill
        """.trimIndent()

        // 1. Parse markdown
        val blocks = EditorBlockParser.parse(initialMarkdown)
        assertEquals(5, blocks.size)
        assertTrue(blocks.all { it is EditorBlock.Checklist })

        val task1 = blocks[0] as EditorBlock.Checklist
        val task2 = blocks[1] as EditorBlock.Checklist
        val task3 = blocks[2] as EditorBlock.Checklist
        val task4 = blocks[3] as EditorBlock.Checklist
        val task5 = blocks[4] as EditorBlock.Checklist

        // Verify task text and reminders
        assertEquals("Buy grocery supplies", task1.text)
        assertNull(task1.reminder)

        assertEquals("Call doctor for appointment", task2.text)
        assertNotNull(task2.reminder)
        assertEquals(time1, task2.reminder?.timestampMillis)

        assertEquals("Schedule team standup", task3.text)
        assertNull(task3.reminder)

        assertEquals("Submit project report", task4.text)
        assertNotNull(task4.reminder)
        assertEquals(time2, task4.reminder?.timestampMillis)

        assertEquals("Pay electric utility bill", task5.text)
        assertNull(task5.reminder)

        // Verify all 5 blocks have distinct unique IDs
        val uniqueIds = blocks.map { it.id }.toSet()
        assertEquals(5, uniqueIds.size)

        // 2. Test editing Task 2 with spaces - spaces preserved and reminder untouched
        val updatedTask2 = task2.copy(text = "Call doctor for annual checkup ")
        val updatedList1 = blocks.toMutableList()
        updatedList1[1] = updatedTask2

        // Verify space preserved
        assertEquals("Call doctor for annual checkup ", (updatedList1[1] as EditorBlock.Checklist).text)
        assertEquals(time1, (updatedList1[1] as EditorBlock.Checklist).reminder?.timestampMillis)

        // 3. Test editing Task 1 does not affect any other task (Bug 3 isolation)
        val updatedTask1 = task1.copy(text = "Buy organic vegetables")
        updatedList1[0] = updatedTask1
        assertEquals("Buy organic vegetables", (updatedList1[0] as EditorBlock.Checklist).text)
        assertEquals("Schedule team standup", (updatedList1[2] as EditorBlock.Checklist).text)

        // 4. Toggle completion on Task 4
        val completedTask4 = task4.copy(isChecked = true)
        updatedList1[3] = completedTask4
        assertTrue((updatedList1[3] as EditorBlock.Checklist).isChecked)
        assertEquals(time2, (updatedList1[3] as EditorBlock.Checklist).reminder?.timestampMillis)

        // 5. Serialize to Markdown
        val markdownOutput = EditorBlockParser.toMarkdown(updatedList1)
        val expectedMarkdown = """
            - [ ] Buy organic vegetables
            - [ ] Call doctor for annual checkup  $tag1
            - [ ] Schedule team standup
            - [x] Submit project report $tag2
            - [ ] Pay electric utility bill
        """.trimIndent()
        assertEquals(expectedMarkdown, markdownOutput)

        // 6. Roundtrip re-parse
        val reparsed = EditorBlockParser.parse(markdownOutput)
        assertEquals(5, reparsed.size)
        val reparsedTask2 = reparsed[1] as EditorBlock.Checklist
        val reparsedTask4 = reparsed[3] as EditorBlock.Checklist
        assertEquals("Call doctor for annual checkup", reparsedTask2.text)
        assertEquals(time1, reparsedTask2.reminder?.timestampMillis)
        assertTrue(reparsedTask4.isChecked)
        assertEquals(time2, reparsedTask4.reminder?.timestampMillis)

        // 7. Delete Task 3 (middle task) - leaves 4 tasks, reminders on Task 2 & Task 4 unaffected
        val listWithoutTask3 = reparsed.toMutableList()
        listWithoutTask3.removeAt(2)
        assertEquals(4, listWithoutTask3.size)
        assertEquals("Call doctor for annual checkup", (listWithoutTask3[1] as EditorBlock.Checklist).text)
        assertEquals(time1, (listWithoutTask3[1] as EditorBlock.Checklist).reminder?.timestampMillis)
        assertEquals("Submit project report", (listWithoutTask3[2] as EditorBlock.Checklist).text)
        assertEquals(time2, (listWithoutTask3[2] as EditorBlock.Checklist).reminder?.timestampMillis)
    }

    @Test
    fun testParseAndToMarkdown_mixedContentPreservesOrder() {
        val input = """
            Meeting Agenda:
            Discuss timeline

            - [ ] Prepare slides
            - [x] Send invitations

            Follow-up notes here.
        """.trimIndent()

        val blocks = EditorBlockParser.parse(input)
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is EditorBlock.Text)
        assertTrue(blocks[1] is EditorBlock.Checklist)
        assertTrue(blocks[2] is EditorBlock.Checklist)
        assertTrue(blocks[3] is EditorBlock.Text)

        assertEquals("Meeting Agenda:\nDiscuss timeline\n", (blocks[0] as EditorBlock.Text).text)
        assertEquals("Prepare slides", (blocks[1] as EditorBlock.Checklist).text)
        assertFalse((blocks[1] as EditorBlock.Checklist).isChecked)
        assertEquals("Send invitations", (blocks[2] as EditorBlock.Checklist).text)
        assertTrue((blocks[2] as EditorBlock.Checklist).isChecked)
        assertEquals("\nFollow-up notes here.", (blocks[3] as EditorBlock.Text).text)

        val output = EditorBlockParser.toMarkdown(blocks)
        assertEquals(input, output)
    }

    @Test
    fun testParse_emptyContentGivesSingleEmptyTextBlock() {
        val blocks = EditorBlockParser.parse("")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is EditorBlock.Text)
        assertEquals("", (blocks[0] as EditorBlock.Text).text)

        val markdown = EditorBlockParser.toMarkdown(blocks)
        assertEquals("", markdown)
    }

    @Test
    fun testSpacePreservationInChecklist() {
        val input = "- [ ] drink water "
        val blocks = EditorBlockParser.parse(input)
        assertEquals(1, blocks.size)
        val checklist = blocks[0] as EditorBlock.Checklist
        assertEquals("drink water ", checklist.text)

        val output = EditorBlockParser.toMarkdown(blocks)
        assertEquals("- [ ] drink water ", output)
    }

    @Test
    fun testMultipleEmptyChecklists_haveDistinctIds() {
        val input = "- [ ] \n- [ ] \n- [ ] "
        val blocks = EditorBlockParser.parse(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks.all { it is EditorBlock.Checklist && it.text.isEmpty() })
        val ids = blocks.map { it.id }.toSet()
        assertEquals(3, ids.size)
    }
}
