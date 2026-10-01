package com.example.naughty

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.naughty.binding.BindingDetectorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NaughtyApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Clear any previous persistent "watching for context" notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(1001)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm?.deleteNotificationChannel(BindingDetectorService.SERVICE_CHANNEL_ID)
        }

        // Stop legacy foreground service if active
        try {
            stopService(Intent(this, BindingDetectorService::class.java))
        } catch (_: Exception) {}

        // Launch binding detector and notification manager in application scope
        container.bindingDetector.start(appScope)
        container.contextNotificationManager.start(appScope)
    }
}

