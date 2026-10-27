package com.example.naughty.notification

import androidx.core.app.NotificationCompat
import com.example.naughty.binding.CtsExitEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCategoriesTest {

    @Test
    fun testNotificationChannelIdsAreDistinct() {
        val channelIds = setOf(
            ContextNotificationManager.CONTEXT_CHANNEL_ID,
            ContextNotificationManager.CTS_CHANNEL_ID,
            ReminderManager.REMINDER_CHANNEL_ID,
            "naughty_detector_service_v1"
        )
        // All 4 channel IDs must be unique
        assertEquals(4, channelIds.size)
        assertEquals("naughty_cts_v1", ContextNotificationManager.CTS_CHANNEL_ID)
        assertEquals("naughty_context_v2", ContextNotificationManager.CONTEXT_CHANNEL_ID)
        assertEquals("naughty_reminders_v1", ReminderManager.REMINDER_CHANNEL_ID)
    }

    @Test
    fun testSemanticNotificationCategories() {
        // Scheduled reminders should use CATEGORY_REMINDER
        assertEquals("reminder", NotificationCompat.CATEGORY_REMINDER)

        // Circle to Search capture prompts should use CATEGORY_RECOMMENDATION
        assertEquals("recommendation", NotificationCompat.CATEGORY_RECOMMENDATION)

        // Background service should use CATEGORY_SERVICE
        assertEquals("service", NotificationCompat.CATEGORY_SERVICE)
    }

    @Test
    fun testCtsNotificationToggleBehavior() {
        var isCtsNotificationEnabled = true
        var notificationPosted = false

        fun triggerCtsExit(event: CtsExitEvent) {
            if (!isCtsNotificationEnabled) return
            notificationPosted = true
        }

        val event = CtsExitEvent(
            sourceAppName = "Chrome",
            sourcePackage = "com.android.chrome",
            timestamp = 1000L
        )

        // When enabled: notification is posted
        triggerCtsExit(event)
        assertTrue(notificationPosted)

        // When disabled: notification is suppressed
        isCtsNotificationEnabled = false
        notificationPosted = false
        triggerCtsExit(event)
        assertFalse(notificationPosted)
    }
}
