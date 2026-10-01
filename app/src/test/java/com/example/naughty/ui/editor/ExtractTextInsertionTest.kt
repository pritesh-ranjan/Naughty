package com.example.naughty.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExtractTextInsertionTest {

    @Test
    fun testInsertExtractedTextIntoEmptyNote() {
        val initialContent = ""
        val blocks = EditorBlockParser.parse(initialContent).toMutableList()
        val attachedImages = EditorBlockParser.extractImages(initialContent)

        val extractedText = "Scanned receipt\nTotal: $42.50"

        // Emulate ViewModel insertion logic
        if (blocks.isEmpty() || (blocks.size == 1 && (blocks[0] as? EditorBlock.Text)?.text.isNullOrBlank())) {
            blocks.clear()
            blocks.add(EditorBlock.Text(id = UUID.randomUUID().toString(), text = extractedText.trim()))
        }

        val resultMarkdown = EditorBlockParser.toMarkdown(blocks, attachedImages)
        assertEquals("Scanned receipt\nTotal: $42.50", resultMarkdown)
    }

    @Test
    fun testInsertExtractedTextIntoNoteWithExistingContent() {
        val initialContent = "# Meeting Notes\nDiscussed quarterly goals."
        val blocks = EditorBlockParser.parse(initialContent).toMutableList()
        val attachedImages = EditorBlockParser.extractImages(initialContent)

        val extractedText = "Action Item 1: Prepare slides\nAction Item 2: Send summary"

        val lastBlock = blocks.lastOrNull()
        if (lastBlock is EditorBlock.Text && lastBlock.text.isBlank()) {
            blocks[blocks.lastIndex] = EditorBlock.Text(id = lastBlock.id, text = extractedText.trim())
        } else {
            blocks.add(EditorBlock.Text(id = UUID.randomUUID().toString(), text = extractedText.trim()))
        }

        val resultMarkdown = EditorBlockParser.toMarkdown(blocks, attachedImages)
        val expected = """
            # Meeting Notes
            Discussed quarterly goals.
            Action Item 1: Prepare slides
            Action Item 2: Send summary
        """.trimIndent()

        assertEquals(expected, resultMarkdown)
    }

    @Test
    fun testInsertExtractedTextPreservesAttachedImagesAtEnd() {
        val initialContent = "Here are my notes.\n\n![Image](file:///data/user/0/img1.jpg)\n\n![Image](file:///data/user/0/img2.jpg)"
        val blocks = EditorBlockParser.parse(initialContent).toMutableList()
        val attachedImages = EditorBlockParser.extractImages(initialContent)

        assertEquals(2, attachedImages.size)
        assertEquals("file:///data/user/0/img1.jpg", attachedImages[0])
        assertEquals("file:///data/user/0/img2.jpg", attachedImages[1])

        val extractedText = "Text extracted from photo"
        val lastBlock = blocks.lastOrNull()
        if (lastBlock is EditorBlock.Text) {
            blocks[blocks.lastIndex] = EditorBlock.Text(id = lastBlock.id, text = lastBlock.text.trimEnd())
        }
        blocks.add(EditorBlock.Text(id = UUID.randomUUID().toString(), text = extractedText))

        val resultMarkdown = EditorBlockParser.toMarkdown(blocks, attachedImages)

        assertTrue(resultMarkdown.startsWith("Here are my notes.\nText extracted from photo"))
        assertTrue(resultMarkdown.endsWith("![Image](file:///data/user/0/img1.jpg)\n\n![Image](file:///data/user/0/img2.jpg)"))
    }

    @Test
    fun testInsertExtractedTextAfterTargetBlock() {
        val block1 = EditorBlock.Checklist(id = "chk-1", text = "Buy coffee")
        val block2 = EditorBlock.Checklist(id = "chk-2", text = "Call plumber")
        val blocks = mutableListOf<EditorBlock>(block1, block2)

        val extractedText = "Store Hours: 9 AM - 6 PM"
        val targetId = "chk-1"

        val targetIndex = blocks.indexOfFirst { it.id == targetId }
        val newBlock = EditorBlock.Text(id = UUID.randomUUID().toString(), text = extractedText)
        blocks.add(targetIndex + 1, newBlock)

        assertEquals(3, blocks.size)
        assertEquals("chk-1", blocks[0].id)
        assertEquals(newBlock.id, blocks[1].id)
        assertEquals("Store Hours: 9 AM - 6 PM", (blocks[1] as EditorBlock.Text).text)
        assertEquals("chk-2", blocks[2].id)
    }
}
