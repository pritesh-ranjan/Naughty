package com.example.naughty.util

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class SpeechTranscriptionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorCode = MutableStateFlow<Int?>(null)
    val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeReadPipe: ParcelFileDescriptor? = null
    private var activeWritePipe: ParcelFileDescriptor? = null
    private var activeStreamJob: Job? = null

    val isOnDeviceAvailable: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            SpeechRecognizer.isRecognitionAvailable(context)
        }

    fun startListening(
        onPartial: ((String) -> Unit)? = null,
        onFinal: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        destroy()

        _errorMessage.value = null
        _errorCode.value = null
        _partialText.value = ""
        _finalText.value = ""
        _soundLevel.value = 0f

        val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            val msg = "Speech recognition service is not available on this device"
            _errorMessage.value = msg
            onError?.invoke(msg)
            return
        }

        speechRecognizer = recognizer

        // Safety watchdog to prevent UI from hanging if SpeechRecognizer stalls
        watchdogJob = scope.launch {
            delay(12000L)
            if (_isListening.value) {
                _isListening.value = false
                val partial = _partialText.value.trim()
                if (partial.isNotBlank()) {
                    _finalText.value = partial
                    onFinal?.invoke(partial)
                } else {
                    val msg = "Speech recognition timed out"
                    _errorMessage.value = msg
                    onError?.invoke(msg)
                }
                destroy()
            }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                watchdogJob?.cancel()
                _isListening.value = false
                _errorCode.value = error
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline speech model not found"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests, please wait"
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported for offline recognition"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Offline speech model not installed on this device"
                    SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot verify language support"
                    SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "Cannot monitor model download"
                    else -> "Speech recognition error ($error)"
                }

                _errorMessage.value = errorMsg
                val partial = _partialText.value.trim()
                if (partial.isNotBlank()) {
                    _finalText.value = partial
                    onFinal?.invoke(partial)
                } else {
                    onError?.invoke(errorMsg)
                }
            }

            override fun onResults(results: Bundle?) {
                watchdogJob?.cancel()
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _finalText.value = text
                    _partialText.value = text
                    onFinal?.invoke(text)
                } else {
                    val fallback = _partialText.value.trim()
                    if (fallback.isNotBlank()) {
                        _finalText.value = fallback
                        onFinal?.invoke(fallback)
                    } else {
                        val msg = "No speech recognized"
                        _errorMessage.value = msg
                        onError?.invoke(msg)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _partialText.value = text
                    onPartial?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            watchdogJob?.cancel()
            val msg = "Could not start speech recognition: ${e.localizedMessage}"
            _errorMessage.value = msg
            _isListening.value = false
            onError?.invoke(msg)
        }
    }

    /**
     * Transcribes an existing recorded audio file offline.
     * Decodes the audio to 16 kHz 16-bit Mono PCM, verifies audio energy, adds VAD silence padding,
     * and streams it smoothly into the SpeechRecognizer pipe in real-time.
     */
    fun transcribeAudioFile(
        audioFile: File,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!audioFile.exists()) {
            onError("Audio file does not exist")
            return
        }

        val existing = AudioRecorderHelper.readTranscript(audioFile)
        if (!existing.isNullOrBlank()) {
            _finalText.value = existing
            onFinal(existing)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onError("Transcribing saved files requires Android 13+")
            return
        }

        destroy()
        _errorMessage.value = null
        _errorCode.value = null
        _partialText.value = ""
        _finalText.value = ""

        val pcm16k = decodeAudioFileToPcm16k(audioFile)
        if (pcm16k == null || pcm16k.isEmpty()) {
            onError("Could not decode audio file")
            return
        }

        // Verify audio contains non-silent speech energy
        val sampleCount = pcm16k.size / 2
        var maxAmp = 0
        val bb = ByteBuffer.wrap(pcm16k).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val sample = bb.short.toInt()
            val absVal = kotlin.math.abs(sample)
            if (absVal > maxAmp) maxAmp = absVal
        }
        if (sampleCount == 0 || maxAmp < 150) {
            onError("No speech detected in audio")
            return
        }

        // Pad with leading (250 ms = 8000 bytes) and trailing (1000 ms = 32000 bytes) silence for VAD
        val leadingSilence = ByteArray(8000)
        val trailingSilence = ByteArray(32000)
        val fullStream = ByteArray(leadingSilence.size + pcm16k.size + trailingSilence.size)
        System.arraycopy(leadingSilence, 0, fullStream, 0, leadingSilence.size)
        System.arraycopy(pcm16k, 0, fullStream, leadingSilence.size, pcm16k.size)
        System.arraycopy(trailingSilence, 0, fullStream, leadingSilence.size + pcm16k.size, trailingSilence.size)

        val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            onError("Speech recognition not available on this device")
            return
        }
        speechRecognizer = recognizer

        val pipes = try {
            ParcelFileDescriptor.createPipe()
        } catch (e: Exception) {
            onError("Cannot create audio stream: ${e.localizedMessage}")
            return
        }
        val readPipe = pipes[0]
        val writePipe = pipes[1]
        activeReadPipe = readPipe
        activeWritePipe = writePipe

        val durationMillis = (sampleCount.toLong() * 1000L) / 16000L
        val watchdogTimeout = maxOf(16000L, durationMillis + 8000L)
        watchdogJob = scope.launch {
            delay(watchdogTimeout)
            if (_isListening.value) {
                _isListening.value = false
                val partial = _partialText.value.trim()
                if (partial.isNotBlank()) {
                    onFinal(partial)
                } else {
                    onError("Transcription timed out")
                }
                destroy()
            }
        }

        val isStreamingStarted = AtomicBoolean(false)
        fun startStreaming() {
            if (!isStreamingStarted.compareAndSet(false, true)) return
            activeStreamJob = scope.launch(Dispatchers.IO) {
                try {
                    FileOutputStream(writePipe.fileDescriptor).use { outputStream ->
                        val chunkSize = 640 // 20 ms at 16 kHz 16-bit mono PCM (320 samples)
                        var offset = 0
                        while (offset < fullStream.size && _isListening.value && isActive) {
                            val count = minOf(chunkSize, fullStream.size - offset)
                            outputStream.write(fullStream, offset, count)
                            offset += count
                            delay(15L) // Real-time streaming
                        }
                        outputStream.flush()
                        // Keep pipe open for recognizer to finish processing
                        delay(2500L)
                    }
                } catch (_: Exception) {
                } finally {
                    try { writePipe.close() } catch (_: Exception) {}
                    activeWritePipe = null
                }
            }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                startStreaming()
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                watchdogJob?.cancel()
                _isListening.value = false
                val partial = _partialText.value.trim()
                if (partial.isNotBlank()) {
                    onFinal(partial)
                } else {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio processing error ($error)"
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline speech model not found"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized in audio"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                        SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected in audio"
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests, please wait"
                        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported for offline recognition"
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Offline speech model not installed"
                        else -> "Transcription error ($error)"
                    }
                    onError(msg)
                }
                destroy()
            }

            override fun onResults(results: Bundle?) {
                watchdogJob?.cancel()
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    _finalText.value = text
                    onFinal(text)
                } else {
                    val fallback = _partialText.value.trim()
                    if (fallback.isNotBlank()) {
                        _finalText.value = fallback
                        onFinal(fallback)
                    } else {
                        onError("No speech recognized in audio")
                    }
                }
                destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    _partialText.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readPipe)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)
        }

        try {
            recognizer.startListening(intent)
            // Fallback: If onReadyForSpeech hasn't triggered streaming after 400ms, start streaming
            scope.launch {
                delay(400L)
                if (!isStreamingStarted.get() && _isListening.value) {
                    startStreaming()
                }
            }
        } catch (e: Exception) {
            watchdogJob?.cancel()
            _isListening.value = false
            onError("Could not start file transcription: ${e.localizedMessage}")
            destroy()
        }
    }

    private fun decodeAudioFileToPcm16k(file: File): ByteArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || trackFormat == null) return null
            extractor.selectTrack(audioTrackIndex)

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isInputEos = false
            var isOutputEos = false

            var actualSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 48000

            var actualChannels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            val rawPcmBaos = ByteArrayOutputStream()

            while (!isOutputEos) {
                if (!isInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isInputEos = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 10000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(chunk)
                        rawPcmBaos.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEos = true
                    }
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        actualSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        actualChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            val rawBytes = rawPcmBaos.toByteArray()
            if (rawBytes.isEmpty()) return null

            return resampleTo16kMono(rawBytes, actualSampleRate, actualChannels)
        } catch (e: Exception) {
            Log.e("SpeechTranscription", "Decoding audio error", e)
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    fun triggerModelDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                val recognizer = speechRecognizer ?: if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { speechRecognizer = it }
                } else null
                recognizer?.triggerModelDownload(intent)
            } catch (e: Exception) {
                _errorMessage.value = "Model download error: ${e.localizedMessage}"
            }
        }
    }

    fun stopListening() {
        watchdogJob?.cancel()
        watchdogJob = null
        activeStreamJob?.cancel()
        activeStreamJob = null
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun destroy() {
        watchdogJob?.cancel()
        watchdogJob = null
        activeStreamJob?.cancel()
        activeStreamJob = null
        _isListening.value = false
        try {
            activeWritePipe?.close()
        } catch (_: Exception) {}
        activeWritePipe = null
        try {
            activeReadPipe?.close()
        } catch (_: Exception) {}
        activeReadPipe = null
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    companion object {
        internal fun resampleTo16kMono(
            rawBytes: ByteArray,
            inputSampleRate: Int,
            inputChannels: Int
        ): ByteArray {
            if (rawBytes.isEmpty()) return ByteArray(0)
            val bytesPerFrame = 2 * inputChannels.coerceAtLeast(1)
            val validByteCount = (rawBytes.size / bytesPerFrame) * bytesPerFrame
            if (validByteCount == 0) return ByteArray(0)

            val totalFrames = validByteCount / bytesPerFrame
            val monoShorts = ShortArray(totalFrames)
            val byteBuffer = ByteBuffer.wrap(rawBytes, 0, validByteCount).order(ByteOrder.LITTLE_ENDIAN)

            if (inputChannels <= 1) {
                for (i in 0 until totalFrames) {
                    monoShorts[i] = byteBuffer.short
                }
            } else {
                for (i in 0 until totalFrames) {
                    var sum = 0
                    for (ch in 0 until inputChannels) {
                        sum += byteBuffer.short.toInt()
                    }
                    monoShorts[i] = (sum / inputChannels).coerceIn(-32768, 32767).toShort()
                }
            }

            val outShorts: ShortArray = when (inputSampleRate) {
                16000 -> monoShorts
                48000 -> {
                    val outLength = monoShorts.size / 3
                    val out = ShortArray(outLength)
                    for (i in 0 until outLength) {
                        val s0 = monoShorts[i * 3].toInt()
                        val s1 = monoShorts[i * 3 + 1].toInt()
                        val s2 = monoShorts[i * 3 + 2].toInt()
                        out[i] = ((s0 + s1 + s2) / 3).coerceIn(-32768, 32767).toShort()
                    }
                    out
                }
                else -> {
                    val ratio = inputSampleRate.toDouble() / 16000.0
                    val outLength = (monoShorts.size / ratio).toInt()
                    val out = ShortArray(outLength)
                    for (i in 0 until outLength) {
                        val srcIdx = i * ratio
                        val idx0 = srcIdx.toInt().coerceIn(0, monoShorts.size - 1)
                        val idx1 = (idx0 + 1).coerceIn(0, monoShorts.size - 1)
                        val frac = (srcIdx - idx0).toFloat()
                        val interp = (monoShorts[idx0] * (1f - frac) + monoShorts[idx1] * frac).toInt()
                        out[i] = interp.coerceIn(-32768, 32767).toShort()
                    }
                    out
                }
            }

            val outBytes = ByteArray(outShorts.size * 2)
            val outBb = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (s in outShorts) {
                outBb.putShort(s)
            }
            return outBytes
        }
    }
}
