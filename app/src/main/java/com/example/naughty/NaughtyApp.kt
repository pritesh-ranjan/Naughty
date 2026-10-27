package com.example.naughty

import android.app.Application
import com.example.naughty.binding.DetectorForegroundService

class NaughtyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Start sticky foreground service which owns the lifecycle of BindingDetector and ContextNotificationManager
        DetectorForegroundService.start(this)
    }
}

