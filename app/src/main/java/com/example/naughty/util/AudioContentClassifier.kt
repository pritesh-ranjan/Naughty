package com.example.naughty.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

enum class AcousticProfile {
    CLEAN_SPEECH,
    SPEECH_WITH_MUSIC,
    MUSIC_ONLY,
    SILENT_OR_NOISE
}

data class AudioClassificationResult(
    val profile: AcousticProfile,
    val speechScore: Float,
    val musicScore: Float,
    val speechWindowRatio: Float
) {
    val shouldTranscribeSpeech: Boolean
        get() = profile == AcousticProfile.CLEAN_SPEECH || profile == AcousticProfile.SPEECH_WITH_MUSIC
}

/**
 * Offline acoustic event classifier backed by Google's pre-trained YAMNet LiteRT model (`yamnet.tflite`).
 * Classifies 16 kHz 16-bit mono PCM streams into Speech vs. Music/Singing so pure-music tracks
 * bypass [android.speech.SpeechRecognizer] and speech-over-music tracks are flagged for stricter
 * cross-modal relevance filtering in [VideoNoteSummarizer].
 */
object AudioContentClassifier {

    private const val TAG = "AudioContentClassifier"
    private const val MODEL_ASSET_NAME = "yamnet.tflite"
    internal const val YAMNET_WINDOW_SAMPLES = 15600 // 0.975s at 16 kHz
    private const val MAX_EVALUATED_WINDOWS = 12
    private const val NUM_CLASSES = 521
    private const val MIN_WINDOW_AMP = 150f

    // Exact AudioSet / YAMNet label indices verified against yamnet_label_list.txt
    private val SPEECH_INDICES = intArrayOf(0, 1, 2, 3, 5) // Speech, Child speech, Conversation, Narration, Synthesizer
    private val SINGING_RANGE = 24..29 // Singing, Choir, Yodeling, Chant, Mantra, Child singing
    private val MUSIC_RANGE = 132..276 // Music .. Scary music

    @Volatile
    private var cachedModelBuffer: MappedByteBuffer? = null

    fun classifyPcm16k(context: Context, pcm16k: ByteArray): AudioClassificationResult {
        val totalSamples = pcm16k.size / 2
        if (totalSamples == 0) {
            return AudioClassificationResult(AcousticProfile.SILENT_OR_NOISE, 0f, 0f, 0f)
        }

        // Convert little-endian 16-bit PCM to normalized [-1f, 1f] floats and check global energy
        val floatSamples = FloatArray(totalSamples)
        val bb = ByteBuffer.wrap(pcm16k).order(ByteOrder.LITTLE_ENDIAN)
        var maxAmp = 0
        for (i in 0 until totalSamples) {
            val s = bb.short.toInt()
            val a = abs(s)
            if (a > maxAmp) maxAmp = a
            floatSamples[i] = s / 32768f
        }
        if (maxAmp < MIN_WINDOW_AMP) {
            return AudioClassificationResult(AcousticProfile.SILENT_OR_NOISE, 0f, 0f, 0f)
        }

        val modelBuffer = getOrLoadModelBuffer(context) ?: return AudioClassificationResult(
            profile = AcousticProfile.CLEAN_SPEECH,
            speechScore = 1f,
            musicScore = 0f,
            speechWindowRatio = 1f
        )

        var interpreter: Interpreter? = null
        return try {
            val options = Interpreter.Options().apply { setNumThreads(2) }
            interpreter = Interpreter(modelBuffer, options)

            val inputShape = interpreter.getInputTensor(0).shape()
            val needsBatchDim = inputShape.size == 2
            val outputShape = interpreter.getOutputTensor(0).shape()
            val outRows = outputShape.firstOrNull()?.coerceAtLeast(1) ?: 1
            val outCols = outputShape.lastOrNull()?.coerceAtLeast(NUM_CLASSES) ?: NUM_CLASSES

            val offsets = buildWindowOffsets(totalSamples, YAMNET_WINDOW_SAMPLES, MAX_EVALUATED_WINDOWS)
            val windowScoresList = ArrayList<FloatArray>(offsets.size)

            for (offset in offsets) {
                val window = FloatArray(YAMNET_WINDOW_SAMPLES)
                val copyLen = minOf(YAMNET_WINDOW_SAMPLES, totalSamples - offset)
                System.arraycopy(floatSamples, offset, window, 0, copyLen)

                // Skip dead-silent middle windows so they don't dilute active speech/music ratios
                if (window.maxOf { abs(it) * 32768f } < MIN_WINDOW_AMP) continue

                val outputArray = Array(outRows) { FloatArray(outCols) }
                val inputObj: Any = if (needsBatchDim) arrayOf(window) else window
                interpreter.run(inputObj, outputArray)

                val avgScores = FloatArray(NUM_CLASSES)
                for (r in 0 until outRows) {
                    val row = outputArray[r]
                    for (c in 0 until minOf(NUM_CLASSES, row.size)) {
                        avgScores[c] += row[c]
                    }
                }
                if (outRows > 1) {
                    val inv = 1f / outRows.toFloat()
                    for (c in 0 until NUM_CLASSES) {
                        avgScores[c] *= inv
                    }
                }
                windowScoresList.add(avgScores)
            }

            if (windowScoresList.isEmpty()) {
                AudioClassificationResult(AcousticProfile.SILENT_OR_NOISE, 0f, 0f, 0f)
            } else {
                classifyWindowScoreVectors(windowScoresList)
            }
        } catch (e: Exception) {
            Log.w(TAG, "YAMNet inference failed, falling back to permissive speech profile: ${e.message}")
            AudioClassificationResult(AcousticProfile.CLEAN_SPEECH, 1f, 0f, 1f)
        } finally {
            try {
                interpreter?.close()
            } catch (_: Exception) {}
        }
    }

    internal fun buildWindowOffsets(
        totalSamples: Int,
        windowSamples: Int = YAMNET_WINDOW_SAMPLES,
        maxWindows: Int = MAX_EVALUATED_WINDOWS
    ): List<Int> {
        if (totalSamples <= 0) return emptyList()
        if (totalSamples <= windowSamples) return listOf(0)

        val maxStart = totalSamples - windowSamples
        val possibleWindows = (totalSamples / windowSamples).coerceAtLeast(1)
        val count = minOf(maxWindows, possibleWindows + 1).coerceAtLeast(2)
        return (0 until count).map { idx ->
            ((idx.toLong() * maxStart) / (count - 1).coerceAtLeast(1)).toInt()
        }.distinct()
    }

    internal fun classifyWindowScoreVectors(windowScores: List<FloatArray>): AudioClassificationResult {
        if (windowScores.isEmpty()) {
            return AudioClassificationResult(AcousticProfile.SILENT_OR_NOISE, 0f, 0f, 0f)
        }

        var speechSum = 0f
        var musicSum = 0f
        var speechWindows = 0

        for (scores in windowScores) {
            var windowSpeech = 0f
            for (idx in SPEECH_INDICES) {
                if (idx < scores.size && scores[idx] > windowSpeech) {
                    windowSpeech = scores[idx]
                }
            }

            var windowMusic = 0f
            for (idx in SINGING_RANGE) {
                if (idx < scores.size && scores[idx] > windowMusic) {
                    windowMusic = scores[idx]
                }
            }
            for (idx in MUSIC_RANGE) {
                if (idx < scores.size && scores[idx] > windowMusic) {
                    windowMusic = scores[idx]
                }
            }

            speechSum += windowSpeech
            musicSum += windowMusic
            if (windowSpeech >= 0.20f) {
                speechWindows++
            }
        }

        val count = windowScores.size.toFloat()
        val avgSpeech = speechSum / count
        val avgMusic = musicSum / count
        val speechRatio = speechWindows.toFloat() / count

        val profile = when {
            avgMusic >= 0.40f && avgSpeech < 0.18f && speechRatio < 0.20f -> AcousticProfile.MUSIC_ONLY
            avgMusic >= 0.55f && avgSpeech < 0.22f && avgSpeech < avgMusic * 0.35f -> AcousticProfile.MUSIC_ONLY
            avgSpeech >= 0.18f && avgMusic >= 0.30f -> AcousticProfile.SPEECH_WITH_MUSIC
            avgSpeech < 0.08f && avgMusic < 0.12f -> AcousticProfile.SILENT_OR_NOISE
            else -> AcousticProfile.CLEAN_SPEECH
        }

        return AudioClassificationResult(
            profile = profile,
            speechScore = avgSpeech,
            musicScore = avgMusic,
            speechWindowRatio = speechRatio
        )
    }

    private fun getOrLoadModelBuffer(context: Context): MappedByteBuffer? {
        cachedModelBuffer?.let { return it }
        return synchronized(this) {
            cachedModelBuffer ?: try {
                val afd: AssetFileDescriptor = context.assets.openFd(MODEL_ASSET_NAME)
                afd.use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).use { input ->
                        input.channel.map(
                            FileChannel.MapMode.READ_ONLY,
                            descriptor.startOffset,
                            descriptor.declaredLength
                        )
                    }
                }.also { cachedModelBuffer = it }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to load $MODEL_ASSET_NAME from assets: ${e.message}")
                null
            }
        }
    }
}
