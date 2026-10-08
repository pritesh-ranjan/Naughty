package com.example.naughty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.naughty.util.ImageTextExtractor
import com.example.naughty.util.ShareIntentHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Headless activity for the grouped share target:
 * "Copy text in image to clipboard".
 *
 * Runs OCR directly on the incoming shared image(s), copies any recognized
 * text to the system clipboard, displays a feedback Toast, and terminates
 * immediately without opening the main note editing interface.
 */
class ShareCopyTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUris = ShareIntentHandler.extractImageUris(intent)
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No image found to extract text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "Extracting text...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val texts = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                for (uri in imageUris) {
                    try {
                        val result = ImageTextExtractor.extractText(this@ShareCopyTextActivity, uri)
                        val text = result.getOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            texts.add(text)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            val combined = texts.joinToString("\n\n").trim()
            if (combined.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Extracted Text", combined)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(this@ShareCopyTextActivity, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ShareCopyTextActivity, "No text detected in image", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
