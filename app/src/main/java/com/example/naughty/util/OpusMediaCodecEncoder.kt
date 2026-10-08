package com.example.naughty.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance, streaming Opus audio encoder and Ogg muxer.
 *
 * Encodes 16-bit PCM at 48 kHz (mono) using Android's native MediaCodec Opus encoder
 * (target bitrate: 32 kbps for studio-grade voice clarity).
 *
 * Encapsulation:
 * - On Android API 29+ (Q and newer): Streams directly via native [MediaMuxer] with
 *   [MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG], creating compliant .opus files with zero temp files.
 * - On API < 29: Encapsulates MediaCodec Opus packets directly into a lightweight standard
 *   Ogg Opus container stream (OpusHead + OpusTags + Ogg pages with CRC32).
 */
class OpusMediaCodecEncoder(
    private val outputFile: File,
    private val sampleRate: Int = 48000,
    private val channelCount: Int = 1,
    private val bitRate: Int = 32000
) {
    companion object {
        private const val TAG = "OpusEncoder"
        private const val MIME_TYPE = "audio/opus"
        private const val TIMEOUT_US = 10_000L // 10 ms poll timeout
        private const val SAMPLES_PER_FRAME = 480 // 10 ms at 48 kHz
    }

    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var oggFallbackWriter: OggOpusWriter? = null
    private var audioTrackIndex = -1
    private var isMuxerStarted = false

    private var presentationTimeUs = 0L
    private val bufferInfo = MediaCodec.BufferInfo()
    private var isReleased = false

    init {
        initEncoder()
    }

    private fun initEncoder() {
        val format = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC) // unused for opus
            setInteger(MediaFormat.KEY_COMPLEXITY, 5)
        }

        val mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()
        codec = mediaCodec

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
        } else {
            oggFallbackWriter = OggOpusWriter(outputFile, sampleRate, channelCount)
        }
    }

    /**
     * Feeds 16-bit PCM audio samples into the Opus encoder.
     * Samples are queued in 480-sample chunks (10 ms) with accurate presentation timestamps.
     */
    fun encodePcm(pcmShorts: ShortArray, offset: Int = 0, length: Int = pcmShorts.size) {
        val activeCodec = codec ?: return
        if (isReleased) return

        var remaining = length
        var currentOffset = offset

        while (remaining > 0) {
            val chunkCount = minOf(remaining, SAMPLES_PER_FRAME)
            val inputBufferIndex = activeCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = activeCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until chunkCount) {
                        inputBuffer.putShort(pcmShorts[currentOffset + i])
                    }

                    val bytesQueued = chunkCount * 2
                    val frameDurationUs = (chunkCount * 1_000_000L) / sampleRate
                    activeCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        bytesQueued,
                        presentationTimeUs,
                        0
                    )
                    presentationTimeUs += frameDurationUs
                }
                currentOffset += chunkCount
                remaining -= chunkCount
            }

            drainEncoder(endOfStream = false)
        }
    }

    /**
     * Drains pending encoded Opus packets from MediaCodec and writes them to the muxer.
     */
    private fun drainEncoder(endOfStream: Boolean) {
        val activeCodec = codec ?: return
        if (isReleased) return

        while (true) {
            val outputBufferIndex = activeCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (isMuxerStarted) {
                        Log.w(TAG, "Output format changed twice, ignoring")
                    } else {
                        val newFormat = activeCodec.outputFormat
                        muxer?.let { m ->
                            audioTrackIndex = m.addTrack(newFormat)
                            m.start()
                            isMuxerStarted = true
                        }
                        oggFallbackWriter?.initHeaders(newFormat)
                    }
                }
                outputBufferIndex >= 0 -> {
                    val encodedBuffer = activeCodec.getOutputBuffer(outputBufferIndex)
                    if (encodedBuffer != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // Codec configuration data consumed by MediaMuxer via format
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            if (isMuxerStarted && muxer != null && audioTrackIndex >= 0) {
                                muxer?.writeSampleData(audioTrackIndex, encodedBuffer, bufferInfo)
                            } else if (oggFallbackWriter != null) {
                                oggFallbackWriter?.writePacket(encodedBuffer, bufferInfo)
                            }
                        }

                        activeCodec.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * Signals End-of-Stream to MediaCodec, drains all remaining packets, and finalizes the file.
     */
    fun stopAndRelease() {
        if (isReleased) return
        isReleased = true

        val activeCodec = codec
        if (activeCodec != null) {
            try {
                // Queue EOS
                val inputBufferIndex = activeCodec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    activeCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
                drainEncoder(endOfStream = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error signaling EOS to MediaCodec", e)
            }

            try {
                activeCodec.stop()
            } catch (_: Exception) {}
            try {
                activeCodec.release()
            } catch (_: Exception) {}
            codec = null
        }

        muxer?.let { m ->
            try {
                if (isMuxerStarted) {
                    m.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaMuxer", e)
            }
            try {
                m.release()
            } catch (_: Exception) {}
            muxer = null
            isMuxerStarted = false
        }

        oggFallbackWriter?.let { writer ->
            try {
                writer.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing Ogg fallback writer", e)
            }
            oggFallbackWriter = null
        }
    }

    /**
     * Standalone Ogg container writer for API < 29 fallback.
     * Compliant with RFC 7845 (Ogg Opus) and RFC 3533 (Ogg bitstream).
     */
    private class OggOpusWriter(
        outputFile: File,
        private val sampleRate: Int,
        private val channels: Int
    ) {
        private val fos = FileOutputStream(outputFile)
        private val serialNo = (System.currentTimeMillis() and 0xFFFFFFF).toInt()
        private var sequenceNo = 0
        private var granulePosition = 0L

        fun initHeaders(format: MediaFormat) {
            // Page 0: OpusHead
            val opusHead = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("OpusHead".toByteArray(Charsets.US_ASCII))
                put(1) // version
                put(channels.toByte())
                putShort(384.toShort()) // pre-skip
                putInt(sampleRate)
                putShort(0) // output gain
                put(0) // channel mapping family
            }.array()

            writePage(opusHead, granulePos = 0L, headerType = 0x02) // 0x02 = BOS (Beginning of Stream)

            // Page 1: OpusTags
            val vendorString = "Naughty Offline Audio".toByteArray(Charsets.UTF_8)
            val opusTags = ByteBuffer.allocate(8 + 4 + vendorString.size + 4).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("OpusTags".toByteArray(Charsets.US_ASCII))
                putInt(vendorString.size)
                put(vendorString)
                putInt(0) // user comment list length = 0
            }.array()

            writePage(opusTags, granulePos = 0L, headerType = 0x00)
        }

        fun writePacket(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            val packetBytes = ByteArray(info.size)
            buffer.get(packetBytes)

            // 10 ms at 48 kHz is 480 samples
            granulePosition += SAMPLES_PER_FRAME
            val isEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
            val headerType = if (isEos) 0x04 else 0x00 // 0x04 = EOS

            writePage(packetBytes, granulePos = granulePosition, headerType = headerType)
        }

        fun close() {
            try {
                fos.flush()
                fos.close()
            } catch (_: Exception) {}
        }

        private fun writePage(data: ByteArray, granulePos: Long, headerType: Int) {
            val pageBuffer = ByteBuffer.allocate(27 + 1 + data.size).order(ByteOrder.LITTLE_ENDIAN)
            pageBuffer.put("OggS".toByteArray(Charsets.US_ASCII))
            pageBuffer.put(0.toByte()) // stream structure version
            pageBuffer.put(headerType.toByte())
            pageBuffer.putLong(granulePos)
            pageBuffer.putInt(serialNo)
            pageBuffer.putInt(sequenceNo++)
            pageBuffer.putInt(0) // CRC placeholder at offset 22
            pageBuffer.put(1.toByte()) // 1 segment
            pageBuffer.put(data.size.coerceAtMost(255).toByte())
            pageBuffer.put(data)

            val rawPage = pageBuffer.array()
            // Compute CRC
            val crc = computeOggCrc(rawPage)
            pageBuffer.putInt(22, crc)

            fos.write(rawPage)
        }

        private fun computeOggCrc(bytes: ByteArray): Int {
            var crc = 0
            for (b in bytes) {
                val byteVal = (b.toInt() and 0xFF)
                crc = (crc shl 8) xor CRC_LOOKUP[((crc ushr 24) xor byteVal) and 0xFF]
            }
            return crc
        }

        companion object {
            private val CRC_LOOKUP = IntArray(256).apply {
                for (i in 0 until 256) {
                    var r = i shl 24
                    for (j in 0 until 8) {
                        r = if ((r and -0x80000000) != 0) (r shl 1) xor 0x04C11DB7 else (r shl 1)
                    }
                    this[i] = r
                }
            }
        }
    }
}
