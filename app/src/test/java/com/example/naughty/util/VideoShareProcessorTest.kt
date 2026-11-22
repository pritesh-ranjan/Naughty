package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoShareProcessorTest {

    @Test
    fun testComputeReducedDimensions_720pCapAndZeroUpscaling() {
        // 4K Landscape -> 1280x720
        assertEquals(1280 to 720, VideoShareProcessor.computeReducedDimensions(3840, 2160))

        // 1080p Landscape -> 1280x720
        assertEquals(1280 to 720, VideoShareProcessor.computeReducedDimensions(1920, 1080))

        // 1080p Portrait -> 720x1280
        assertEquals(720 to 1280, VideoShareProcessor.computeReducedDimensions(1080, 1920))

        // Already 720p -> unchanged
        assertEquals(1280 to 720, VideoShareProcessor.computeReducedDimensions(1280, 720))

        // Smaller than 720p -> zero upscaling
        assertEquals(640 to 360, VideoShareProcessor.computeReducedDimensions(640, 360))
        assertEquals(480 to 854, VideoShareProcessor.computeReducedDimensions(480, 854))

        // Odd dimensions aligned down to even
        assertEquals(480 to 360, VideoShareProcessor.computeReducedDimensions(481, 361))
    }

    @Test
    fun testIsWithinDurationLimit() {
        assertFalse(VideoShareProcessor.isWithinDurationLimit(0L))
        assertFalse(VideoShareProcessor.isWithinDurationLimit(-500L))
        assertTrue(VideoShareProcessor.isWithinDurationLimit(1_000L))
        assertTrue(VideoShareProcessor.isWithinDurationLimit(60_000L))
        assertTrue(VideoShareProcessor.isWithinDurationLimit(120_000L))
        assertTrue(VideoShareProcessor.isWithinDurationLimit(180_000L))
        assertTrue(VideoShareProcessor.isWithinDurationLimit(180_500L))
        assertFalse(VideoShareProcessor.isWithinDurationLimit(180_501L))
        assertFalse(VideoShareProcessor.isWithinDurationLimit(210_000L))
    }

    @Test
    fun testComputeFpsFromSampleTimestamps() {
        // 30 FPS -> ~33,333 us per frame
        val samples30 = (0 until 30).map { it * 33_333L }
        val fps30 = VideoShareProcessor.computeFpsFromSampleTimestamps(samples30)
        assertNotNull(fps30)
        assertEquals(30f, fps30!!, 0.5f)

        // 60 FPS -> ~16,667 us per frame
        val samples60 = (0 until 60).map { it * 16_667L }
        val fps60 = VideoShareProcessor.computeFpsFromSampleTimestamps(samples60)
        assertNotNull(fps60)
        assertEquals(60f, fps60!!, 0.5f)

        // 24 FPS -> ~41,667 us per frame
        val samples24 = (0 until 24).map { it * 41_667L }
        val fps24 = VideoShareProcessor.computeFpsFromSampleTimestamps(samples24)
        assertNotNull(fps24)
        assertEquals(24f, fps24!!, 0.5f)

        // Too few samples -> null
        assertNull(VideoShareProcessor.computeFpsFromSampleTimestamps(listOf(0L)))
        assertNull(VideoShareProcessor.computeFpsFromSampleTimestamps(emptyList()))
    }

    @Test
    fun testGridSignatureDiff_globalSceneCutAndLocalSubtitleChange() {
        val w = 64
        val h = 64
        val darkPixels = IntArray(w * h) { 0xFF101010.toInt() }
        val brightPixels = IntArray(w * h) { 0xFFEFEFEF.toInt() }

        val sigDark = VideoShareProcessor.computeGridSignatureFromPixels(darkPixels, w, h)
        val sigDarkCopy = VideoShareProcessor.computeGridSignatureFromPixels(darkPixels, w, h)
        val sigBright = VideoShareProcessor.computeGridSignatureFromPixels(brightPixels, w, h)

        // Identical frames -> no visual change
        val sameDiff = VideoShareProcessor.compareGridSignatures(sigDark, sigDarkCopy)
        assertFalse(sameDiff.hasVisualChange)
        assertFalse(sameDiff.isSceneCut)

        // Global bright scene cut -> scene cut + visual change
        val cutDiff = VideoShareProcessor.compareGridSignatures(sigDark, sigBright)
        assertTrue(cutDiff.isSceneCut)
        assertTrue(cutDiff.hasVisualChange)

        // Localized subtitle stripe in bottom row (approx 1/8th of image height)
        val subtitlePixels = darkPixels.copyOf()
        for (y in 56 until 64) {
            for (x in 8 until 56) {
                subtitlePixels[y * w + x] = if (x % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF101010.toInt()
            }
        }
        val sigSubtitle = VideoShareProcessor.computeGridSignatureFromPixels(subtitlePixels, w, h)
        val localDiff = VideoShareProcessor.compareGridSignatures(sigDark, sigSubtitle)
        assertTrue(localDiff.hasVisualChange)
        assertFalse(localDiff.isSceneCut)
    }

    @Test
    fun testAdaptiveSamplingControllerStateTransitions() {
        val controller = AdaptiveSamplingController()
        val sourceFps = 30f

        val noChange = GridDiffResult(0.5f, 1.0f, isSceneCut = false, hasVisualChange = false)
        val activeChange = GridDiffResult(5.0f, 14.0f, isSceneCut = false, hasVisualChange = true)
        val sceneCut = GridDiffResult(45.0f, 60.0f, isSceneCut = true, hasVisualChange = true)

        // First static frame keeps baseline 2.5 FPS (400 ms)
        val step1 = controller.onSampleEvaluated(0L, noChange, newTextDetected = false, detectedFps = sourceFps)
        assertEquals(SamplingMode.NORMAL_TALKING, controller.currentMode)
        assertEquals(400L, step1)

        // Second consecutive static frame drops to 1 FPS (1000 ms)
        val step2 = controller.onSampleEvaluated(400L, noChange, newTextDetected = false, detectedFps = sourceFps)
        assertEquals(SamplingMode.STATIC_PRESENTATION, controller.currentMode)
        assertEquals(1000L, step2)

        // Scene cut / new text triggers 8 FPS burst (125 ms)
        val step3 = controller.onSampleEvaluated(1400L, sceneCut, newTextDetected = true, detectedFps = sourceFps)
        assertEquals(SamplingMode.SCENE_OR_TEXT_BURST, controller.currentMode)
        assertEquals(125L, step3)

        // Inside 1000 ms burst window remains at 8 FPS
        val step4 = controller.onSampleEvaluated(1600L, noChange, newTextDetected = false, detectedFps = sourceFps)
        assertEquals(SamplingMode.SCENE_OR_TEXT_BURST, controller.currentMode)
        assertEquals(125L, step4)

        // After burst window expires with continuous active visual changes -> 4 FPS (250 ms)
        controller.onSampleEvaluated(2500L, activeChange, newTextDetected = false, detectedFps = sourceFps)
        val step5 = controller.onSampleEvaluated(2900L, activeChange, newTextDetected = false, detectedFps = sourceFps)
        assertEquals(SamplingMode.FAST_CHANGING, controller.currentMode)
        assertEquals(250L, step5)

        // Clamping when source video is 4 FPS
        val lowFpsController = AdaptiveSamplingController()
        val clampedStep = lowFpsController.onSampleEvaluated(0L, sceneCut, newTextDetected = true, detectedFps = 4f)
        assertEquals(250L, clampedStep)
    }
}
