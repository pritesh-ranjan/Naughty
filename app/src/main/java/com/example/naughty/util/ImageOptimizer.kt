package com.example.naughty.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageOptimizer {
    const val MAX_IMAGES_PER_NOTE = 10
    private const val MAX_DIMENSION = 1600
    private const val COMPRESSION_QUALITY = 85

    internal fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        maxDimension: Int = MAX_DIMENSION
    ): Int {
        var sampleSize = 1
        var maxDim = maxOf(rawWidth, rawHeight)
        while (maxDim > maxDimension * 1.5) {
            sampleSize *= 2
            maxDim /= 2
        }
        return sampleSize
    }

    /**
     * Reads an image stream from [sourceUri], calculates [BitmapFactory.Options.inSampleSize]
     * to avoid memory spikes, downsamples to fit within [MAX_DIMENSION] x [MAX_DIMENSION],
     * and saves an optimized JPEG in [context.filesDir]/note_images.
     *
     * Runs completely off the main thread on [Dispatchers.IO].
     */
    suspend fun optimizeAndSave(context: Context, sourceUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Read bounds only (zero bitmap allocation); decodeStream returns null by design here
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val boundsStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext null
            boundsStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

            // 2. Calculate optimal inSampleSize as a power of 2
            val sampleSize = calculateInSampleSize(rawWidth, rawHeight, MAX_DIMENSION)

            // 3. Decode sampled bitmap into ARGB_8888 or RGB_565
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 4. Scale down precisely if sampled dimensions still exceed MAX_DIMENSION
            val finalBitmap = if (sampledBitmap.width > MAX_DIMENSION || sampledBitmap.height > MAX_DIMENSION) {
                val ratio = minOf(
                    MAX_DIMENSION.toFloat() / sampledBitmap.width,
                    MAX_DIMENSION.toFloat() / sampledBitmap.height
                )
                val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                    if (it != sampledBitmap) {
                        sampledBitmap.recycle()
                    }
                }
            } else {
                sampledBitmap
            }

            // 5. Compress and persist into app-managed note_images directory
            val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(imagesDir, fileName)

            FileOutputStream(targetFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outStream)
            }
            finalBitmap.recycle()

            targetFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Optimizes and saves [sourceUri], falling back to a direct stream copy if decoding fails
     * (e.g., for uncommon formats or animated GIFs/WebPs).
     */
    suspend fun saveImageWithFallback(context: Context, sourceUri: Uri): File? = withContext(Dispatchers.IO) {
        val optimized = optimizeAndSave(context, sourceUri)
        if (optimized != null) return@withContext optimized

        try {
            val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
            val mimeType = try {
                context.contentResolver.getType(sourceUri)
            } catch (_: Exception) {
                null
            }
            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext"
            val destFile = File(imagesDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            destFile
        } catch (_: Exception) {
            null
        }
    }
}
