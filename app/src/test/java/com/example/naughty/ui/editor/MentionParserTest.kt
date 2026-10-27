package com.example.naughty.ui.editor

import com.example.naughty.data.mention.MentionItem
import com.example.naughty.data.mention.MentionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionParserTest {

    @Test
    fun testParseMentions_contact() {
        val text = "Call [@Alice Smith](mention:contact:42) regarding the budget."
        val mentions = MentionParser.parseMentions(text)

        assertEquals(1, mentions.size)
        val mention = mentions[0]
        assertEquals("Alice Smith", mention.title)
        assertEquals(MentionType.CONTACT, mention.type)
        assertEquals("42", mention.id)
        assertEquals("[@Alice Smith](mention:contact:42)", mention.rawTag)
    }

    @Test
    fun testParseMentions_app() {
        val text = "Check out [@Spotify](mention:app:com.spotify.music) for new tracks"
        val mentions = MentionParser.parseMentions(text)

        assertEquals(1, mentions.size)
        val mention = mentions[0]
        assertEquals("Spotify", mention.title)
        assertEquals(MentionType.APP, mention.type)
        assertEquals("com.spotify.music", mention.id)
    }

    @Test
    fun testParseMentions_calendar() {
        val text = "- [ ] Prepare slides for [@Product Sync](mention:calendar:105)"
        val mentions = MentionParser.parseMentions(text)

        assertEquals(1, mentions.size)
        val mention = mentions[0]
        assertEquals("Product Sync", mention.title)
        assertEquals(MentionType.CALENDAR, mention.type)
        assertEquals("105", mention.id)
    }

    @Test
    fun testParseMentions_multiple() {
        val text = "Ping [@Bob](mention:contact:1) on [@Slack](mention:app:com.slack) before [@Sprint Review](mention:calendar:99)"
        val mentions = MentionParser.parseMentions(text)

        assertEquals(3, mentions.size)
        assertEquals("Bob", mentions[0].title)
        assertEquals(MentionType.CONTACT, mentions[0].type)
        assertEquals("Slack", mentions[1].title)
        assertEquals(MentionType.APP, mentions[1].type)
        assertEquals("Sprint Review", mentions[2].title)
        assertEquals(MentionType.CALENDAR, mentions[2].type)
    }

    @Test
    fun testDetectMentionTrigger_startOfLine() {
        val text = "@"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = 1)

        assertNotNull(trigger)
        assertEquals("", trigger!!.query)
        assertEquals(0, trigger.startIndex)
        assertEquals(1, trigger.endIndex)
    }

    @Test
    fun testDetectMentionTrigger_withQuery() {
        val text = "Call @Joh"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = text.length)

        assertNotNull(trigger)
        assertEquals("Joh", trigger!!.query)
        assertEquals(5, trigger.startIndex)
        assertEquals(text.length, trigger.endIndex)
    }

    @Test
    fun testDetectMentionTrigger_ignoreEmail() {
        val text = "Send email to user@domain.com"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = text.length)

        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_newlineCancels() {
        val text = "Hello @John\nHow are you"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = text.length)

        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_tooLongIgnored() {
        val text = "@" + "a".repeat(35)
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = text.length)

        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_spaceAfterAt_returnsNull() {
        val text = "Hello @ world"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = 8)
        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_multipleSpaces_returnsNull() {
        val text = "Call @John Doe Smith"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = text.length)
        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_punctuation_returnsNull() {
        val text = "Hello @John, how are you?"
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = 12)
        assertNull(trigger)
    }

    @Test
    fun testDetectMentionTrigger_secondCharTyped_hasQueryLength1() {
        // 1st char: '@'
        val atAlone = MentionParser.detectMentionTrigger("Meet @", cursorPosition = 6)
        assertNotNull(atAlone)
        assertEquals("", atAlone!!.query)
        assertEquals(0, atAlone.query.length) // query length is 0, suggestion sheet hidden

        // 2nd char typed: 'a'
        val secondChar = MentionParser.detectMentionTrigger("Meet @a", cursorPosition = 7)
        assertNotNull(secondChar)
        assertEquals("a", secondChar!!.query)
        assertTrue(secondChar.query.length >= 1) // query length >= 1, starts suggesting!
    }

    @Test
    fun testDetectMentionTrigger_cursorInMiddle_expandsEndIndex() {
        val text = "Call @Johnathan today"
        // Cursor placed at offset 9 (right after "@Joh")
        val trigger = MentionParser.detectMentionTrigger(text, cursorPosition = 9)

        assertNotNull(trigger)
        assertEquals("Joh", trigger!!.query)
        assertEquals(5, trigger.startIndex)
        // End index should extend through 'Johnathan' to offset 15
        assertEquals(15, trigger.endIndex)
    }

    @Test
    fun testCreateMentionTag() {
        val contactItem = MentionItem(
            id = "123",
            type = MentionType.CONTACT,
            title = "John Doe",
            subtitle = "+1 555-0100"
        )
        val tag = MentionParser.createMentionTag(contactItem)
        assertEquals("[@John Doe](mention:contact:123)", tag)

        val appItem = MentionItem(
            id = "com.google.android.apps.maps",
            type = MentionType.APP,
            title = "Google Maps"
        )
        assertEquals("[@Google Maps](mention:app:com.google.android.apps.maps)", MentionParser.createMentionTag(appItem))

        val calendarItem = MentionItem(
            id = "456",
            type = MentionType.CALENDAR,
            title = "Team Lunch"
        )
        assertEquals("[@Team Lunch](mention:calendar:456)", MentionParser.createMentionTag(calendarItem))
    }

    @Test
    fun testCleanMentions() {
        val text = "Meeting with [@Alice](mention:contact:1) on [@Zoom](mention:app:us.zoom.videomeetings)"
        val cleanedWithAt = MentionParser.cleanMentions(text, keepAtPrefix = true)
        assertEquals("Meeting with @Alice on @Zoom", cleanedWithAt)

        val cleanedWithoutAt = MentionParser.cleanMentions(text, keepAtPrefix = false)
        assertEquals("Meeting with Alice on Zoom", cleanedWithoutAt)
    }

    @Test
    fun testHasMentions() {
        assertTrue(MentionParser.hasMentions("Call [@Doc](mention:contact:9)"))
        assertFalse(MentionParser.hasMentions("Regular text with no mentions"))
    }

    @Test
    fun testBuildInlineAnnotatedString_hidesMarkdownCode() {
        val raw = "talk to [@Mom](mention:contact:34) today"
        val annotated = MentionParser.buildInlineAnnotatedString(raw)

        // Verifies the markdown code is completely hidden
        assertEquals("talk to @Mom today", annotated.text)

        // Verifies the annotation is stored for tap detection
        val annotations = annotated.getStringAnnotations("mention", 8, 12)
        assertEquals(1, annotations.size)
        assertEquals("contact:34:Mom", annotations[0].item)
    }

    @Test
    fun testParseAnnotation() {
        val parsed = MentionParser.parseAnnotation("contact:34:Mom")
        assertNotNull(parsed)
        assertEquals("Mom", parsed!!.title)
        assertEquals("34", parsed.id)
        assertEquals(MentionType.CONTACT, parsed.type)
        assertEquals("[@Mom](mention:contact:34)", parsed.rawTag)
    }

    @Test
    fun testMentionVisualTransformation_filter() {
        val raw = "talk to [@Mom](mention:contact:34) today"
        val vt = MentionVisualTransformation()
        val transformed = vt.filter(androidx.compose.ui.text.AnnotatedString(raw))

        assertEquals("talk to @Mom today", transformed.text.text)

        // Test offset mapping boundaries
        val mapping = transformed.offsetMapping
        // "talk to " -> 0..8 matches 0..8
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(8, mapping.originalToTransformed(8))
        // " today" in transformed begins at 12, in original begins at 34
        assertEquals(12, mapping.originalToTransformed(34))
        assertEquals(34, mapping.transformedToOriginal(12))
        assertEquals(18, mapping.originalToTransformed(40))
        assertEquals(40, mapping.transformedToOriginal(18))
    }
}
