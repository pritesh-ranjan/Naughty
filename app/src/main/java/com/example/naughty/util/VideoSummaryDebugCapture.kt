package com.example.naughty.util

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File

/**
 * Debug-only diagnostic recorder that snapshots extracted OCR lines (with geometry,
 * confidence, and angle), speech transcript, and acoustic classification to app cache
 * so real-world video inputs can be inspected and replayed as JVM golden test fixtures.
 */
object VideoSummaryDebugCapture {

    private const val DIR_NAME = "video_debug"

    fun capture(
        context: Context,
        frames: List<OcrFrameSample>,
        transcript: String?,
        classification: AudioClassificationResult?
    ) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return

        try {
            val rootDir = File(context.cacheDir, DIR_NAME)
            val latestDir = File(rootDir, "latest").apply { mkdirs() }
            val runDir = File(rootDir, System.currentTimeMillis().toString()).apply { mkdirs() }

            val tsvContent = buildString {
                appendLine("timestampMs\tframeW\tframeH\tblockIndex\tleft\ttop\tright\tbottom\tconfidence\tangle\ttext")
                for (frame in frames) {
                    if (frame.lines.isNotEmpty()) {
                        for (line in frame.lines) {
                            val sanitizedText = line.text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
                            append(frame.timestampMs).append('\t')
                            append(frame.frameWidth).append('\t')
                            append(frame.frameHeight).append('\t')
                            append(line.blockIndex).append('\t')
                            append(line.left).append('\t')
                            append(line.top).append('\t')
                            append(line.right).append('\t')
                            append(line.bottom).append('\t')
                            append(line.confidence).append('\t')
                            append(line.angle).append('\t')
                            appendLine(sanitizedText)
                        }
                    } else {
                        for (rawLine in frame.text.lineSequence()) {
                            val sanitizedText = rawLine.replace('\t', ' ').trim()
                            if (sanitizedText.isNotEmpty()) {
                                append(frame.timestampMs).append('\t')
                                append(frame.frameWidth).append('\t')
                                append(frame.frameHeight).append('\t')
                                append("-1\t0\t0\t0\t0\t1.0\t0.0\t")
                                appendLine(sanitizedText)
                            }
                        }
                    }
                }
            }

            val metaContent = buildString {
                appendLine("profile=${classification?.profile?.name ?: "UNKNOWN"}")
                appendLine("speechScore=${classification?.speechScore ?: 0f}")
                appendLine("musicScore=${classification?.musicScore ?: 0f}")
                appendLine("speechWindowRatio=${classification?.speechWindowRatio ?: 0f}")
                appendLine("frameCount=${frames.size}")
            }

            val transcriptText = transcript.orEmpty()

            for (dir in listOf(latestDir, runDir)) {
                File(dir, "frames.tsv").writeText(tsvContent, Charsets.UTF_8)
                File(dir, "transcript.txt").writeText(transcriptText, Charsets.UTF_8)
                File(dir, "meta.properties").writeText(metaContent, Charsets.UTF_8)
            }
        } catch (_: Throwable) {
            // Best-effort debug capture; never fail video summarization
        }
    }
}
