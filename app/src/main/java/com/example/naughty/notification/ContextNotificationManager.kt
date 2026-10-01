package com.example.naughty.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.naughty.MainActivity
import com.example.naughty.R
import com.example.naughty.binding.BindingDetector
import com.example.naughty.binding.BindingEvent
import com.example.naughty.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ContextNotificationManager(
    private val context: Context,
    private val bindingDetector: BindingDetector,
    private val noteRepository: NoteRepository
) {
    private var collectionJob: Job? = null
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createContextChannel()
    }

    fun start(scope: CoroutineScope) {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch {
            bindingDetector.events.collect { event ->
                if (event.isDismiss) {
                    dismissContextNotification()
                } else {
                    showContextNotification(event)
                }
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        dismissContextNotification()
    }

    fun dismissContextNotification() {
        notificationManager.cancel(CONTEXT_NOTIFICATION_ID)
    }

    private suspend fun showContextNotification(event: BindingEvent) {
        if (event.noteId.isBlank()) return
        val noteContent = noteRepository.getNoteContent(event.noteId)
        val note = noteRepository.getNote(event.noteId) ?: return

        // Strip markdown formatting for notification preview
        val preview = noteContent
            // Remove image markdown ![alt](url) first
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
            // Remove leftover [image] or similar placeholders
            .replace(Regex("\\[image\\]", RegexOption.IGNORE_CASE), "")
            // Remove reminder tags [text](reminder:timestamp)
            .replace(Regex("\\[.*?\\]\\(reminder:\\d+\\)"), "")
            // Remove remaining markdown formatting chars
            .replace(Regex("[#*_~`>\\[\\]()]"), "")
            .replace(Regex("\\n+"), " ")
            .trim()
            .take(200)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("noteId", event.noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentDisplay = if (preview.isNotBlank()) preview else "Tap to view contextual note"

        val notification = NotificationCompat.Builder(context, CONTEXT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(note.metadata.title.ifBlank { "Untitled Note" })
            .setContentText(contentDisplay)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentDisplay))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000L) // Auto-dismiss after 30 seconds
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(CONTEXT_NOTIFICATION_ID, notification)
    }

    private fun createContextChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CONTEXT_CHANNEL_ID,
                "Context Notes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notes surfaced based on app context"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CONTEXT_CHANNEL_ID = "naughty_context_v2"
        const val CONTEXT_NOTIFICATION_ID = 2001
    }
}
