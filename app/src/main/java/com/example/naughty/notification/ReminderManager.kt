package com.example.naughty.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import com.example.naughty.MainActivity
import com.example.naughty.R

class ReminderManager(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    init {
        createReminderChannel()
    }

    fun scheduleReminder(
        noteId: String,
        reminderId: Int,
        title: String,
        contentSnippet: String,
        triggerAtMillis: Long,
        trackerId: String? = null
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title.ifBlank { "Untitled Note" })
            putExtra(EXTRA_CONTENT, contentSnippet)
            if (trackerId != null) {
                putExtra(EXTRA_TRACKER_ID, trackerId)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showReminderNotification(
        noteId: String,
        notificationId: Int,
        title: String,
        contentSnippet: String,
        trackerId: String? = null
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!noteId.isNullOrBlank()) {
                putExtra("noteId", noteId)
            }
            if (!trackerId.isNullOrBlank()) {
                putExtra("trackerId", trackerId)
            }
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = title.ifBlank { if (trackerId != null) "Timeline Reminder" else "Note Reminder" }
        val displayContent = contentSnippet.ifBlank { "You have a scheduled reminder." }
        val actionText = if (!trackerId.isNullOrBlank()) "Open Tracker" else "Open Note"

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 $displayTitle")
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                actionText,
                openPendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System and scheduled alerts for notes & tasks"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "naughty_reminders_v1"
        const val ACTION_FIRE_REMINDER = "com.example.naughty.action.FIRE_REMINDER"

        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_TRACKER_ID = "extra_tracker_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"

        fun createCalendarIntent(
            title: String,
            description: String,
            startTimeMillis: Long
        ): Intent {
            return Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTimeMillis + (60 * 60 * 1000))
                putExtra(CalendarContract.Events.TITLE, title.ifBlank { "Untitled Note" })
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.HAS_ALARM, 1)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
