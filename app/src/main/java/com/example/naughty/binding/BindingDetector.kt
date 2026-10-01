package com.example.naughty.binding

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import com.example.naughty.data.local.BindingType
import com.example.naughty.data.repository.BindingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BindingDetector(
    private val context: Context,
    private val bindingRepository: BindingRepository
) {
    private val _events = MutableSharedFlow<BindingEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<BindingEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var lastPollTime = System.currentTimeMillis()
    private var currentForegroundPackage: String? = null
    private var activeBoundPackage: String? = null

    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    fun start(scope: CoroutineScope) {
        if (pollingJob?.isActive == true) return
        lastPollTime = System.currentTimeMillis() - 5000 // Look back 5s on start

        pollingJob = scope.launch(Dispatchers.IO) {
            while (true) {
                pollForegroundApp()
                delay(1000) // Poll every 1 second for snappy detection
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        currentForegroundPackage = null
        activeBoundPackage = null
    }

    val currentForeground: String? get() = currentForegroundPackage

    private suspend fun pollForegroundApp() {
        val isScreenOn = powerManager?.isInteractive ?: true
        if (!isScreenOn) {
            if (activeBoundPackage != null) {
                _events.emit(BindingEvent(isDismiss = true, packageName = activeBoundPackage ?: "", noteId = ""))
                activeBoundPackage = null
            }
            return
        }

        val usm = usageStatsManager ?: return
        val now = System.currentTimeMillis()

        try {
            val events = usm.queryEvents(lastPollTime, now)
            val event = UsageEvents.Event()
            var latestForegroundPackage: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // Event 1 = MOVE_TO_FOREGROUND, Event 15 = ACTIVITY_RESUMED
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || event.eventType == 15) {
                    if (event.timeStamp > latestTimestamp) {
                        latestTimestamp = event.timeStamp
                        latestForegroundPackage = event.packageName
                    }
                }
            }

            // Fallback: Check UsageStats in recent window if no events in slice
            if (latestForegroundPackage == null) {
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, lastPollTime - 2000, now)
                val mostRecent = stats?.filter { it.lastTimeUsed > 0 }?.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > lastPollTime) {
                    latestForegroundPackage = mostRecent.packageName
                    latestTimestamp = mostRecent.lastTimeUsed
                }
            }

            lastPollTime = now

            if (latestForegroundPackage != null && latestForegroundPackage != currentForegroundPackage) {
                currentForegroundPackage = latestForegroundPackage
                handleForegroundChange(latestForegroundPackage)
            }
        } catch (_: SecurityException) {
            // Usage stats permission not granted
        }
    }

    private suspend fun handleForegroundChange(packageName: String) {
        // If we moved away from the currently active bound package, clear the notification!
        if (activeBoundPackage != null && activeBoundPackage != packageName) {
            _events.emit(BindingEvent(isDismiss = true, packageName = activeBoundPackage ?: "", noteId = ""))
            activeBoundPackage = null
        }

        // Do not trigger binding notifications for Naughty itself
        if (packageName == context.packageName) return

        val activeBindings = bindingRepository.getActiveByPackage(packageName)
        val now = System.currentTimeMillis()

        var triggeredAny = false
        for (binding in activeBindings) {
            // Check if binding has expired
            if (binding.expiresAt != null && binding.expiresAt <= now) {
                bindingRepository.deactivateBinding(binding.id)
                continue
            }

            _events.emit(
                BindingEvent(
                    binding = binding,
                    noteId = binding.noteId,
                    packageName = binding.packageName,
                    type = binding.type,
                    isDismiss = false
                )
            )
            triggeredAny = true

            // Update last triggered
            bindingRepository.updateLastTriggered(binding.id)

            // Deactivate one-shot bindings after firing
            if (binding.type == BindingType.ONE_SHOT) {
                bindingRepository.deactivateBinding(binding.id)
            }
        }

        if (triggeredAny) {
            activeBoundPackage = packageName
        }
    }
}
