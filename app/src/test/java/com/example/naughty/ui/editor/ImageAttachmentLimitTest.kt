package com.example.naughty.ui.editor

import com.example.naughty.util.ImageOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageAttachmentLimitTest {

    private fun fakeUris(count: Int, prefix: String = "img"): List<String> =
        (1..count).map { "file:///data/user/0/com.example.naughty/files/note_images/${prefix}_$it.jpg" }

    @Test
    fun testMaxImagesConstantIsTen() {
        assertEquals(10, ImageOptimizer.MAX_IMAGES_PER_NOTE)
    }

    @Test
    fun testCalculateInSampleSize() {
        assertEquals(1, ImageOptimizer.calculateInSampleSize(800, 600))
        assertEquals(1, ImageOptimizer.calculateInSampleSize(2400, 1800))
        assertEquals(2, ImageOptimizer.calculateInSampleSize(2401, 1800))
        assertEquals(4, ImageOptimizer.calculateInSampleSize(6000, 4000))
    }

    @Test
    fun testAddMoreThanTwoImagesSucceeds() {
        val (content, result) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = "Trip notes",
            imageUris = fakeUris(3)
        )
        assertEquals(3, result.addedCount)
        assertEquals(0, result.skippedDueToLimit)
        assertEquals(3, result.totalCount)

        val extracted = EditorBlockParser.extractImages(content)
        assertEquals(3, extracted.size)
        assertTrue(content.startsWith("Trip notes\n\n"))
    }

    @Test
    fun testBatchAddTenImagesAtOnceSucceeds() {
        val (content, result) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = "",
            imageUris = fakeUris(10)
        )
        assertEquals(10, result.addedCount)
        assertEquals(0, result.skippedDueToLimit)
        assertEquals(10, result.totalCount)
        assertEquals(10, EditorBlockParser.extractImages(content).size)
        assertEquals(0, NoteEditorViewModel.remainingImageSlots(content))
    }

    @Test
    fun testBatchAddTwelveImagesCapsAtTen() {
        val (content, result) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = "",
            imageUris = fakeUris(12)
        )
        assertEquals(10, result.addedCount)
        assertEquals(2, result.skippedDueToLimit)
        assertEquals(10, result.totalCount)

        val extracted = EditorBlockParser.extractImages(content)
        assertEquals(10, extracted.size)
        assertEquals("file:///data/user/0/com.example.naughty/files/note_images/img_10.jpg", extracted.last())
    }

    @Test
    fun testIncrementalAddWhenEightExistAddsOnlyTwo() {
        val (initialContent, initialResult) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = "Meeting whiteboard",
            imageUris = fakeUris(8, prefix = "first")
        )
        assertEquals(8, initialResult.totalCount)
        assertEquals(2, NoteEditorViewModel.remainingImageSlots(initialContent))

        val (updatedContent, secondResult) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = initialContent,
            imageUris = fakeUris(4, prefix = "second")
        )
        assertEquals(2, secondResult.addedCount)
        assertEquals(2, secondResult.skippedDueToLimit)
        assertEquals(10, secondResult.totalCount)
        assertEquals(10, EditorBlockParser.extractImages(updatedContent).size)
    }

    @Test
    fun testAddImagesWhenAlreadyAtLimitAddsZero() {
        val (fullContent, _) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = "Full gallery",
            imageUris = fakeUris(10)
        )
        val (afterContent, result) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = fullContent,
            imageUris = fakeUris(3, prefix = "extra")
        )
        assertEquals(0, result.addedCount)
        assertEquals(3, result.skippedDueToLimit)
        assertEquals(10, result.totalCount)
        assertEquals(fullContent, afterContent)
    }

    @Test
    fun testRemovingOneImageFromTenFreesOneSlot() {
        val uris = fakeUris(10)
        val (fullContent, _) = NoteEditorViewModel.appendImagesWithLimit("", uris)
        assertEquals(0, NoteEditorViewModel.remainingImageSlots(fullContent))

        val removeRegex = Regex("!\\[.*?\\]\\(" + Regex.escape(uris[4]) + "\\)\n?")
        val afterRemoval = fullContent.replace(removeRegex, "").trim()
        assertEquals(1, NoteEditorViewModel.remainingImageSlots(afterRemoval))

        val (finalContent, addOneResult) = NoteEditorViewModel.appendImagesWithLimit(
            currentContent = afterRemoval,
            imageUris = fakeUris(2, prefix = "new")
        )
        assertEquals(1, addOneResult.addedCount)
        assertEquals(1, addOneResult.skippedDueToLimit)
        assertEquals(10, addOneResult.totalCount)
        assertEquals(10, EditorBlockParser.extractImages(finalContent).size)
    }

    @Test
    fun testTenImagesRoundtripWithTextAndChecklists() {
        val uris = fakeUris(10)
        val blocks = listOf(
            EditorBlock.Text(id = "t1", text = "Project inspection"),
            EditorBlock.Checklist(id = "c1", isChecked = false, text = "Review photos")
        )
        val markdown = EditorBlockParser.toMarkdown(blocks, attachedImages = uris)
        val parsedBlocks = EditorBlockParser.parse(markdown)
        val parsedImages = EditorBlockParser.extractImages(markdown)

        assertEquals(2, parsedBlocks.size)
        assertEquals(10, parsedImages.size)
        assertEquals(uris, parsedImages)
    }
}
