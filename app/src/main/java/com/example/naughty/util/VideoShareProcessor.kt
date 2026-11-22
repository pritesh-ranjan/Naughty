package com.example.naughty.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.example.naughty.data.repository.NoteRepository
import com.example.naughty.notification.ContextNotificationManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

data class VideoMetadata(
    val durationMs: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val reducedWidth: Int,
    val reducedHeight: Int,
    val detectedFps: Float,
    val totalFrames: Int,
    val hasAudioTrack: Boolean,
    val isWithinDurationLimit: Boolean
)

sealed interface VideoProcessingState {
    data object Idle : VideoProcessingState
    data class Processing(
        val progressPercent: Int,
        val stageLabel: String,
        val metadata: VideoMetadata? = null
    ) : VideoProcessingState

    data class Completed(
        val noteId: String,
        val noteTitle: String
    ) : VideoProcessingState

    data class Error(
        val message: String
    ) : VideoProcessingState
}

enum class SamplingMode(val targetFps: Float) {
    STATIC_PRESENTATION(1.0f),
    NORMAL_TALKING(2.5f),
    FAST_CHANGING(4.0f),
    SCENE_OR_TEXT_BURST(8.0f)
}

data class GridDiffResult(
    val globalMeanDelta: Float,
    val maxCellDelta: Float,
    val isSceneCut: Boolean,
    val hasVisualChange: Boolean
)

class AdaptiveSamplingController {
    var currentMode: SamplingMode = SamplingMode.NORMAL_TALKING
        private set

    private var burstUntilMs: Long = -1L
    private var consecutiveStaticCount: Int = 0
    private var consecutiveChangeCount: Int = 0

    fun onSampleEvaluated(
        timestampMs: Long,
        diff: GridDiffResult,
        newTextDetected: Boolean,
        detectedFps: Float
    ): Long {
        if (diff.isSceneCut || newTextDetected) {
            burstUntilMs = timestampMs + BURST_WINDOW_MS
            consecutiveStaticCount = 0
            consecutiveChangeCount++
            currentMode = SamplingMode.SCENE_OR_TEXT_BURST
        } else if (timestampMs < burstUntilMs) {
            currentMode = SamplingMode.SCENE_OR_TEXT_BURST
            if (diff.hasVisualChange) {
                consecutiveStaticCount = 0
            } else {
                consecutiveStaticCount++
            }
        } else if (diff.hasVisualChange) {
            consecutiveStaticCount = 0
            consecutiveChangeCount++
            currentMode = if (consecutiveChangeCount >= 2) {
                SamplingMode.FAST_CHANGING
            } else {
                SamplingMode.NORMAL_TALKING
            }
        } else {
            consecutiveChangeCount = 0
            consecutiveStaticCount++
            currentMode = if (consecutiveStaticCount >= 2) {
                SamplingMode.STATIC_PRESENTATION
            } else {
                SamplingMode.NORMAL_TALKING
            }
        }

        val clampedSourceFps = detectedFps.coerceIn(1f, 120f)
        val effectiveFps = minOf(currentMode.targetFps, clampedSourceFps)
        return (1000f / effectiveFps).toLong().coerceIn(80L, 1000L)
    }

    companion object {
        private const val BURST_WINDOW_MS = 1000L
    }
}

class VideoShareProcessor(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val notificationManager: ContextNotificationManager
) {
    private val processorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    private val _state = MutableStateFlow<VideoProcessingState>(VideoProcessingState.Idle)
    val state: StateFlow<VideoProcessingState> = _state.asStateFlow()

    fun clearCompletedOrError() {
        val current = _state.value
        if (current is VideoProcessingState.Completed || current is VideoProcessingState.Error) {
            _state.value = VideoProcessingState.Idle
        }
    }

    suspend fun inspectVideoUri(uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val hasAudioMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                ?.equals("yes", ignoreCase = true) ?: false
            val frameCountHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
            } else 0

            val (displayW, displayH) = if (rotation == 90 || rotation == 270) rawH to rawW else rawW to rawH
            val (reducedW, reducedH) = computeReducedDimensions(displayW, displayH)
            val detectedFps = detectVideoFpsFromUri(uri, durationMs, frameCountHint)
            val totalFrames = if (frameCountHint > 0) {
                frameCountHint
            } else {
                ((durationMs / 1000f) * detectedFps).toInt().coerceAtLeast(1)
            }

            VideoMetadata(
                durationMs = durationMs,
                originalWidth = displayW,
                originalHeight = displayH,
                reducedWidth = reducedW,
                reducedHeight = reducedH,
                detectedFps = detectedFps,
                totalFrames = totalFrames,
                hasAudioTrack = hasAudioMeta,
                isWithinDurationLimit = isWithinDurationLimit(durationMs)
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun extractPreviewThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 480)
            } else {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun startProcessing(share: PendingVideoShare, preInspectedMetadata: VideoMetadata? = null) {
        activeJob?.cancel()
        _state.value = VideoProcessingState.Processing(
            progressPercent = 5,
            stageLabel = "Preparing video offline...",
            metadata = preInspectedMetadata
        )
        notificationManager.showVideoProcessingProgress(5, "Preparing video offline...")

        activeJob = processorScope.launch {
            var stagedFile: File? = null
            try {
                stagedFile = copyVideoToCache(share.uri)
                if (stagedFile == null || !stagedFile.exists()) {
                    failWith("Could not read shared video file.")
                    return@launch
                }

                val metadata = inspectVideoFile(stagedFile) ?: preInspectedMetadata
                if (metadata == null || metadata.durationMs <= 0L) {
                    failWith("Unsupported or unreadable video format.")
                    return@launch
                }
                if (!metadata.isWithinDurationLimit) {
                    failWith("Video exceeds the 3-minute maximum limit.")
                    return@launch
                }

                updateProgress(
                    percent = 12,
                    label = "Scanning frames at 720p & transcribing audio...",
                    metadata = metadata
                )

                val extractionResult = coroutineScope {
                    val audioDeferred = if (metadata.hasAudioTrack) {
                        async(Dispatchers.IO) {
                            transcribeVideoAudio(stagedFile, metadata.durationMs)
                        }
                    } else {
                        async(Dispatchers.IO) { VideoAudioExtractionResult(null, null) }
                    }

                    val ocrDeferred = async(Dispatchers.IO) {
                        extractAdaptiveOcrFrames(stagedFile, metadata)
                    }

                    val ocrFrames = ocrDeferred.await()
                    updateProgress(
                        percent = 88,
                        label = "Finishing audio transcription & summarizing...",
                        metadata = metadata
                    )
                    val audioResult = audioDeferred.await()
                    ocrFrames to audioResult
                }

                updateProgress(
                    percent = 94,
                    label = "Running semantic summarization...",
                    metadata = metadata
                )

                VideoSummaryDebugCapture.capture(
                    context = context,
                    frames = extractionResult.first,
                    transcript = extractionResult.second.transcript,
                    classification = extractionResult.second.classification
                )

                val summary = VideoNoteSummarizer.summarizeToCrispNoteAsync(
                    context = context,
                    subject = share.subject,
                    accompanyingText = share.text,
                    ocrFrames = extractionResult.first,
                    transcript = extractionResult.second.transcript,
                    audioClassification = extractionResult.second.classification
                )

                val createdNote = noteRepository.createNote(
                    title = summary.title,
                    content = summary.content,
                    cardType = summary.cardType
                )

                _state.value = VideoProcessingState.Completed(
                    noteId = createdNote.id,
                    noteTitle = createdNote.title
                )
                notificationManager.showVideoSummaryReadyNotification(
                    noteId = createdNote.id,
                    noteTitle = createdNote.title
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                notificationManager.dismissVideoProcessingNotification()
                throw e
            } catch (e: Exception) {
                failWith("Failed to process video: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                try {
                    stagedFile?.delete()
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateProgress(percent: Int, label: String, metadata: VideoMetadata?) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = VideoProcessingState.Processing(
            progressPercent = clamped,
            stageLabel = label,
            metadata = metadata
        )
        notificationManager.showVideoProcessingProgress(clamped, label)
    }

    private fun failWith(message: String) {
        notificationManager.dismissVideoProcessingNotification()
        _state.value = VideoProcessingState.Error(message)
    }

    private fun copyVideoToCache(uri: Uri): File? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_videos").apply { mkdirs() }
            val fileName = "vid_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4"
            val destFile = File(cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destFile
        } catch (_: Exception) {
            null
        }
    }

    private fun inspectVideoFile(file: File): VideoMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val hasAudio = SpeechTranscriptionHelper.hasAudioTrack(file) ||
                (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)?.equals("yes", ignoreCase = true) == true)
            val frameCountHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
            } else 0

            val (displayW, displayH) = if (rotation == 90 || rotation == 270) rawH to rawW else rawW to rawH
            val (reducedW, reducedH) = computeReducedDimensions(displayW, displayH)
            val detectedFps = detectVideoFpsFromFile(file, durationMs, frameCountHint)
            val totalFrames = if (frameCountHint > 0) {
                frameCountHint
            } else {
                ((durationMs / 1000f) * detectedFps).toInt().coerceAtLeast(1)
            }

            VideoMetadata(
                durationMs = durationMs,
                originalWidth = displayW,
                originalHeight = displayH,
                reducedWidth = reducedW,
                reducedHeight = reducedH,
                detectedFps = detectedFps,
                totalFrames = totalFrames,
                hasAudioTrack = hasAudio,
                isWithinDurationLimit = isWithinDurationLimit(durationMs)
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun detectVideoFpsFromUri(uri: Uri, durationMs: Long, frameCountHint: Int): Float {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            extractFpsFromExtractor(extractor, durationMs, frameCountHint)
        } catch (_: Exception) {
            fallbackFps(durationMs, frameCountHint)
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun detectVideoFpsFromFile(file: File, durationMs: Long, frameCountHint: Int): Float {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            extractFpsFromExtractor(extractor, durationMs, frameCountHint)
        } catch (_: Exception) {
            fallbackFps(durationMs, frameCountHint)
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun extractFpsFromExtractor(extractor: MediaExtractor, durationMs: Long, frameCountHint: Int): Float {
        var videoTrackIndex = -1
        var videoFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                videoTrackIndex = i
                videoFormat = format
                break
            }
        }
        if (videoTrackIndex < 0 || videoFormat == null) {
            return fallbackFps(durationMs, frameCountHint)
        }

        if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            val rate = try {
                videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
            } catch (_: Exception) {
                try {
                    videoFormat.getFloat(MediaFormat.KEY_FRAME_RATE)
                } catch (_: Exception) {
                    0f
                }
            }
            if (rate in 1f..120f) return rate
        }

        extractor.selectTrack(videoTrackIndex)
        val timestamps = ArrayList<Long>(90)
        while (timestamps.size < 90) {
            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0) break
            timestamps.add(sampleTimeUs)
            if (!extractor.advance()) break
        }

        return computeFpsFromSampleTimestamps(timestamps) ?: fallbackFps(durationMs, frameCountHint)
    }

    private fun fallbackFps(durationMs: Long, frameCountHint: Int): Float {
        if (durationMs > 0L && frameCountHint > 1) {
            val est = frameCountHint / (durationMs / 1000f)
            if (est in 1f..120f) return est
        }
        return 30f
    }

    private data class VideoAudioExtractionResult(
        val transcript: String?,
        val classification: AudioClassificationResult?
    )

    private suspend fun transcribeVideoAudio(
        videoFile: File,
        durationMs: Long
    ): VideoAudioExtractionResult {
        val helper = SpeechTranscriptionHelper(context)
        val deferred = CompletableDeferred<String?>()
        val classificationRef = AtomicReference<AudioClassificationResult?>(null)
        return try {
            helper.transcribeAudioFile(
                audioFile = videoFile,
                onFinal = { text -> deferred.complete(text.trim().takeIf { it.isNotBlank() }) },
                onError = { deferred.complete(null) },
                skipMusicOnly = true,
                enableFormatting = true,
                onClassified = { classificationRef.set(it) }
            )
            val transcript = withTimeoutOrNull(durationMs + 25_000L) {
                deferred.await()
            }
            VideoAudioExtractionResult(transcript, classificationRef.get())
        } catch (_: Exception) {
            VideoAudioExtractionResult(null, classificationRef.get())
        } finally {
            helper.destroy()
        }
    }

    private suspend fun extractAdaptiveOcrFrames(
        videoFile: File,
        metadata: VideoMetadata
    ): List<OcrFrameSample> = coroutineScope {
        val results = mutableListOf<OcrFrameSample>()
        val retriever = MediaMetadataRetriever()
        val recognizer = ImageTextExtractor.createRecognizer()
        val samplingController = AdaptiveSamplingController()
        val seenNormalizedLines = HashSet<String>()

        try {
            retriever.setDataSource(videoFile.absolutePath)
            var currentMs = 0L
            var prevSignature: FloatArray? = null
            var lastReportedPercent = 12

            while (currentMs <= metadata.durationMs && isActive) {
                val frameBitmap = decodeScaledFrame(
                    retriever = retriever,
                    timeUs = currentMs * 1000L,
                    targetWidth = metadata.reducedWidth,
                    targetHeight = metadata.reducedHeight
                )

                var newTextDetected = false
                var diffResult = GridDiffResult(0f, 0f, isSceneCut = false, hasVisualChange = false)

                if (frameBitmap != null) {
                    try {
                        val currSignature = withContext(Dispatchers.Default) {
                            computeGridSignature(frameBitmap)
                        }
                        diffResult = compareGridSignatures(prevSignature, currSignature)

                        if (diffResult.hasVisualChange || prevSignature == null) {
                            prevSignature = currSignature
                            val ocrLines = ImageTextExtractor.extractLinesFromBitmap(
                                bitmap = frameBitmap,
                                sharedRecognizer = recognizer
                            ).getOrNull().orEmpty()
                            val ocrText = ocrLines.joinToString("\n") { it.text }.trim()

                            if (ocrText.isNotBlank()) {
                                results.add(
                                    OcrFrameSample(
                                        timestampMs = currentMs,
                                        text = ocrText,
                                        lines = ocrLines,
                                        frameWidth = frameBitmap.width,
                                        frameHeight = frameBitmap.height
                                    )
                                )
                                val lines = ocrText.lineSequence()
                                    .map { it.lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ").trim() }
                                    .filter { it.length >= 3 }
                                for (line in lines) {
                                    if (seenNormalizedLines.add(line)) {
                                        newTextDetected = true
                                    }
                                }
                            }
                        }
                    } finally {
                        frameBitmap.recycle()
                    }
                }

                val stepMs = samplingController.onSampleEvaluated(
                    timestampMs = currentMs,
                    diff = diffResult,
                    newTextDetected = newTextDetected,
                    detectedFps = metadata.detectedFps
                )

                val progress = 12 + ((currentMs.toFloat() / metadata.durationMs.coerceAtLeast(1L)) * 72f).toInt()
                if (progress - lastReportedPercent >= 5) {
                    lastReportedPercent = progress
                    val modeLabel = when (samplingController.currentMode) {
                        SamplingMode.SCENE_OR_TEXT_BURST -> "Scanning text burst (${samplingController.currentMode.targetFps.toInt()} FPS)..."
                        SamplingMode.FAST_CHANGING -> "Scanning active frames (4 FPS)..."
                        SamplingMode.NORMAL_TALKING -> "Scanning video frames..."
                        SamplingMode.STATIC_PRESENTATION -> "Scanning static slide..."
                    }
                    updateProgress(progress, modeLabel, metadata)
                }

                if (currentMs == metadata.durationMs) break
                val nextMs = currentMs + stepMs
                currentMs = if (nextMs > metadata.durationMs && metadata.durationMs - currentMs > 120L) {
                    metadata.durationMs
                } else {
                    nextMs
                }
            }
        } finally {
            try {
                recognizer.close()
            } catch (_: Exception) {}
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        results
    }

    private fun decodeScaledFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    targetWidth,
                    targetHeight
                )
            } else {
                val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: return null
                if (raw.width > targetWidth || raw.height > targetHeight) {
                    Bitmap.createScaledBitmap(raw, targetWidth, targetHeight, true).also { scaled ->
                        if (scaled != raw) raw.recycle()
                    }
                } else {
                    raw
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MAX_DURATION_MS = 180_500L
        const val MAX_LONG_EDGE_720P = 1280
        const val MAX_SHORT_EDGE_720P = 720
        private const val GRID_SIZE = 8
        private const val SCENE_CUT_THRESHOLD = 18.0f
        private const val LOCAL_CHANGE_THRESHOLD = 10.0f

        fun isWithinDurationLimit(durationMs: Long): Boolean {
            return durationMs in 1L..MAX_DURATION_MS
        }

        /**
         * Computes a 720p-capped target resolution preserving aspect ratio with even dimensions.
         * Never upscales videos whose native resolution is already within the 720p envelope.
         */
        fun computeReducedDimensions(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
            if (originalWidth <= 0 || originalHeight <= 0) return 640 to 360

            val isLandscape = originalWidth >= originalHeight
            val maxW = if (isLandscape) MAX_LONG_EDGE_720P else MAX_SHORT_EDGE_720P
            val maxH = if (isLandscape) MAX_SHORT_EDGE_720P else MAX_LONG_EDGE_720P

            if (originalWidth <= maxW && originalHeight <= maxH) {
                val evenW = (originalWidth / 2 * 2).coerceAtLeast(2)
                val evenH = (originalHeight / 2 * 2).coerceAtLeast(2)
                return evenW to evenH
            }

            val scale = minOf(maxW.toFloat() / originalWidth, maxH.toFloat() / originalHeight)
            val scaledW = ((originalWidth * scale).toInt() / 2 * 2).coerceAtLeast(2)
            val scaledH = ((originalHeight * scale).toInt() / 2 * 2).coerceAtLeast(2)
            return scaledW to scaledH
        }

        /**
         * Computes FPS from presentation timestamps (in microseconds) using median inter-frame delta.
         */
        fun computeFpsFromSampleTimestamps(sampleTimesUs: List<Long>): Float? {
            if (sampleTimesUs.size < 2) return null
            val sorted = sampleTimesUs.filter { it >= 0L }.sorted()
            if (sorted.size < 2) return null

            val deltas = ArrayList<Long>(sorted.size - 1)
            for (i in 1 until sorted.size) {
                val dt = sorted[i] - sorted[i - 1]
                if (dt in 8_000L..1_000_000L) {
                    deltas.add(dt)
                }
            }
            if (deltas.isEmpty()) return null
            deltas.sort()
            val medianDeltaUs = deltas[deltas.size / 2]
            if (medianDeltaUs <= 0L) return null
            return (1_000_000f / medianDeltaUs.toFloat()).coerceIn(1f, 120f)
        }

        fun computeGridSignature(bitmap: Bitmap): FloatArray {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return FloatArray(GRID_SIZE * GRID_SIZE * 2)
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            return computeGridSignatureFromPixels(pixels, w, h)
        }

        /**
         * Computes a 128-element regional grid signature (64 mean luma cells + 64 edge-energy cells)
         * over an 8x8 spatial grid so both global scene cuts and small subtitle updates are detected.
         */
        fun computeGridSignatureFromPixels(pixels: IntArray, width: Int, height: Int): FloatArray {
            val cellCount = GRID_SIZE * GRID_SIZE
            val signature = FloatArray(cellCount * 2)
            if (width <= 0 || height <= 0 || pixels.size < width * height) return signature

            val lumaSums = FloatArray(cellCount)
            val edgeSums = FloatArray(cellCount)
            val counts = IntArray(cellCount)

            val strideX = (width / 64).coerceAtLeast(1)
            val strideY = (height / 64).coerceAtLeast(1)

            var y = 0
            while (y < height) {
                val cellY = ((y * GRID_SIZE) / height).coerceIn(0, GRID_SIZE - 1)
                val rowOffset = y * width
                var prevLuma = -1f
                var x = 0
                while (x < width) {
                    val rgb = pixels[rowOffset + x]
                    val r = (rgb ushr 16) and 0xFF
                    val g = (rgb ushr 8) and 0xFF
                    val b = rgb and 0xFF
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b

                    val cellX = ((x * GRID_SIZE) / width).coerceIn(0, GRID_SIZE - 1)
                    val idx = cellY * GRID_SIZE + cellX
                    lumaSums[idx] += luma
                    if (prevLuma >= 0f) {
                        edgeSums[idx] += abs(luma - prevLuma)
                    }
                    counts[idx]++
                    prevLuma = luma
                    x += strideX
                }
                y += strideY
            }

            for (i in 0 until cellCount) {
                val c = counts[i].coerceAtLeast(1)
                signature[i] = lumaSums[i] / c
                signature[cellCount + i] = edgeSums[i] / c
            }
            return signature
        }

        fun compareGridSignatures(prev: FloatArray?, curr: FloatArray): GridDiffResult {
            val cellCount = GRID_SIZE * GRID_SIZE
            if (prev == null || prev.size < cellCount * 2 || curr.size < cellCount * 2) {
                return GridDiffResult(
                    globalMeanDelta = 255f,
                    maxCellDelta = 255f,
                    isSceneCut = true,
                    hasVisualChange = true
                )
            }

            var totalLumaDelta = 0f
            var maxCellScore = 0f
            for (i in 0 until cellCount) {
                val lumaDelta = abs(curr[i] - prev[i])
                val edgeDelta = abs(curr[cellCount + i] - prev[cellCount + i])
                totalLumaDelta += lumaDelta
                val combinedCell = lumaDelta + 0.8f * edgeDelta
                if (combinedCell > maxCellScore) {
                    maxCellScore = combinedCell
                }
            }

            val globalMean = totalLumaDelta / cellCount
            val isSceneCut = globalMean >= SCENE_CUT_THRESHOLD
            val hasVisualChange = isSceneCut || maxCellScore >= LOCAL_CHANGE_THRESHOLD
            return GridDiffResult(
                globalMeanDelta = globalMean,
                maxCellDelta = maxCellScore,
                isSceneCut = isSceneCut,
                hasVisualChange = hasVisualChange
            )
        }
    }
}
