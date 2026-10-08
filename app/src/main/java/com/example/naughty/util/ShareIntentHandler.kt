package com.example.naughty.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.UUID

data class ParsedSharedNote(
    val title: String,
    val content: String,
    val cardType: String = "modular"
)

enum class ImageShareMode {
    IMAGE_ONLY,
    OCR_ONLY,
    BOTH
}

data class PendingImageShare(
    val uris: List<Uri>,
    val subject: String?,
    val text: String?
)

object ShareIntentHandler {

    private const val EXTRA_HANDLED_SHARE = "com.example.naughty.HANDLED_SHARE"

    fun isShareAction(action: String?): Boolean {
        if (action == null) return false
        return action == Intent.ACTION_SEND ||
                action == Intent.ACTION_SEND_MULTIPLE ||
                action == Intent.ACTION_PROCESS_TEXT
    }

    fun isShareIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        return isShareAction(intent.action)
    }

    fun isHandled(intent: Intent?): Boolean {
        return intent?.getBooleanExtra(EXTRA_HANDLED_SHARE, false) == true
    }

    fun markAsHandled(intent: Intent?) {
        intent?.putExtra(EXTRA_HANDLED_SHARE, true)
    }

    fun isUrl(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)) &&
                !trimmed.contains(" ") &&
                !trimmed.contains("\n")
    }

    fun extractTitleFromUrl(url: String): String {
        return try {
            val trimmed = url.trim()
            val uri = try {
                URI(trimmed)
            } catch (_: Exception) {
                null
            }

            val rawHost = uri?.host ?: run {
                val match = Regex("""https?://([^/?#]+)""").find(trimmed)
                match?.groupValues?.get(1)
            } ?: ""

            val host = rawHost.removePrefix("www.")
            val rawPath = uri?.path ?: run {
                val match = Regex("""https?://[^/?#]+(/[^?#]*)""").find(trimmed)
                match?.groupValues?.get(1) ?: ""
            }

            val segments = rawPath.split("/")
                .filter { it.isNotBlank() }
                .map { segment ->
                    segment.removeSuffix(".html")
                        .removeSuffix(".htm")
                        .removeSuffix(".php")
                        .replace(Regex("[_\\-]+"), " ")
                        .trim()
                }
                .filter { it.isNotBlank() }

            val lastSegment = segments.lastOrNull()

            when {
                host.isNotBlank() && !lastSegment.isNullOrBlank() && !lastSegment.all { it.isDigit() } -> {
                    "$host • $lastSegment"
                }
                host.isNotBlank() -> host
                else -> "Shared Link"
            }
        } catch (_: Exception) {
            "Shared Link"
        }
    }

    fun cleanTitleFromText(text: String): String {
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: return "Shared Note"

        if (isUrl(firstLine)) {
            return extractTitleFromUrl(firstLine)
        }

        // Clean leading markdown headers, bullets, checklist checkboxes, or blockquotes
        val cleaned = firstLine
            .replace(Regex("""^#{1,6}\s+"""), "")
            .replace(Regex("""^[-*+]\s+\[[ xX]\]\s+"""), "")
            .replace(Regex("""^[-*+]\s+"""), "")
            .replace(Regex("""^>\s+"""), "")
            .replace(Regex("""^\[[ xX]\]\s+"""), "")
            .trim()

        val finalTitle = cleaned.ifBlank { "Shared Note" }
        return if (finalTitle.length > 60) {
            finalTitle.take(57).trimEnd() + "..."
        } else {
            finalTitle
        }
    }

    fun detectCardType(content: String): String {
        val trimmed = content.trim()
        return if (trimmed.contains("- [ ]") || trimmed.contains("- [x]") || trimmed.contains("- [X]")) {
            "checklist"
        } else {
            "modular"
        }
    }

    fun parseTextContent(subject: String?, text: String?): ParsedSharedNote? {
        val cleanSubject = subject?.trim().orEmpty()
        val cleanText = text?.trim().orEmpty()

        if (cleanSubject.isBlank() && cleanText.isBlank()) {
            return null
        }

        val title = when {
            cleanSubject.isNotBlank() -> cleanSubject
            else -> cleanTitleFromText(cleanText)
        }

        val content = when {
            cleanText.isNotBlank() -> cleanText
            else -> cleanSubject
        }

        val cardType = detectCardType(content)
        return ParsedSharedNote(title = title, content = content, cardType = cardType)
    }

    fun formatNoteWithImages(
        subject: String?,
        text: String?,
        localImageUris: List<String>
    ): ParsedSharedNote? {
        if (localImageUris.isEmpty()) {
            return parseTextContent(subject, text)
        }

        val imageMarkdown = localImageUris.joinToString("\n\n") { "![Image]($it)" }
        val cleanText = text?.trim().orEmpty()
        val cleanSubject = subject?.trim().orEmpty()

        val content = if (cleanText.isNotBlank()) {
            "$cleanText\n\n$imageMarkdown"
        } else {
            imageMarkdown
        }

        val title = when {
            cleanSubject.isNotBlank() -> cleanSubject
            cleanText.isNotBlank() -> cleanTitleFromText(cleanText)
            localImageUris.size == 1 -> "Photo note"
            else -> "${localImageUris.size} Photos"
        }

        return ParsedSharedNote(title = title, content = content, cardType = "modular")
    }

    fun combineOcrResult(
        subject: String?,
        accompanyingText: String?,
        ocrText: String?,
        savedImageFileUris: List<String>,
        mode: ImageShareMode
    ): ParsedSharedNote? {
        val cleanSubject = subject?.trim().orEmpty()
        val cleanAccompanying = accompanyingText?.trim().orEmpty()
        val cleanOcr = ocrText?.trim().orEmpty()

        return when (mode) {
            ImageShareMode.IMAGE_ONLY -> {
                formatNoteWithImages(cleanSubject, cleanAccompanying, savedImageFileUris)
            }
            ImageShareMode.OCR_ONLY -> {
                if (cleanOcr.isNotBlank()) {
                    val fullContent = if (cleanAccompanying.isNotBlank()) {
                        "$cleanAccompanying\n\n$cleanOcr"
                    } else {
                        cleanOcr
                    }
                    val title = if (cleanSubject.isNotBlank()) {
                        cleanSubject
                    } else {
                        cleanTitleFromText(fullContent)
                    }
                    val cardType = detectCardType(fullContent)
                    ParsedSharedNote(title = title, content = fullContent, cardType = cardType)
                } else {
                    // Fallback if no text detected: preserve image or accompanying text
                    formatNoteWithImages(cleanSubject, cleanAccompanying, savedImageFileUris)
                        ?: parseTextContent(cleanSubject, cleanAccompanying)
                }
            }
            ImageShareMode.BOTH -> {
                val textPart = when {
                    cleanOcr.isNotBlank() && cleanAccompanying.isNotBlank() -> "$cleanAccompanying\n\n$cleanOcr"
                    cleanOcr.isNotBlank() -> cleanOcr
                    cleanAccompanying.isNotBlank() -> cleanAccompanying
                    else -> ""
                }
                val imageMarkdown = savedImageFileUris.joinToString("\n\n") { "![Image]($it)" }
                val fullContent = when {
                    textPart.isNotBlank() && imageMarkdown.isNotBlank() -> "$textPart\n\n$imageMarkdown"
                    imageMarkdown.isNotBlank() -> imageMarkdown
                    else -> textPart
                }
                if (fullContent.isBlank()) return null
                val title = if (cleanSubject.isNotBlank()) {
                    cleanSubject
                } else if (textPart.isNotBlank()) {
                    cleanTitleFromText(textPart)
                } else if (savedImageFileUris.size == 1) {
                    "Photo note"
                } else {
                    "${savedImageFileUris.size} Photos"
                }
                val cardType = detectCardType(fullContent)
                ParsedSharedNote(title = title, content = fullContent, cardType = cardType)
            }
        }
    }

    fun extractImageUris(intent: Intent): List<Uri> {
        val uris = mutableListOf<Uri>()

        // 1. Check EXTRA_STREAM based on action
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            try {
                val streamList = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (streamList != null) {
                    uris.addAll(streamList.filterNotNull())
                }
            } catch (_: Exception) {}
        } else {
            try {
                val singleStream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (singleStream != null) {
                    uris.add(singleStream)
                }
            } catch (_: Exception) {}
        }

        // 3. Check ClipData
        val clipData = intent.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val itemUri = clipData.getItemAt(i)?.uri
                if (itemUri != null && itemUri !in uris) {
                    uris.add(itemUri)
                }
            }
        }

        // 4. Check intent.data
        val dataUri = intent.data
        if (dataUri != null && dataUri !in uris) {
            uris.add(dataUri)
        }

        return uris.distinct()
    }

    fun extractImageShare(intent: Intent): PendingImageShare? {
        val action = intent.action ?: return null
        val type = intent.type
        val isImageAction = action == Intent.ACTION_SEND_MULTIPLE ||
                (type?.startsWith("image/") == true) ||
                intent.hasExtra(Intent.EXTRA_STREAM)

        if (!isImageAction) return null

        val uris = extractImageUris(intent)
        if (uris.isEmpty()) return null

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

        return PendingImageShare(uris = uris, subject = subject, text = text)
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): File? {
        return try {
            val imagesDir = File(context.filesDir, "note_images").apply { mkdirs() }
            val mimeType = try {
                context.contentResolver.getType(uri)
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

    suspend fun processImageShare(
        context: Context,
        share: PendingImageShare,
        mode: ImageShareMode,
        repository: NoteRepository
    ): NoteMetadata? = withContext(Dispatchers.IO) {
        val savedImageFileUris = if (mode == ImageShareMode.IMAGE_ONLY || mode == ImageShareMode.BOTH) {
            share.uris.mapNotNull { uri ->
                copyUriToInternalStorage(context, uri)?.let { "file://${it.absolutePath}" }
            }
        } else emptyList()

        val ocrText = if (mode == ImageShareMode.OCR_ONLY || mode == ImageShareMode.BOTH) {
            val texts = mutableListOf<String>()
            for (uri in share.uris) {
                val res = ImageTextExtractor.extractText(context, uri)
                res.getOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            }
            texts.joinToString("\n\n")
        } else null

        // In OCR_ONLY mode, if no text was detected, fall back to copying image to storage so no data is lost
        val fallbackImages = if (mode == ImageShareMode.OCR_ONLY && (ocrText.isNullOrBlank())) {
            share.uris.mapNotNull { uri ->
                copyUriToInternalStorage(context, uri)?.let { "file://${it.absolutePath}" }
            }
        } else savedImageFileUris

        val parsedNote = combineOcrResult(
            subject = share.subject,
            accompanyingText = share.text,
            ocrText = ocrText,
            savedImageFileUris = fallbackImages,
            mode = mode
        ) ?: return@withContext null

        repository.createNote(
            title = parsedNote.title,
            content = parsedNote.content,
            cardType = parsedNote.cardType
        )
    }

    suspend fun processShareIntent(
        context: Context,
        intent: Intent,
        repository: NoteRepository
    ): NoteMetadata? {
        if (!isShareIntent(intent) || isHandled(intent)) return null
        markAsHandled(intent)

        return withContext(Dispatchers.IO) {
            val action = intent.action ?: return@withContext null

            val imageShare = extractImageShare(intent)
            if (imageShare != null) {
                return@withContext processImageShare(
                    context = context,
                    share = imageShare,
                    mode = ImageShareMode.IMAGE_ONLY,
                    repository = repository
                )
            }

            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
                ?: intent.getStringExtra(Intent.EXTRA_TITLE)

            val text = when (action) {
                Intent.ACTION_PROCESS_TEXT -> {
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                        ?: intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
                        ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                }
                else -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                        ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                }
            }

            val parsedNote = parseTextContent(subject, text) ?: return@withContext null

            repository.createNote(
                title = parsedNote.title,
                content = parsedNote.content,
                cardType = parsedNote.cardType
            )
        }
    }
}
