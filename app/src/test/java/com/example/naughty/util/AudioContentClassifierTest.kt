package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioContentClassifierTest {

    private fun makeScoreVector(vararg indexWeights: Pair<Int, Float>): FloatArray {
        return FloatArray(521).apply {
            for ((idx, value) in indexWeights) {
                this[idx] = value
            }
        }
    }

    @Test
    fun testClassifyWindowScoreVectors_detectsPureMusicAndSkipsTranscription() {
        val windows = listOf(
            makeScoreVector(132 to 0.85f, 24 to 0.70f, 0 to 0.04f),
            makeScoreVector(132 to 0.78f, 261 to 0.62f, 0 to 0.06f),
            makeScoreVector(132 to 0.82f, 24 to 0.65f, 0 to 0.05f)
        )

        val result = AudioContentClassifier.classifyWindowScoreVectors(windows)
        assertEquals(AcousticProfile.MUSIC_ONLY, result.profile)
        assertFalse(result.shouldTranscribeSpeech)
    }

    @Test
    fun testClassifyWindowScoreVectors_detectsSpeechWithBackgroundMusic() {
        val windows = listOf(
            makeScoreVector(0 to 0.48f, 132 to 0.52f),
            makeScoreVector(0 to 0.42f, 262 to 0.46f),
            makeScoreVector(2 to 0.36f, 132 to 0.40f)
        )

        val result = AudioContentClassifier.classifyWindowScoreVectors(windows)
        assertEquals(AcousticProfile.SPEECH_WITH_MUSIC, result.profile)
        assertTrue(result.shouldTranscribeSpeech)
    }

    @Test
    fun testClassifyWindowScoreVectors_detectsCleanSpeech() {
        val windows = listOf(
            makeScoreVector(0 to 0.88f, 132 to 0.03f),
            makeScoreVector(3 to 0.76f, 132 to 0.05f)
        )

        val result = AudioContentClassifier.classifyWindowScoreVectors(windows)
        assertEquals(AcousticProfile.CLEAN_SPEECH, result.profile)
        assertTrue(result.shouldTranscribeSpeech)
    }

    @Test
    fun testBuildWindowOffsets_distributesEvenlyAcrossStream() {
        val offsets = AudioContentClassifier.buildWindowOffsets(totalSamples = 160_000, windowSamples = 15_600, maxWindows = 6)
        assertEquals(6, offsets.size)
        assertEquals(0, offsets.first())
        assertEquals(160_000 - 15_600, offsets.last())
    }
}
