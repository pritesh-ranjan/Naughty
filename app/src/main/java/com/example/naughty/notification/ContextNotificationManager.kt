package com.example.naughty.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.naughty.MainActivity
import com.example.naughty.QuickNotePopupActivity
import com.example.naughty.R
import com.example.naughty.binding.BindingDetector
import com.example.naughty.binding.BindingEvent
import com.example.naughty.binding.CtsExitEvent
import com.example.naughty.data.repository.NoteRepository
import com.example.naughty.ui.theme.ctsNotificationPreferenceFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ContextNotificationManager(
    private val context: Context,
    private val bindingDetector: BindingDetector,
    private val noteRepository: NoteRepository
) {
    private var collectionJob: Job? = null
    private var ctsJob: Job? = null
    private var preferenceJob: Job? = null
    var isCtsNotificationEnabled: Boolean = true
        internal set

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    fun start(scope: CoroutineScope) {
        if (collectionJob?.isActive == true) return

        preferenceJob = scope.launch {
            context.ctsNotificationPreferenceFlow().collect { enabled ->
                isCtsNotificationEnabled = enabled
                if (!enabled) {
                    dismissCtsNotification()
                }
            }
        }

        collectionJob = scope.launch {
            bindingDetector.events.collect { event ->
                if (event.isDismiss) {
                    dismissContextNotification()
                } else {
                    showContextNotification(event)
                }
            }
        }

        ctsJob = scope.launch {
            bindingDetector.ctsExitEvents.collect { ctsEvent ->
                if (isCtsNotificationEnabled) {
                    showCtsExitNotification(ctsEvent)
                }
            }
        }
    }

    fun stop() {
        preferenceJob?.cancel()
        preferenceJob = null
        collectionJob?.cancel()
        collectionJob = null
        ctsJob?.cancel()
        ctsJob = null
        dismissContextNotification()
        dismissCtsNotification()
    }

    fun dismissContextNotification() {
        notificationManager.cancel(CONTEXT_NOTIFICATION_ID)
    }

    fun dismissCtsNotification() {
        notificationManager.cancel(CTS_NOTIFICATION_ID)
    }

    fun showCtsExitNotification(event: CtsExitEvent) {
        if (!isCtsNotificationEnabled) return

        val sourceApp = event.sourceAppName.ifBlank { "External App" }
        val title = "Circle to Search"
        val contentText = "Save snippet from $sourceApp to 'Circle to Search'?"

        // Direct 1-tap save action
        val directSaveIntent = Intent(context, QuickNotePopupActivity::class.java).apply {
            action = QuickNotePopupActivity.ACTION_DIRECT_SAVE_CTS
            putExtra(QuickNotePopupActivity.EXTRA_SOURCE_APP, sourceApp)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val directSavePendingIntent = PendingIntent.getActivity(
            context,
            CTS_SAVE_REQUEST_CODE,
            directSaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Review & edit intent (tapping body)
        val reviewIntent = Intent(context, QuickNotePopupActivity::class.java).apply {
            action = QuickNotePopupActivity.ACTION_REVIEW_CTS
            putExtra(QuickNotePopupActivity.EXTRA_SOURCE_APP, sourceApp)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            CTS_REVIEW_REQUEST_CODE,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setTimeoutAfter(15_000L) // Auto-dismiss after 15 seconds
            .setContentIntent(reviewPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Save",
                directSavePendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        notificationManager.notify(CTS_NOTIFICATION_ID, notification)
    }

    private suspend fun showContextNotification(event: BindingEvent) {
        if (event.noteId.isBlank()) return
        val note = noteRepository.getNote(event.noteId) ?: return
        if (note.metadata.isDeleted) return
        val noteContent = note.content

        // Strip markdown formatting for notification preview
        val preview = noteContent
            // Remove image markdown ![alt](url) first
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
            // Remove leftover [image] or similar placeholders
            .replace(Regex("\\[image\\]", RegexOption.IGNORE_CASE), "")
            // Remove voice note tags [🎤 Voice Note](file://...)
            .replace(Regex("\\[🎤\\s*Voice Note\\]\\(.*?\\)", RegexOption.IGNORE_CASE), "")
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

        val contentDisplay = if (note.metadata.isLocked) {
            "Locked note • Tap to unlock"
        } else if (preview.isNotBlank()) {
            preview
        } else {
            "Tap to view contextual note"
        }

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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val contextChannel = NotificationChannel(
                CONTEXT_CHANNEL_ID,
                "Context Notes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notes surfaced based on app context"
                setShowBadge(true)
                enableVibration(true)
            }
            val ctsChannel = NotificationChannel(
                CTS_CHANNEL_ID,
                "Circle to Search",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts to capture notes when exiting Circle to Search"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(contextChannel)
            notificationManager.createNotificationChannel(ctsChannel)
        }
    }

    companion object {
        const val CONTEXT_CHANNEL_ID = "naughty_context_v2"
        const val CTS_CHANNEL_ID = "naughty_cts_v1"
        const val CONTEXT_NOTIFICATION_ID = 2001
        const val CTS_NOTIFICATION_ID = 2002
        const val CTS_SAVE_REQUEST_CODE = 3001
        const val CTS_REVIEW_REQUEST_CODE = 3002
    }
}
