package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpeechTranscriptionHelperTest {

    @Test
    fun testResample48kMonoTo16kMono() {
        // 48 kHz mono -> 3 samples produce 1 sample at 16 kHz
        // 9 samples at 48 kHz -> 3 samples at 16 kHz
        val sampleCount = 9
        val bytes = ByteArray(sampleCount * 2)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Triplets: (300, 600, 900) -> avg 600; (1000, 2000, 3000) -> avg 2000; (-100, -200, -300) -> avg -200
        val inputs = shortArrayOf(300, 600, 900, 1000, 2000, 3000, -100, -200, -300)
        for (s in inputs) {
            bb.putShort(s)
        }

        val resampled = SpeechTranscriptionHelper.resampleTo16kMono(bytes, 48000, 1)
        assertEquals(3 * 2, resampled.size) // 3 samples, 6 bytes

        val outBb = ByteBuffer.wrap(resampled).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(600.toShort(), outBb.short)
        assertEquals(2000.toShort(), outBb.short)
        assertEquals((-200).toShort(), outBb.short)
    }

    @Test
    fun testResampleStereoToMono() {
        // Stereo 16 kHz: 2 frames = 4 shorts (8 bytes) -> 2 mono shorts (4 bytes)
        val frameCount = 2
        val bytes = ByteArray(frameCount * 4) // 2 channels * 2 bytes
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Frame 1: L=1000, R=3000 -> avg 2000
        bb.putShort(1000)
        bb.putShort(3000)
        // Frame 2: L=-500, R=-1500 -> avg -1000
        bb.putShort(-500)
        bb.putShort(-1500)

        val resampled = SpeechTranscriptionHelper.resampleTo16kMono(bytes, 16000, 2)
        assertEquals(frameCount * 2, resampled.size)

        val outBb = ByteBuffer.wrap(resampled).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2000.toShort(), outBb.short)
        assertEquals((-1000).toShort(), outBb.short)
    }

    @Test
    fun testEmptyInputHandling() {
        val emptyResult = SpeechTranscriptionHelper.resampleTo16kMono(ByteArray(0), 48000, 1)
        assertEquals(0, emptyResult.size)

        val partialFrame = SpeechTranscriptionHelper.resampleTo16kMono(ByteArray(1), 48000, 1)
        assertEquals(0, partialFrame.size)
    }
}
