package com.example.naughty.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ImageTextExtractor {

    fun createRecognizer(): TextRecognizer {
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from an image URI using Google ML Kit's local on-device
     * Latin text recognition model.
     *
     * @param context Application/Activity context used to resolve the URI.
     * @param imageUri Image content/file URI to extract text from.
     * @return Result containing recognized text or an exception.
     */
    suspend fun extractText(context: Context, imageUri: Uri): Result<String> =
        suspendCancellableCoroutine { continuation ->
            var recognizer: TextRecognizer? = null
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                recognizer = createRecognizer()
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(Result.success(visionText.text))
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error))
                        }
                    }
                    .addOnCompleteListener {
                        try {
                            recognizer.close()
                        } catch (_: Exception) {}
                    }
            } catch (e: Exception) {
                try {
                    recognizer?.close()
                } catch (_: Exception) {}
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Extracts text from an in-memory [Bitmap] using Google ML Kit's local on-device
     * Latin text recognition model. If [sharedRecognizer] is provided, it will be reused
     * without closing it after this frame.
     */
    suspend fun extractTextFromBitmap(
        bitmap: Bitmap,
        sharedRecognizer: TextRecognizer? = null,
        rotationDegrees: Int = 0
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val ownsRecognizer = sharedRecognizer == null
        var recognizer: TextRecognizer? = sharedRecognizer
        try {
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            if (recognizer == null) {
                recognizer = createRecognizer()
            }
            val activeRecognizer = recognizer
            activeRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(Result.success(visionText.text))
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                }
                .addOnCompleteListener {
                    if (ownsRecognizer) {
                        try {
                            activeRecognizer.close()
                        } catch (_: Exception) {}
                    }
                }
        } catch (e: Exception) {
            if (ownsRecognizer) {
                try {
                    recognizer?.close()
                } catch (_: Exception) {}
            }
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Extracts structured [OcrLine] entries with bounding box geometry, ML Kit confidence,
     * rotation angle, and text-block index from an in-memory [Bitmap].
     */
    suspend fun extractLinesFromBitmap(
        bitmap: Bitmap,
        sharedRecognizer: TextRecognizer? = null,
        rotationDegrees: Int = 0
    ): Result<List<OcrLine>> = suspendCancellableCoroutine { continuation ->
        val ownsRecognizer = sharedRecognizer == null
        var recognizer: TextRecognizer? = sharedRecognizer
        try {
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            if (recognizer == null) {
                recognizer = createRecognizer()
            }
            val activeRecognizer = recognizer
            activeRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        val lines = ArrayList<OcrLine>()
                        visionText.textBlocks.forEachIndexed { blockIdx, block ->
                            for (line in block.lines) {
                                val box = line.boundingBox
                                val conf = line.confidence.takeIf { it.isFinite() && it >= 0f } ?: 1f
                                val ang = line.angle.takeIf { it.isFinite() } ?: 0f
                                lines.add(
                                    OcrLine(
                                        text = line.text,
                                        left = box?.left ?: 0,
                                        top = box?.top ?: 0,
                                        right = box?.right ?: 0,
                                        bottom = box?.bottom ?: 0,
                                        confidence = conf,
                                        angle = ang,
                                        blockIndex = blockIdx
                                    )
                                )
                            }
                        }
                        continuation.resume(Result.success(lines))
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                }
                .addOnCompleteListener {
                    if (ownsRecognizer) {
                        try {
                            activeRecognizer.close()
                        } catch (_: Exception) {}
                    }
                }
        } catch (e: Exception) {
            if (ownsRecognizer) {
                try {
                    recognizer?.close()
                } catch (_: Exception) {}
            }
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}
