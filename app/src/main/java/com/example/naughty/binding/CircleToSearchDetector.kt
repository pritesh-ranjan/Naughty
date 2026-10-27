package com.example.naughty.binding

data class CtsExitEvent(
    val sourcePackage: String?,
    val sourceAppName: String,
    val timestamp: Long
)

/**
 * Tracks the lifecycle transitions of Circle to Search (OmnientActivity, Lens, Robin)
 * and resolves the host app from which Circle to Search was invoked.
 */
class CircleToSearchDetector {

    var isCtsActive: Boolean = false
        private set

    private var currentSourcePackage: String? = null
    private var currentSourceAppName: String = "External App"
    private var ctsEnteredTimestamp = 0L

    fun onUsageEvent(
        packageName: String,
        className: String?,
        eventType: Int,
        timeStamp: Long,
        lastHostPackage: String?,
        resolveAppName: (String) -> String
    ): CtsExitEvent? {
        // Auto-recover if CTS was active for longer than max duration (e.g. missed exit event)
        if (isCtsActive && ctsEnteredTimestamp > 0L && (timeStamp - ctsEnteredTimestamp > CTS_MAX_DURATION_MS)) {
            isCtsActive = false
        }

        val isCts = packageName == CTS_PACKAGE &&
                className != null &&
                CTS_KEYWORDS.any { className.contains(it, ignoreCase = true) }

        if (isCts) {
            // Event 1 = ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
            if (eventType == 1) {
                if (!isCtsActive) {
                    isCtsActive = true
                    ctsEnteredTimestamp = timeStamp
                    currentSourcePackage = lastHostPackage
                    currentSourceAppName = if (!lastHostPackage.isNullOrBlank()) {
                        resolveAppName(lastHostPackage)
                    } else {
                        "External App"
                    }
                }
                return null
            } else if (isCtsActive && (eventType == 2 || eventType == 23)) {
                // Event 2 = ACTIVITY_PAUSED, Event 23 = ACTIVITY_STOPPED
                isCtsActive = false
                val exitEvent = CtsExitEvent(
                    sourcePackage = currentSourcePackage,
                    sourceAppName = currentSourceAppName,
                    timestamp = timeStamp
                )
                ctsEnteredTimestamp = 0L
                return exitEvent
            }
        } else if (isCtsActive && eventType == 1 && packageName != CTS_PACKAGE) {
            // Another app resumed while CTS was active -> CTS dismissed
            isCtsActive = false
            val exitEvent = CtsExitEvent(
                sourcePackage = currentSourcePackage,
                sourceAppName = currentSourceAppName,
                timestamp = timeStamp
            )
            ctsEnteredTimestamp = 0L
            return exitEvent
        }
        return null
    }

    fun reset() {
        isCtsActive = false
        currentSourcePackage = null
        currentSourceAppName = "External App"
        ctsEnteredTimestamp = 0L
    }

    companion object {
        const val CTS_PACKAGE = "com.google.android.googlequicksearchbox"
        val CTS_KEYWORDS = listOf("omnient", "lens", "robin", "search.assistant")
        const val CTS_MAX_DURATION_MS = 20_000L
    }
}
