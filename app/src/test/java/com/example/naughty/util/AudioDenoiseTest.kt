package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioDenoiseTest {

    @Test
    fun testFrameDurationCalculation() {
        val sampleRate = 48000
        val frameSize = 480
        val frameDurationUs = (frameSize * 1_000_000L) / sampleRate

        // 480 samples at 48 kHz must equal exactly 10,000 microseconds (10 ms)
        assertEquals(10_000L, frameDurationUs)
    }

    @Test
    fun testStereoToMonoDownmix() {
        val frameSize = 480
        val stereoShorts = ShortArray(frameSize * 2)
        val monoShorts = ShortArray(frameSize)

        // Populate with synthetic stereo values
        for (i in 0 until frameSize) {
            stereoShorts[i * 2] = 1000 // Left channel
            stereoShorts[i * 2 + 1] = 3000 // Right channel
        }

        for (i in 0 until frameSize) {
            val left = stereoShorts[i * 2].toInt()
            val right = stereoShorts[i * 2 + 1].toInt()
            monoShorts[i] = ((left + right) / 2).coerceIn(-32768, 32767).toShort()
        }

        // (1000 + 3000) / 2 = 2000
        for (i in 0 until frameSize) {
            assertEquals(2000.toShort(), monoShorts[i])
        }
    }

    @Test
    fun testNormalizeAmplitudesTargetBars() {
        val sampleList = (1..200).map { (it % 100) / 100f }
        val normalized = AudioRecorderHelper.normalizeAmplitudes(sampleList, targetCount = 40)

        assertEquals(40, normalized.size)
        for (bar in normalized) {
            assertTrue(bar in 0.05f..1.0f)
        }
    }

    @Test
    fun testWaveformPersistenceRoundtrip() {
        val tempFile = File.createTempFile("test_audio", ".opus")
        val testBars = listOf(0.1f, 0.5f, 0.9f, 0.2f, 0.8f)

        try {
            AudioRecorderHelper.saveWaveform(tempFile, testBars)
            val readBars = AudioRecorderHelper.readWaveform(tempFile, targetCount = 5)

            assertEquals(5, readBars.size)
            assertEquals(0.1f, readBars[0], 0.01f)
            assertEquals(0.5f, readBars[1], 0.01f)
            assertEquals(0.9f, readBars[2], 0.01f)
        } finally {
            tempFile.delete()
            File(tempFile.parentFile, "${tempFile.nameWithoutExtension}.wf").delete()
        }
    }
}
