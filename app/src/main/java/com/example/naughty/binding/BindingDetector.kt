package com.example.naughty.binding

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
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

    private val _ctsExitEvents = MutableSharedFlow<CtsExitEvent>(extraBufferCapacity = 8)
    val ctsExitEvents: SharedFlow<CtsExitEvent> = _ctsExitEvents.asSharedFlow()

    private val ctsDetector = CircleToSearchDetector()
    private var lastHostPackage: String? = null

    private var pollingJob: Job? = null
    private var lastProcessedTimestamp = System.currentTimeMillis() - 5000L
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
        lastProcessedTimestamp = System.currentTimeMillis() - 5000L // Look back 5s on start

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
        lastHostPackage = null
        lastProcessedTimestamp = 0L
        ctsDetector.reset()
    }

    val currentForeground: String? get() = currentForegroundPackage

    fun resolveAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    private suspend fun pollForegroundApp() {
        val isScreenOn = powerManager?.isInteractive ?: true
        if (!isScreenOn) {
            if (activeBoundPackage != null) {
                _events.emit(BindingEvent(isDismiss = true, packageName = activeBoundPackage ?: "", noteId = ""))
                activeBoundPackage = null
            }
            // Reset current foreground so turning screen back on re-evaluates bound app cleanly
            currentForegroundPackage = null
            return
        }

        val usm = usageStatsManager ?: return
        val now = System.currentTimeMillis()

        try {
            // Query with a 2-second lookback overlap to prevent dropped events at IPC boundaries
            val queryStart = maxOf(lastProcessedTimestamp - 2000L, now - 60_000L)
            val events = usm.queryEvents(queryStart, now)
            val event = UsageEvents.Event()
            var latestForegroundPackage: String? = null
            var latestTimestamp = 0L
            var maxEventTimestamp = lastProcessedTimestamp

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                // Skip events already processed in earlier sliding queries
                if (event.timeStamp <= lastProcessedTimestamp) {
                    continue
                }
                if (event.timeStamp > maxEventTimestamp) {
                    maxEventTimestamp = event.timeStamp
                }

                // Check Circle to Search lifecycle
                val ctsExit = ctsDetector.onUsageEvent(
                    packageName = event.packageName,
                    className = event.className,
                    eventType = event.eventType,
                    timeStamp = event.timeStamp,
                    lastHostPackage = lastHostPackage,
                    resolveAppName = ::resolveAppName
                )
                if (ctsExit != null) {
                    Log.d("BindingDetector", "CTS exit detected: ${ctsExit.sourceAppName}")
                    _ctsExitEvents.emit(ctsExit)
                }

                // Event 1 = ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
                if (event.eventType == 1) {
                    // Update last third-party host package when another app is in the foreground
                    if (event.packageName != context.packageName && event.packageName != CircleToSearchDetector.CTS_PACKAGE) {
                        lastHostPackage = event.packageName
                    }

                    if (event.timeStamp > latestTimestamp) {
                        latestTimestamp = event.timeStamp
                        latestForegroundPackage = event.packageName
                    }
                }
            }

            if (maxEventTimestamp > lastProcessedTimestamp) {
                lastProcessedTimestamp = maxEventTimestamp
            } else {
                lastProcessedTimestamp = now
            }

            if (latestForegroundPackage != null && latestForegroundPackage != currentForegroundPackage) {
                Log.d("BindingDetector", "Foreground app changed: $currentForegroundPackage -> $latestForegroundPackage")
                currentForegroundPackage = latestForegroundPackage
                handleForegroundChange(latestForegroundPackage)
            }
        } catch (e: SecurityException) {
            Log.w("BindingDetector", "Usage stats permission not granted", e)
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

            Log.d("BindingDetector", "Triggering binding for note ${binding.noteId} on $packageName")
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
