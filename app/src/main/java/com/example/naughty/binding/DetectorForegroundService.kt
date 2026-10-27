package com.example.naughty.binding

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.naughty.MainActivity
import com.example.naughty.NaughtyApp
import com.example.naughty.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Sticky Foreground Service that holds FOREGROUND_SERVICE priority
 * to prevent the Linux cgroup v2 cached-app freezer from halting
 * context-aware app binding and Circle to Search detection.
 */
class DetectorForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating DetectorForegroundService")
        createServiceNotificationChannel()

        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }

        val app = applicationContext as? NaughtyApp
        if (app != null) {
            app.container.bindingDetector.start(serviceScope)
            app.container.contextNotificationManager.start(serviceScope)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DetectorForegroundService started")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying DetectorForegroundService")
        val app = applicationContext as? NaughtyApp
        if (app != null) {
            app.container.bindingDetector.stop()
            app.container.contextNotificationManager.stop()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildOngoingNotification(): android.app.Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Naughty Context Engine")
            .setContentText("Monitoring contextual note bindings")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Context Engine Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps contextual app binding detection active in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "DetectorService"
        const val SERVICE_NOTIFICATION_ID = 2005
        const val SERVICE_CHANNEL_ID = "naughty_detector_service_v1"

        fun start(context: Context) {
            try {
                val intent = Intent(context, DetectorForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start DetectorForegroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DetectorForegroundService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop DetectorForegroundService", e)
            }
        }
    }
}
