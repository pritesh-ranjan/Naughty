package com.example.naughty.binding

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.naughty.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class BindingDetectorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(), serviceType)
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())
        }

        // Get detector from app container and start it
        val app = application as com.example.naughty.NaughtyApp
        app.container.bindingDetector.start(serviceScope)
        app.container.contextNotificationManager.start(serviceScope)
    }

    override fun onDestroy() {
        val app = application as com.example.naughty.NaughtyApp
        app.container.bindingDetector.stop()
        app.container.contextNotificationManager.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Naughty Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service monitoring app context"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Naughty")
            .setContentText("Watching for context…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "naughty_service"
        const val SERVICE_NOTIFICATION_ID = 1001

        fun start(context: Context) {
            // No persistent foreground notification needed; detector runs in NaughtyApp appScope.
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(SERVICE_NOTIFICATION_ID)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    nm?.deleteNotificationChannel(SERVICE_CHANNEL_ID)
                }
                context.stopService(Intent(context, BindingDetectorService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(SERVICE_NOTIFICATION_ID)
                context.stopService(Intent(context, BindingDetectorService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
