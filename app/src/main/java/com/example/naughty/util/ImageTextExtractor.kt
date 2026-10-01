package com.example.naughty.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ImageTextExtractor {

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
            var recognizer: com.google.mlkit.vision.text.TextRecognizer? = null
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
}
