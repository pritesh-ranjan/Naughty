package com.example.naughty.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.naughty.NaughtyApp
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
                val reminderId = intent.getIntExtra(ReminderManager.EXTRA_REMINDER_ID, noteId.hashCode())
                val title = intent.getStringExtra(ReminderManager.EXTRA_TITLE) ?: "Note Reminder"
                val content = intent.getStringExtra(ReminderManager.EXTRA_CONTENT) ?: ""

                reminderManager.showReminderNotification(
                    noteId = noteId,
                    notificationId = reminderId,
                    title = title,
                    contentSnippet = content
                )
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = app.container.noteRepository
                        val pending = repository.getNotesWithPendingReminders()
                        for (note in pending) {
                            val reminderTime = note.reminderTime ?: continue
                            if (reminderTime > System.currentTimeMillis()) {
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
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
