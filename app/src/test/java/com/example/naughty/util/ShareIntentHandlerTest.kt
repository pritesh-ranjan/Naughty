package com.example.naughty.util

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentHandlerTest {

    @Test
    fun testIsShareAction() {
        assertTrue(ShareIntentHandler.isShareAction(Intent.ACTION_SEND))
        assertTrue(ShareIntentHandler.isShareAction(Intent.ACTION_SEND_MULTIPLE))
        assertTrue(ShareIntentHandler.isShareAction(Intent.ACTION_PROCESS_TEXT))

        assertFalse(ShareIntentHandler.isShareAction(Intent.ACTION_MAIN))
        assertFalse(ShareIntentHandler.isShareAction(Intent.ACTION_VIEW))
        assertFalse(ShareIntentHandler.isShareAction(null))
        assertFalse(ShareIntentHandler.isShareAction("android.intent.action.BATTERY_LOW"))
    }

    @Test
    fun testIsUrl() {
        assertTrue(ShareIntentHandler.isUrl("https://example.com"))
        assertTrue(ShareIntentHandler.isUrl("http://example.com/page?id=42"))
        assertTrue(ShareIntentHandler.isUrl("https://kotlinlang.org/docs/home.html"))

        assertFalse(ShareIntentHandler.isUrl("https://example.com Check this out"))
        assertFalse(ShareIntentHandler.isUrl("Not a URL"))
        assertFalse(ShareIntentHandler.isUrl("ftp://files.example.com"))
        assertFalse(ShareIntentHandler.isUrl(""))
    }

    @Test
    fun testExtractTitleFromUrl() {
        val title1 = ShareIntentHandler.extractTitleFromUrl("https://github.com/torvalds/linux")
        assertEquals("github.com • linux", title1)

        val title2 = ShareIntentHandler.extractTitleFromUrl("https://www.android.com")
        assertEquals("android.com", title2)

        val title3 = ShareIntentHandler.extractTitleFromUrl("https://kotlinlang.org/docs/home.html")
        assertEquals("kotlinlang.org • home", title3)

        val title4 = ShareIntentHandler.extractTitleFromUrl("https://example.com/posts/my-favorite-things_2026")
        assertEquals("example.com • my favorite things 2026", title4)

        val fallback = ShareIntentHandler.extractTitleFromUrl("invalid-url")
        assertEquals("Shared Link", fallback)
    }

    @Test
    fun testCleanTitleFromText() {
        // Simple plain line
        assertEquals("Quick thought", ShareIntentHandler.cleanTitleFromText("Quick thought"))

        // Multiple lines - uses first non-empty line
        val multiLine = "\n\nFirst line of note\nSecond line"
        assertEquals("First line of note", ShareIntentHandler.cleanTitleFromText(multiLine))

        // Markdown headers
        assertEquals("Project Architecture", ShareIntentHandler.cleanTitleFromText("# Project Architecture\nSome body"))
        assertEquals("Deep Dive", ShareIntentHandler.cleanTitleFromText("### Deep Dive"))

        // Markdown checklist
        assertEquals("Buy fresh apples", ShareIntentHandler.cleanTitleFromText("- [ ] Buy fresh apples\n- [ ] Milk"))
        assertEquals("Completed task", ShareIntentHandler.cleanTitleFromText("- [x] Completed task"))

        // Markdown bullets & quotes
        assertEquals("Bullet item", ShareIntentHandler.cleanTitleFromText("* Bullet item"))
        assertEquals("Wise quotation", ShareIntentHandler.cleanTitleFromText("> Wise quotation"))

        // Pure URL
        val urlTitle = ShareIntentHandler.cleanTitleFromText("https://en.wikipedia.org/wiki/Kotlin")
        assertEquals("en.wikipedia.org • Kotlin", urlTitle)

        // Truncation for long first line
        val longText = "This is an extremely long title for a note that should definitely be shortened by the title cleaner"
        val truncated = ShareIntentHandler.cleanTitleFromText(longText)
        assertTrue(truncated.length <= 60)
        assertTrue(truncated.endsWith("..."))

        // Blank
        assertEquals("Shared Note", ShareIntentHandler.cleanTitleFromText(""))
    }

    @Test
    fun testDetectCardType() {
        assertEquals("checklist", ShareIntentHandler.detectCardType("- [ ] Task one\n- [ ] Task two"))
        assertEquals("checklist", ShareIntentHandler.detectCardType("- [x] Already done task"))
        assertEquals("modular", ShareIntentHandler.detectCardType("Simple note without checklist"))
        assertEquals("modular", ShareIntentHandler.detectCardType("https://example.com"))
    }

    @Test
    fun testParseTextContent_withSubjectAndText() {
        val result = ShareIntentHandler.parseTextContent(
            subject = "Jetpack Compose Docs",
            text = "https://developer.android.com/compose"
        )
        assertNotNull(result)
        assertEquals("Jetpack Compose Docs", result?.title)
        assertEquals("https://developer.android.com/compose", result?.content)
        assertEquals("modular", result?.cardType)
    }

    @Test
    fun testParseTextContent_withTextOnly() {
        // Plain text
        val textOnly = ShareIntentHandler.parseTextContent(
            subject = null,
            text = "Remember to call doctor at 2pm"
        )
        assertNotNull(textOnly)
        assertEquals("Remember to call doctor at 2pm", textOnly?.title)
        assertEquals("Remember to call doctor at 2pm", textOnly?.content)

        // Web link
        val linkOnly = ShareIntentHandler.parseTextContent(
            subject = "",
            text = "https://github.com/torvalds/linux"
        )
        assertNotNull(linkOnly)
        assertEquals("github.com • linux", linkOnly?.title)
        assertEquals("https://github.com/torvalds/linux", linkOnly?.content)

        // Checklist text
        val checklist = ShareIntentHandler.parseTextContent(
            subject = null,
            text = "- [ ] Milk\n- [ ] Bread\n- [ ] Eggs"
        )
        assertNotNull(checklist)
        assertEquals("Milk", checklist?.title)
        assertEquals("checklist", checklist?.cardType)
    }

    @Test
    fun testParseTextContent_withSubjectOnly() {
        val subjectOnly = ShareIntentHandler.parseTextContent(
            subject = "Meeting follow up",
            text = null
        )
        assertNotNull(subjectOnly)
        assertEquals("Meeting follow up", subjectOnly?.title)
        assertEquals("Meeting follow up", subjectOnly?.content)
    }

    @Test
    fun testParseTextContent_blankInputReturnsNull() {
        assertNull(ShareIntentHandler.parseTextContent(null, null))
        assertNull(ShareIntentHandler.parseTextContent("", "   "))
    }

    @Test
    fun testFormatNoteWithImages_singleImage() {
        val images = listOf("file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg")
        val note = ShareIntentHandler.formatNoteWithImages(
            subject = null,
            text = null,
            localImageUris = images
        )
        assertNotNull(note)
        assertEquals("Photo note", note?.title)
        assertEquals("![Image](file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg)", note?.content)
        assertEquals("modular", note?.cardType)
    }

    @Test
    fun testFormatNoteWithImages_multipleImagesWithCaption() {
        val images = listOf(
            "file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg",
            "file:///data/user/0/com.example.naughty/files/note_images/img_2.jpg"
        )
        val note = ShareIntentHandler.formatNoteWithImages(
            subject = "Vacation Memories",
            text = "Had a wonderful time in Kyoto!",
            localImageUris = images
        )
        assertNotNull(note)
        assertEquals("Vacation Memories", note?.title)
        assertTrue(note!!.content.startsWith("Had a wonderful time in Kyoto!\n\n"))
        assertTrue(note.content.contains("![Image](file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg)"))
        assertTrue(note.content.contains("![Image](file:///data/user/0/com.example.naughty/files/note_images/img_2.jpg)"))
    }

    @Test
    fun testFormatNoteWithImages_multipleImagesNoCaption() {
        val images = listOf(
            "file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg",
            "file:///data/user/0/com.example.naughty/files/note_images/img_2.jpg",
            "file:///data/user/0/com.example.naughty/files/note_images/img_3.jpg"
        )
        val note = ShareIntentHandler.formatNoteWithImages(
            subject = null,
            text = null,
            localImageUris = images
        )
        assertNotNull(note)
        assertEquals("3 Photos", note?.title)
    }

    @Test
    fun testCombineOcrResult_imageOnly() {
        val images = listOf("file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg")
        val result = ShareIntentHandler.combineOcrResult(
            subject = "Receipt",
            accompanyingText = null,
            ocrText = "Coffee $4.50",
            savedImageFileUris = images,
            mode = ImageShareMode.IMAGE_ONLY
        )
        assertNotNull(result)
        assertEquals("Receipt", result?.title)
        assertEquals("![Image](file:///data/user/0/com.example.naughty/files/note_images/img_1.jpg)", result?.content)
    }

    @Test
    fun testCombineOcrResult_ocrOnly_withDetectedText() {
        val result = ShareIntentHandler.combineOcrResult(
            subject = null,
            accompanyingText = "Scanned document note",
            ocrText = "Chapter 1: The Beginning of Wisdom\nIt started on a quiet morning.",
            savedImageFileUris = emptyList(),
            mode = ImageShareMode.OCR_ONLY
        )
        assertNotNull(result)
        assertEquals("Scanned document note", result?.title)
        assertTrue(result!!.content.contains("Chapter 1: The Beginning of Wisdom"))
        assertFalse(result.content.contains("![Image]"))
    }

    @Test
    fun testCombineOcrResult_ocrOnly_fallbackWhenNoText() {
        val images = listOf("file:///data/user/0/com.example.naughty/files/note_images/img_blank.jpg")
        val result = ShareIntentHandler.combineOcrResult(
            subject = null,
            accompanyingText = null,
            ocrText = "",
            savedImageFileUris = images,
            mode = ImageShareMode.OCR_ONLY
        )
        assertNotNull(result)
        assertEquals("Photo note", result?.title)
        assertEquals("![Image](file:///data/user/0/com.example.naughty/files/note_images/img_blank.jpg)", result?.content)
    }

    @Test
    fun testCombineOcrResult_both_withOcrAndImage() {
        val images = listOf("file:///data/user/0/com.example.naughty/files/note_images/img_book.jpg")
        val result = ShareIntentHandler.combineOcrResult(
            subject = "Book Excerpt",
            accompanyingText = "Favorite quote from page 42",
            ocrText = "\"Two roads diverged in a yellow wood.\"",
            savedImageFileUris = images,
            mode = ImageShareMode.BOTH
        )
        assertNotNull(result)
        assertEquals("Book Excerpt", result?.title)
        assertTrue(result!!.content.startsWith("Favorite quote from page 42\n\n\"Two roads diverged in a yellow wood.\""))
        assertTrue(result.content.endsWith("\n\n![Image](file:///data/user/0/com.example.naughty/files/note_images/img_book.jpg)"))
    }

    @Test
    fun testCombineOcrResult_both_checklistDetection() {
        val images = listOf("file:///data/user/0/com.example.naughty/files/note_images/img_whiteboard.jpg")
        val result = ShareIntentHandler.combineOcrResult(
            subject = null,
            accompanyingText = null,
            ocrText = "- [ ] Fix database leak\n- [ ] Deploy to release",
            savedImageFileUris = images,
            mode = ImageShareMode.BOTH
        )
        assertNotNull(result)
        assertEquals("Fix database leak", result?.title)
        assertEquals("checklist", result?.cardType)
        assertTrue(result!!.content.contains("![Image]"))
    }
}
