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

    @Test
    fun testMultilineChecklistItem_serializationAndParsing() {
        val multilineChecklist = EditorBlock.Checklist(
            text = "Buy ingredients for pasta\nFresh tomatoes and basil\nOlive oil and garlic"
        )
        val markdown = EditorBlockParser.toMarkdown(listOf(multilineChecklist))
        val expectedMarkdown = """
            - [ ] Buy ingredients for pasta
              Fresh tomatoes and basil
              Olive oil and garlic
        """.trimIndent()
        assertEquals(expectedMarkdown, markdown)

        val reparsed = EditorBlockParser.parse(markdown)
        assertEquals(1, reparsed.size)
        assertTrue(reparsed[0] is EditorBlock.Checklist)
        val reparsedItem = reparsed[0] as EditorBlock.Checklist
        assertEquals("Buy ingredients for pasta\nFresh tomatoes and basil\nOlive oil and garlic", reparsedItem.text)
        assertFalse(reparsedItem.isChecked)
    }

    @Test
    fun testMultilineChecklistItemWithReminder() {
        val timestamp = 1750000000000L
        val tag = InlineReminderParser.createReminderTag(timestamp, "Tomorrow, 9:00 AM")
        val input = """
            - [ ] Review quarterly budget
              Verify spreadsheet numbers $tag
            - [ ] Send summary email
        """.trimIndent()

        val blocks = EditorBlockParser.parse(input)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is EditorBlock.Checklist)
        assertTrue(blocks[1] is EditorBlock.Checklist)

        val task1 = blocks[0] as EditorBlock.Checklist
        val task2 = blocks[1] as EditorBlock.Checklist

        assertEquals("Review quarterly budget\nVerify spreadsheet numbers", task1.text)
        assertNotNull(task1.reminder)
        assertEquals(timestamp, task1.reminder?.timestampMillis)

        assertEquals("Send summary email", task2.text)
        assertNull(task2.reminder)

        val reserialized = EditorBlockParser.toMarkdown(blocks)
        assertEquals(input, reserialized)
    }

    @Test
    fun testVoiceNoteParsingAndExtraction() {
        val voiceUri1 = "file:///data/user/0/com.example.naughty/files/note_audio/audio_1.opus"
        val voiceUri2 = "file:///data/user/0/com.example.naughty/files/note_audio/audio_2.m4a"
        val input = """
            Meeting discussion summary

            - [ ] Complete team retro notes

            [🎤 Voice Note]($voiceUri1)
            [Voice Note]($voiceUri2)
        """.trimIndent()

        // 1. Verify extractVoiceNotes
        val voiceNotes = EditorBlockParser.extractVoiceNotes(input)
        assertEquals(2, voiceNotes.size)
        assertEquals(voiceUri1, voiceNotes[0])
        assertEquals(voiceUri2, voiceNotes[1])

        // 2. Verify parse excludes voice note lines from body blocks
        val blocks = EditorBlockParser.parse(input)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is EditorBlock.Text)
        assertTrue(blocks[1] is EditorBlock.Checklist)
        assertEquals("Meeting discussion summary", (blocks[0] as EditorBlock.Text).text.trim())

        // 3. Verify toMarkdown serializes cleanly
        val serialized = EditorBlockParser.toMarkdown(
            blocks = blocks,
            attachedImages = emptyList(),
            attachedVoiceNotes = voiceNotes
        )
        assertTrue(serialized.contains("[🎤 Voice Note]($voiceUri1)"))
        assertTrue(serialized.contains("[🎤 Voice Note]($voiceUri2)"))
    }

    @Test
    fun testVoiceNoteWithImagesAndChecklistsRoundtrip() {
        val imageUri = "file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg"
        val voiceUri = "file:///data/user/0/com.example.naughty/files/note_audio/audio_1.opus"
        val input = """
            Project Kickoff

            - [ ] Align on API contract
            - [x] Create repository

            ![Image]($imageUri)

            [🎤 Voice Note]($voiceUri)
        """.trimIndent()

        val images = EditorBlockParser.extractImages(input)
        val voiceNotes = EditorBlockParser.extractVoiceNotes(input)
        val blocks = EditorBlockParser.parse(input)

        assertEquals(1, images.size)
        assertEquals(imageUri, images[0])

        assertEquals(1, voiceNotes.size)
        assertEquals(voiceUri, voiceNotes[0])

        assertEquals(3, blocks.size) // 1 Text block ("Project Kickoff"), 2 Checklist blocks
        assertTrue(blocks[0] is EditorBlock.Text)
        assertTrue(blocks[1] is EditorBlock.Checklist)
        assertTrue(blocks[2] is EditorBlock.Checklist)

        val reserialized = EditorBlockParser.toMarkdown(blocks, images, voiceNotes)
        assertTrue(reserialized.contains("![Image]($imageUri)"))
        assertTrue(reserialized.contains("[🎤 Voice Note]($voiceUri)"))
    }

    @Test
    fun testNormalizeAmplitudes() {
        val samples = listOf(0.1f, 0.4f, 0.8f, 0.2f, 0.9f)
        val normalized = com.example.naughty.util.AudioRecorderHelper.normalizeAmplitudes(samples, targetCount = 10)
        assertEquals(10, normalized.size)
        assertTrue(normalized.all { it in 0.05f..1.0f })
    }
}

