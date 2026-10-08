package com.example.naughty.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.naughty.NaughtyApp
import com.example.naughty.ui.timeline.TimelineProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NaughtyApp ?: return
        val reminderManager = app.container.reminderManager

        when (intent.action) {
            ReminderManager.ACTION_FIRE_REMINDER -> {
                val noteId = intent.getStringExtra(ReminderManager.EXTRA_NOTE_ID) ?: ""
                val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                val defaultId = if (!trackerId.isNullOrBlank()) trackerId.hashCode() else noteId.hashCode()
                val reminderId = intent.getIntExtra(ReminderManager.EXTRA_REMINDER_ID, defaultId)
                val title = intent.getStringExtra(ReminderManager.EXTRA_TITLE) ?: "Reminder"
                val content = intent.getStringExtra(ReminderManager.EXTRA_CONTENT) ?: ""

                reminderManager.showReminderNotification(
                    noteId = noteId,
                    notificationId = reminderId,
                    title = title,
                    contentSnippet = content,
                    trackerId = trackerId
                )
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = app.container.noteRepository
                        val pending = repository.getNotesWithPendingReminders()
                        val now = System.currentTimeMillis()
                        for (note in pending) {
                            val reminderTime = note.reminderTime ?: continue
                            if (reminderTime > now) {
                                val content = repository.getNoteContent(note.id)
                                val snippet = content.take(150).replace("\n", " ").trim()
                                reminderManager.scheduleReminder(
                                    noteId = note.id,
                                    reminderId = note.id.hashCode(),
                                    title = note.title,
                                    contentSnippet = snippet,
                                    triggerAtMillis = reminderTime
                                )
                            }
                        }

                        // Also reschedule timeline milestone reminders
                        val timelineRepo = app.container.timelineRepository
                        val pendingMilestones = timelineRepo.getMilestonesWithPendingReminders(now)
                        for (milestone in pendingMilestones) {
                            val dueAt = milestone.dueAt ?: continue
                            if (dueAt > now) {
                                val tracker = timelineRepo.getTracker(milestone.trackerId)
                                reminderManager.scheduleReminder(
                                    noteId = "",
                                    reminderId = TimelineProgress.reminderId(milestone.id),
                                    title = "⏱ ${tracker?.title ?: "Timeline"}: ${milestone.title}",
                                    contentSnippet = milestone.note.ifBlank { "Milestone is due." },
                                    triggerAtMillis = dueAt,
                                    trackerId = milestone.trackerId
                                )
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
