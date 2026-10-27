package com.example.naughty.binding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CircleToSearchDetectorTest {

    private lateinit var detector: CircleToSearchDetector

    @Before
    fun setUp() {
        detector = CircleToSearchDetector()
    }

    @Test
    fun testCtsEnterAndExitLifecycle() {
        val resolveAppName: (String) -> String = { pkg ->
            if (pkg == "com.android.chrome") "Chrome" else "Other"
        }

        // 1. Initial state
        assertFalse(detector.isCtsActive)

        // 2. CTS opens (OmnientActivity resumes)
        val enterResult = detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.gsa.omnient.OmnientActivity",
            eventType = 1, // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
            timeStamp = 1000L,
            lastHostPackage = "com.android.chrome",
            resolveAppName = resolveAppName
        )
        assertNull(enterResult)
        assertTrue(detector.isCtsActive)

        // 3. CTS exits (OmnientActivity stopped)
        val exitResult = detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.gsa.omnient.OmnientActivity",
            eventType = 23, // ACTIVITY_STOPPED
            timeStamp = 5000L,
            lastHostPackage = "com.android.chrome",
            resolveAppName = resolveAppName
        )
        assertNotNull(exitResult)
        assertEquals("Chrome", exitResult?.sourceAppName)
        assertEquals("com.android.chrome", exitResult?.sourcePackage)
        assertEquals(5000L, exitResult?.timestamp)
        assertFalse(detector.isCtsActive)
    }

    @Test
    fun testCtsModernPixelSurfacesLensAndRobin() {
        val resolveAppName: (String) -> String = { "Camera" }

        // Test Lens surface
        detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.search.lens.LensExportedActivity",
            eventType = 1,
            timeStamp = 1000L,
            lastHostPackage = "com.android.camera2",
            resolveAppName = resolveAppName
        )
        assertTrue(detector.isCtsActive)

        val lensExit = detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.search.lens.LensExportedActivity",
            eventType = 2, // ACTIVITY_PAUSED
            timeStamp = 2500L,
            lastHostPackage = "com.android.camera2",
            resolveAppName = resolveAppName
        )
        assertNotNull(lensExit)
        assertEquals("Camera", lensExit?.sourceAppName)
        assertFalse(detector.isCtsActive)

        // Test Robin / Assistant surface
        detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.search.assistant.surfaces.voice.robin.main.MainActivity",
            eventType = 1,
            timeStamp = 5000L,
            lastHostPackage = "com.android.camera2",
            resolveAppName = resolveAppName
        )
        assertTrue(detector.isCtsActive)

        val robinExit = detector.onUsageEvent(
            packageName = "com.android.camera2",
            className = "com.android.camera.CameraActivity",
            eventType = 1,
            timeStamp = 7000L,
            lastHostPackage = "com.android.camera2",
            resolveAppName = resolveAppName
        )
        assertNotNull(robinExit)
        assertEquals("Camera", robinExit?.sourceAppName)
        assertFalse(detector.isCtsActive)
    }

    @Test
    fun testWatchdogTimeoutRecoversFromMissedExitEvent() {
        val resolveAppName: (String) -> String = { "Photos" }

        // 1. Enter CTS
        detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.search.lens.LensExportedActivity",
            eventType = 1,
            timeStamp = 1000L,
            lastHostPackage = "com.google.android.apps.photos",
            resolveAppName = resolveAppName
        )
        assertTrue(detector.isCtsActive)

        // 2. Next event arrives 25 seconds later (past 20s watchdog) without exit event
        detector.onUsageEvent(
            packageName = "com.google.android.apps.photos",
            className = "com.google.android.apps.photos.home.HomeActivity",
            eventType = 1,
            timeStamp = 26_000L,
            lastHostPackage = "com.google.android.apps.photos",
            resolveAppName = resolveAppName
        )
        // Detector should have auto-recovered and cleared isCtsActive
        assertFalse(detector.isCtsActive)

        // 3. Next CTS enter must succeed and not be blocked
        detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.search.lens.LensExportedActivity",
            eventType = 1,
            timeStamp = 27_000L,
            lastHostPackage = "com.google.android.apps.photos",
            resolveAppName = resolveAppName
        )
        assertTrue(detector.isCtsActive)
    }

    @Test
    fun testCtsExitedByAnotherAppResuming() {
        val resolveAppName: (String) -> String = { "Twitter" }

        // 1. CTS opens
        detector.onUsageEvent(
            packageName = CircleToSearchDetector.CTS_PACKAGE,
            className = "com.google.android.apps.gsa.omnient.views.OmnientActivity",
            eventType = 1, // MOVE_TO_FOREGROUND
            timeStamp = 1000L,
            lastHostPackage = "com.twitter.android",
            resolveAppName = resolveAppName
        )
        assertTrue(detector.isCtsActive)

        // 2. User returns to Twitter (Twitter resumes while CTS was active)
        val exitResult = detector.onUsageEvent(
            packageName = "com.twitter.android",
            className = "com.twitter.android.MainActivity",
            eventType = 1, // ACTIVITY_RESUMED
            timeStamp = 3000L,
            lastHostPackage = "com.twitter.android",
            resolveAppName = resolveAppName
        )
        assertNotNull(exitResult)
        assertEquals("Twitter", exitResult?.sourceAppName)
        assertFalse(detector.isCtsActive)
    }

    @Test
    fun testUnrelatedEventsDoNotTriggerCts() {
        val resolveAppName: (String) -> String = { "App" }

        val result = detector.onUsageEvent(
            packageName = "com.example.other",
            className = "com.example.other.MainActivity",
            eventType = 1,
            timeStamp = 1000L,
            lastHostPackage = null,
            resolveAppName = resolveAppName
        )
        assertNull(result)
        assertFalse(detector.isCtsActive)
    }
}
