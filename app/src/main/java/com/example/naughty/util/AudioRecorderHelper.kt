package com.example.naughty.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import android.util.Log
import com.audx.android.Audx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

data class AudioRecordResult(
    val file: File,
    val durationMillis: Long,
    val amplitudes: List<Float>
)

/**
 * Real-time audio recording pipeline featuring:
 * 1. Low-latency 48 kHz PCM16 audio capture via [AudioRecord] (UNPROCESSED / VOICE_RECOGNITION).
 * 2. Hardware noise suppression disabled where possible to avoid double-processing.
 * 3. On-device [Audx] neural background noise suppression (RNNoise model, 10-ms 480-sample frames).
 * 4. Runtime toggle to enable/disable AI noise cancellation on the fly.
 * 5. Direct native [MediaCodec] Opus encoding (32 kbps) streamed into compliant .opus (Ogg) files.
 */
class AudioRecorderHelper(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderHelper"
        const val SAMPLE_RATE = 48000
        const val FRAME_SIZE_SAMPLES = 480 // 10 ms at 48 kHz
        const val OPUS_FRAME_SAMPLES = 960 // 20 ms at 48 kHz
        private const val TARGET_BARS = 40

        fun normalizeAmplitudes(samples: List<Float>, targetCount: Int = TARGET_BARS): List<Float> {
            if (samples.isEmpty()) {
                return List(targetCount) { 0.18f }
            }
            if (samples.size <= targetCount) {
                val padded = samples.toMutableList()
                while (padded.size < targetCount) {
                    padded.add(0.12f)
                }
                return padded
            }

            val chunkSize = samples.size.toFloat() / targetCount
            val result = mutableListOf<Float>()
            for (i in 0 until targetCount) {
                val startIdx = (i * chunkSize).toInt().coerceIn(0, samples.size - 1)
                val endIdx = ((i + 1) * chunkSize).toInt().coerceIn(startIdx + 1, samples.size)
                val subList = samples.subList(startIdx, endIdx)
                val peak = if (subList.isNotEmpty()) subList.maxOrNull() ?: 0.15f else 0.15f
                result.add(peak.coerceIn(0.12f, 1.0f))
            }
            return result
        }

        private class SimpleLruCache<K, V>(private val maxEntries: Int) {
            private val map = object : LinkedHashMap<K, V>(maxEntries, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                    return size > maxEntries
                }
            }
            @Synchronized fun get(key: K): V? = map[key]
            @Synchronized fun put(key: K, value: V) { map[key] = value }
            @Synchronized fun clear() { map.clear() }
        }

        private val waveformCache = SimpleLruCache<String, List<Float>>(128)
        private val durationCache = SimpleLruCache<String, String>(128)

        fun saveWaveform(audioFile: File, amplitudes: List<Float>) {
            try {
                waveformCache.put("${audioFile.absolutePath}_${amplitudes.size}", amplitudes)
                val wfFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.wf")
                wfFile.writeText(amplitudes.joinToString(",") { "%.3f".format(java.util.Locale.US, it) })
            } catch (_: Exception) {}
        }

        fun readWaveform(audioFile: File, targetCount: Int = TARGET_BARS): List<Float> {
            val cacheKey = "${audioFile.absolutePath}_$targetCount"
            val cached = waveformCache.get(cacheKey)
            if (cached != null) return cached

            try {
                val wfFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.wf")
                if (wfFile.exists()) {
                    val content = wfFile.readText()
                    val parsed = content.split(",").mapNotNull { it.trim().toFloatOrNull() }
                    if (parsed.isNotEmpty()) {
                        val result = if (parsed.size == targetCount) parsed else normalizeAmplitudes(parsed, targetCount)
                        waveformCache.put(cacheKey, result)
                        return result
                    }
                }
            } catch (_: Exception) {}

            val seed = audioFile.name.hashCode()
            val rng = java.util.Random(seed.toLong())
            val fallback = List(targetCount) {
                (0.12f + rng.nextFloat() * 0.75f).coerceIn(0.08f, 0.95f)
            }
            waveformCache.put(cacheKey, fallback)
            return fallback
        }

        fun getAudioDuration(audioFile: File): String? {
            if (!audioFile.exists()) return null
            val key = audioFile.absolutePath
            val cached = durationCache.get(key)
            if (cached != null) return cached

            return try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(audioFile.absolutePath)
                val durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                mmr.release()
                val durMs = durationStr?.toLongOrNull() ?: 0L
                if (durMs > 0) {
                    val totalSec = durMs / 1000
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    val formatted = "%d:%02d".format(java.util.Locale.US, min, sec)
                    durationCache.put(key, formatted)
                    formatted
                } else null
            } catch (_: Exception) {
                null
            }
        }

        fun saveTranscript(audioFile: File, text: String) {
            if (text.isBlank()) return
            try {
                val txtFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.txt")
                txtFile.writeText(text.trim())
            } catch (_: Exception) {}
        }

        fun readTranscript(audioFile: File): String? {
            return try {
                val txtFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.txt")
                if (txtFile.exists()) txtFile.readText().trim().ifBlank { null } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var audxInstance: Audx? = null
    private var opusEncoder: OpusMediaCodecEncoder? = null
    private var workerThread: Thread? = null
    private var encoderThread: Thread? = null
    private val pcmQueue = java.util.concurrent.LinkedBlockingQueue<ShortArray>(64)

    private val isRecordingRunning = AtomicBoolean(false)
    private val isPausedAtomic = AtomicBoolean(false)
    private val isNoiseSuppressionEnabledAtomic = AtomicBoolean(true)

    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var totalRecordedDuration = 0L

    private val rawAmplitudes = mutableListOf<Float>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var timerJob: Job? = null

    private val _isNoiseSuppressionEnabled = MutableStateFlow(true)
    val isNoiseSuppressionEnabled: StateFlow<Boolean> = _isNoiseSuppressionEnabled.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0.12f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _liveAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val liveAmplitudes: StateFlow<List<Float>> = _liveAmplitudes.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /**
     * Toggles or sets AI background noise suppression in real-time.
     * Can be invoked before recording starts or dynamically mid-stream.
     */
    fun setNoiseSuppressionEnabled(enabled: Boolean) {
        isNoiseSuppressionEnabledAtomic.set(enabled)
        _isNoiseSuppressionEnabled.value = enabled
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): File {
        val audioDir = File(context.filesDir, "note_audio").apply { mkdirs() }
        val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.opus")
        currentOutputFile = outputFile

        pcmQueue.clear()
        rawAmplitudes.clear()
        _liveAmplitudes.value = emptyList()
        _elapsedMillis.value = 0L
        _currentAmplitude.value = 0.12f
        totalRecordedDuration = 0L
        isPausedAtomic.set(false)
        isRecordingRunning.set(true)

        // 1. Choose optimal audio source: VOICE_RECOGNITION provides stable hardware AGC & voice tuning
        val audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION

        // 2. Configure AudioRecord with generous buffer (at least 1 second headroom)
        var channelConfig = AudioFormat.CHANNEL_IN_MONO
        var isStereo = false
        var minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

        if (minBufferSize <= 0) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO
            isStereo = true
            minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        }

        val bufferSize = maxOf(minBufferSize * 8, SAMPLE_RATE * 2)

        val record = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord = record

        // 3. Disable platform noise suppression to prevent double-filtering
        if (NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                    enabled = false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disable hardware NoiseSuppressor", e)
            }
        }

        // 4. Initialize Audx with default RNNoise model and VoIP resampler quality
        audxInstance = try {
            Audx.Builder()
                .inputRate(SAMPLE_RATE)
                .resampleQuality(Audx.AUDX_RESAMPLER_QUALITY_VOIP)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Audx initialization error, falling back to passthrough", e)
            null
        }

        // 5. Initialize Opus MediaCodec Encoder (standard 20 ms frames)
        opusEncoder = OpusMediaCodecEncoder(
            outputFile = outputFile,
            sampleRate = SAMPLE_RATE,
            channelCount = 1,
            bitRate = 32000
        )

        record.startRecording()
        recordingStartTime = System.currentTimeMillis()
        _isRecording.value = true

        // 6. Launch independent producer (capture) and consumer (encoder) threads
        startEncoderThread()
        startCaptureThread(record, isStereo)
        startTimerJob()

        return outputFile
    }

    private fun startCaptureThread(record: AudioRecord, isStereo: Boolean) {
        workerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            val rawInputShorts = ShortArray(if (isStereo) FRAME_SIZE_SAMPLES * 2 else FRAME_SIZE_SAMPLES)
            val monoInputShorts = ShortArray(FRAME_SIZE_SAMPLES)
            val processedShorts = ShortArray(FRAME_SIZE_SAMPLES)

            // Accumulator for 20 ms Opus frames (960 samples)
            val opusFrameBuffer = ShortArray(OPUS_FRAME_SAMPLES)
            var opusBufferOffset = 0
            var amplitudeUpdateCounter = 0

            while (isRecordingRunning.get()) {
                val shortsToRead = rawInputShorts.size
                var totalRead = 0

                while (totalRead < shortsToRead && isRecordingRunning.get()) {
                    val read = record.read(rawInputShorts, totalRead, shortsToRead - totalRead)
                    if (read > 0) {
                        totalRead += read
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord read error: $read")
                        break
                    }
                }

                if (totalRead != shortsToRead || !isRecordingRunning.get()) {
                    continue
                }

                // If stereo, downmix to mono (L + R) / 2
                if (isStereo) {
                    for (i in 0 until FRAME_SIZE_SAMPLES) {
                        val left = rawInputShorts[i * 2].toInt()
                        val right = rawInputShorts[i * 2 + 1].toInt()
                        monoInputShorts[i] = ((left + right) / 2).coerceIn(-32768, 32767).toShort()
                    }
                } else {
                    System.arraycopy(rawInputShorts, 0, monoInputShorts, 0, FRAME_SIZE_SAMPLES)
                }

                // Process via Audx RNNoise or passthrough if bypassed
                val isDenoiseActive = isNoiseSuppressionEnabledAtomic.get()
                val audx = audxInstance
                if (isDenoiseActive && audx != null && !audx.isClosed()) {
                    try {
                        audx.process(monoInputShorts, processedShorts) { _ -> }
                    } catch (e: Exception) {
                        System.arraycopy(monoInputShorts, 0, processedShorts, 0, FRAME_SIZE_SAMPLES)
                    }
                } else {
                    System.arraycopy(monoInputShorts, 0, processedShorts, 0, FRAME_SIZE_SAMPLES)
                }

                // Calculate responsive amplitude with root-power scaling
                amplitudeUpdateCounter++
                if (amplitudeUpdateCounter % 4 == 0) { // ~ every 40 ms
                    var sumSquares = 0.0
                    for (sample in processedShorts) {
                        sumSquares += sample.toDouble() * sample.toDouble()
                    }
                    val rms = sqrt(sumSquares / FRAME_SIZE_SAMPLES).toFloat()
                    val normalized = (sqrt((rms / 2500f).coerceIn(0f, 1f))).coerceIn(0.12f, 1.0f)

                    _currentAmplitude.value = normalized
                    synchronized(rawAmplitudes) {
                        rawAmplitudes.add(normalized)
                        _liveAmplitudes.value = rawAmplitudes.takeLast(45)
                    }
                }

                // Accumulate two 10 ms frames into one 20 ms (960-sample) Opus frame
                if (!isPausedAtomic.get()) {
                    System.arraycopy(processedShorts, 0, opusFrameBuffer, opusBufferOffset, FRAME_SIZE_SAMPLES)
                    opusBufferOffset += FRAME_SIZE_SAMPLES

                    if (opusBufferOffset >= OPUS_FRAME_SAMPLES) {
                        val chunkCopy = opusFrameBuffer.clone()
                        pcmQueue.offer(chunkCopy)
                        opusBufferOffset = 0
                    }
                } else {
                    opusBufferOffset = 0
                }
            }

            // Flush any remaining accumulated samples
            if (opusBufferOffset > 0 && !isPausedAtomic.get()) {
                val partialChunk = ShortArray(opusBufferOffset)
                System.arraycopy(opusFrameBuffer, 0, partialChunk, 0, opusBufferOffset)
                pcmQueue.offer(partialChunk)
            }
        }, "AudxAudioRecordThread").apply { start() }
    }

    private fun startEncoderThread() {
        encoderThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            while (isRecordingRunning.get() || pcmQueue.isNotEmpty()) {
                val chunk = try {
                    pcmQueue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    null
                }

                if (chunk != null) {
                    if (chunk.isEmpty()) {
                        break // End-of-stream marker
                    }
                    if (!isPausedAtomic.get()) {
                        opusEncoder?.encodePcm(chunk, 0, chunk.size)
                    }
                }
            }
        }, "OpusEncoderWorkerThread").apply { start() }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _isRecording.value) {
                if (!isPausedAtomic.get()) {
                    _elapsedMillis.value = totalRecordedDuration + (System.currentTimeMillis() - recordingStartTime)
                }
                delay(100)
            }
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !isPausedAtomic.get()) {
            isPausedAtomic.set(true)
            totalRecordedDuration += (System.currentTimeMillis() - recordingStartTime)
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && isPausedAtomic.get()) {
            isPausedAtomic.set(false)
            recordingStartTime = System.currentTimeMillis()
        }
    }

    fun stopRecording(): AudioRecordResult? {
        val file = currentOutputFile ?: return null
        timerJob?.cancel()
        _isRecording.value = false
        isRecordingRunning.set(false)

        val duration = if (isPausedAtomic.get()) {
            totalRecordedDuration
        } else {
            totalRecordedDuration + (System.currentTimeMillis() - recordingStartTime)
        }

        // Wait for capture thread to exit cleanly
        try {
            workerThread?.join(500)
        } catch (_: Exception) {}
        workerThread = null

        // Stop and release AudioRecord
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        // Signal EOS marker to encoder queue and join encoder thread
        pcmQueue.offer(ShortArray(0))
        try {
            encoderThread?.join(600)
        } catch (_: Exception) {}
        encoderThread = null

        // Release hardware noise suppressor
        try {
            noiseSuppressor?.release()
        } catch (_: Exception) {}
        noiseSuppressor = null

        // Close Audx native resources
        try {
            audxInstance?.close()
        } catch (_: Exception) {}
        audxInstance = null

        // Finalize Opus MediaCodec and MediaMuxer
        try {
            opusEncoder?.stopAndRelease()
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing Opus encoder", e)
        }
        opusEncoder = null

        val finalBars = synchronized(rawAmplitudes) {
            normalizeAmplitudes(rawAmplitudes, targetCount = TARGET_BARS)
        }
        saveWaveform(file, finalBars)

        currentOutputFile = null
        return AudioRecordResult(
            file = file,
            durationMillis = duration.coerceAtLeast(500L),
            amplitudes = finalBars
        )
    }

    fun cancelRecording() {
        timerJob?.cancel()
        _isRecording.value = false
        isRecordingRunning.set(false)

        try {
            workerThread?.join(500)
        } catch (_: Exception) {}
        workerThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        pcmQueue.clear()
        pcmQueue.offer(ShortArray(0))
        try {
            encoderThread?.join(500)
        } catch (_: Exception) {}
        encoderThread = null

        try {
            noiseSuppressor?.release()
        } catch (_: Exception) {}
        noiseSuppressor = null

        try {
            audxInstance?.close()
        } catch (_: Exception) {}
        audxInstance = null

        try {
            opusEncoder?.stopAndRelease()
        } catch (_: Exception) {}
        opusEncoder = null

        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
