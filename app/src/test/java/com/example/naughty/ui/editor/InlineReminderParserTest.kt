package com.example.naughty.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineReminderParserTest {

    @Test
    fun testParseReminders_standardToken() {
        // 1738291829000L is Jan 31, 2025 10:50:29 AM GMT
        val time = 1738291829000L
        val line = "- [ ] Finish presentation [🔔 Tomorrow, 10:00 AM](reminder:$time)"
        // When parsed from yesterday's perspective (time is tomorrow):
        val oneDayBefore = time - (24 * 60 * 60 * 1000L)
        val reminders = InlineReminderParser.parseReminders(line, nowMillis = oneDayBefore)

        assertEquals(1, reminders.size)
        val reminder = reminders[0]
        assertTrue(reminder.displayText.startsWith("Tomorrow,"))
        assertEquals(time, reminder.timestampMillis)
    }

    @Test
    fun testParseReminders_passedReminderShowsActualDate() {
        val pastTime = 1700000000000L // Nov 14, 2023
        val line = "- [ ] Stale task [🔔 Tomorrow, 10:00 AM](reminder:$pastTime)"
        // Current perspective is way past the reminder
        val now = pastTime + (7 * 24 * 60 * 60 * 1000L)
        val reminders = InlineReminderParser.parseReminders(line, nowMillis = now)

        assertEquals(1, reminders.size)
        val reminder = reminders[0]
        // Must NOT show "Tomorrow" or "Today" because the date has passed
        assertFalse(reminder.displayText.startsWith("Tomorrow"))
        assertFalse(reminder.displayText.startsWith("Today"))
        val expectedMonthDay = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(pastTime))
        assertTrue(reminder.displayText.contains(expectedMonthDay))
    }

    @Test
    fun testParseReminders_middleOfLine() {
        val line = "Meeting with Sarah [🔔 3:30 PM](reminder:1700000000000) at Starbucks"
        val reminders = InlineReminderParser.parseReminders(line)

        assertEquals(1, reminders.size)
        assertEquals("3:30 PM", reminders[0].displayText)
        assertEquals(1700000000000L, reminders[0].timestampMillis)
    }

    @Test
    fun testAttachOrUpdateReminderInLine_newReminderOnTask() {
        val task = "- [ ] Buy fresh coffee beans"
        val time = 1750000000000L
        val updated = InlineReminderParser.attachOrUpdateReminderInLine(task, time, "Tomorrow, 9:00 AM")

        assertEquals("- [ ] Buy fresh coffee beans [🔔 Tomorrow, 9:00 AM](reminder:1750000000000)", updated)
    }

    @Test
    fun testAttachOrUpdateReminderInLine_updateExistingReminder() {
        val task = "- [ ] Buy coffee [🔔 Today, 4:00 PM](reminder:1740000000000)"
        val newTime = 1750000000000L
        val updated = InlineReminderParser.attachOrUpdateReminderInLine(task, newTime, "Tomorrow, 9:00 AM")

        assertEquals("- [ ] Buy coffee [🔔 Tomorrow, 9:00 AM](reminder:1750000000000)", updated)
    }

    @Test
    fun testRemoveReminderFromLine() {
        val line = "- [ ] Call accountant [🔔 Tomorrow, 10:00 AM](reminder:1738291829000)"
        val cleaned = InlineReminderParser.removeReminderFromLine(line)

        assertEquals("- [ ] Call accountant", cleaned)
    }

    @Test
    fun testHasReminder() {
        val lineWith = "Important task [🔔 Tomorrow](reminder:12345678)"
        val lineWithout = "Just a regular task"

        assertTrue(InlineReminderParser.hasReminder(lineWith))
        assertFalse(InlineReminderParser.hasReminder(lineWithout))
    }

    @Test
    fun testExtractFirstReminder() {
        val line = "- [ ] Submit taxes [🔔 Apr 15, 11:59 PM](reminder:1744761540000)"
        val reminder = InlineReminderParser.extractFirstReminder(line)

        assertNotNull(reminder)
        assertEquals("Apr 15, 11:59 PM", reminder?.displayText)
    }

    @Test
    fun testFindUpcomingReminders() {
        val future = System.currentTimeMillis() + 100000L
        val past = System.currentTimeMillis() - 100000L

        val content = """
            - [ ] Task 1 [🔔 Past](reminder:$past)
            - [ ] Task 2 [🔔 Future](reminder:$future)
        """.trimIndent()

        val upcoming = InlineReminderParser.findUpcomingReminders(content)
        assertEquals(1, upcoming.size)
        assertEquals("Future", upcoming[0].displayText)
    }

    @Test
    fun testCleanAllReminders_stripsWeirdReminderSyntax() {
        val weirdText = "wv [🔔 Today, 6:22 PM](reminder:1790513520597)"
        val cleaned = InlineReminderParser.cleanAllReminders(weirdText)
        assertEquals("wv", cleaned)
    }

    @Test
    fun testCleanAllReminders_multilineText() {
        val multiline = """
            wkfw
            wkc w
            wv [🔔 Today, 6:22 PM](reminder:1790513520597)
        """.trimIndent()
        val cleaned = InlineReminderParser.cleanAllReminders(multiline)
        val expected = """
            wkfw
            wkc w
            wv
        """.trimIndent()
        assertEquals(expected, cleaned)
    }
}
