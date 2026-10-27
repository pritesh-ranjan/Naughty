package com.example.naughty.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CtsCardFormattingTest {

    private fun formatCard(sourceAppName: String, timeStr: String, text: String, existingContent: String): String {
        val cardHeader = "### 🌐 $sourceAppName • $timeStr"
        val snippet = text.trim()
        val trimmedExisting = existingContent.trim()
        return if (trimmedExisting.isBlank()) {
            "$cardHeader\n$snippet"
        } else {
            "$trimmedExisting\n\n---\n$cardHeader\n$snippet"
        }
    }

    private fun countCards(content: String): Int {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return 0
        val headerMatches = Regex("""^###\s+🌐""", RegexOption.MULTILINE).findAll(trimmed).count()
        return if (headerMatches > 0) headerMatches else 1
    }

    @Test
    fun testFirstCardFormatting() {
        val formatted = formatCard("Chrome", "8:15 PM", "Sample text from web", "")
        assertEquals("### 🌐 Chrome • 8:15 PM\nSample text from web", formatted)
        assertEquals(1, countCards(formatted))
    }

    @Test
    fun testSuccessiveCardAppends() {
        var content = formatCard("Chrome", "8:15 PM", "First snippet", "")
        assertEquals(1, countCards(content))

        content = formatCard("Twitter", "8:20 PM", "Second snippet", content)
        assertEquals(2, countCards(content))
        assertTrue(content.contains("### 🌐 Chrome • 8:15 PM"))
        assertTrue(content.contains("---"))
        assertTrue(content.contains("### 🌐 Twitter • 8:20 PM"))

        content = formatCard("Instagram", "8:25 PM", "Third snippet", content)
        assertEquals(3, countCards(content))
    }

    @Test
    fun testEmptyContentCardCount() {
        assertEquals(0, countCards(""))
        assertEquals(0, countCards("   \n\n  "))
    }
}
