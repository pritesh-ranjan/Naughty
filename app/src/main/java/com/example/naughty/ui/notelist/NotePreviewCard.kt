package com.example.naughty.ui.notelist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.editor.EditorBlockParser
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.editor.MentionParser
import com.example.naughty.ui.oneui.components.OneUiCard
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.theme.NoteColorPalette
import com.example.naughty.util.AudioRecorderHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NotePreviewChecklistItem(
    val isChecked: Boolean,
    val text: String,
    val reminderText: String? = null
)

data class NotePreview(
    val title: String,
    val isTodo: Boolean,
    val todoDoneCount: Int,
    val todoTotalCount: Int,
    val checklistItems: List<NotePreviewChecklistItem>,
    val remainingChecklistCount: Int,
    val textSnippet: String,
    val imageRef: String?,
    val voiceNoteRef: String?,
    val hasActiveReminder: Boolean,
    val formattedDate: String
) {
    companion object {
        fun from(note: NoteMetadata, rawContent: String): NotePreview {
            val titleClean = if (
                note.title.isBlank() ||
                note.title.equals("New thought", ignoreCase = true) ||
                note.title.equals("Untitled", ignoreCase = true)
            ) "" else note.title

            val isChecklistType = note.cardType == "todo" ||
                    note.cardType == "checklist" ||
                    rawContent.contains("- [ ]") ||
                    rawContent.contains("- [x]", ignoreCase = true)

            val checklistLines = if (isChecklistType && !note.isLocked) {
                rawContent.lines().filter {
                    it.trim().startsWith("- [") && it.substringAfter("] ").isNotBlank()
                }
            } else emptyList()

            val totalTasks = checklistLines.size
            val doneTasks = checklistLines.count { it.contains("- [x]", ignoreCase = true) }

            val items = checklistLines.take(4).map { line ->
                val checked = line.contains("- [x]", ignoreCase = true)
                val rawText = line.substringAfter("] ").trim()
                val reminder = InlineReminderParser.extractFirstReminder(rawText)
                val clean = InlineReminderParser.removeReminderFromLine(rawText)
                NotePreviewChecklistItem(
                    isChecked = checked,
                    text = MentionParser.cleanMentions(clean),
                    reminderText = reminder?.displayText
                )
            }

            val remaining = if (totalTasks > 4) totalTasks - 4 else 0

            val textSnippet = if (!isChecklistType && !note.isLocked) {
                val cleaned = InlineReminderParser.cleanAllReminders(rawContent)
                    .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
                    .replace(Regex("\\[🎤\\s*Voice Note\\]\\(.*?\\)", RegexOption.IGNORE_CASE), "")
                val withoutMentions = MentionParser.cleanMentions(cleaned)
                withoutMentions
                    .replace(Regex("[#*_~`>\\[\\]()]"), "")
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(3)
                    .joinToString("\n")
            } else ""

            val imageRef = if (!note.isLocked) {
                Regex("!\\[.*?\\]\\((.*?)\\)").find(rawContent)?.groupValues?.getOrNull(1)
            } else null

            val voiceNote = if (!note.isLocked) {
                EditorBlockParser.extractVoiceNotes(rawContent).firstOrNull()
            } else null

            val hasReminder = (note.reminderTime != null && note.reminderTime > System.currentTimeMillis()) ||
                    rawContent.contains("](reminder:")

            return NotePreview(
                title = titleClean,
                isTodo = isChecklistType,
                todoDoneCount = doneTasks,
                todoTotalCount = totalTasks,
                checklistItems = items,
                remainingChecklistCount = remaining,
                textSnippet = textSnippet,
                imageRef = imageRef,
                voiceNoteRef = voiceNote,
                hasActiveReminder = hasReminder,
                formattedDate = formatNoteDate(note.modifiedAt)
            )
        }

        private fun formatNoteDate(timestamp: Long): String {
            val noteCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val nowCal = Calendar.getInstance()

            val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
            val timeStr = timeFormat.format(Date(timestamp))

            val isToday = nowCal.get(Calendar.YEAR) == noteCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == noteCal.get(Calendar.DAY_OF_YEAR)

            val isYesterday = nowCal.get(Calendar.YEAR) == noteCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) - noteCal.get(Calendar.DAY_OF_YEAR) == 1

            return when {
                isToday -> "Today, $timeStr"
                isYesterday -> "Yesterday, $timeStr"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }
}

@Composable
fun NotePreviewCard(
    note: NoteMetadata,
    rawContent: String,
    boundApps: List<String> = emptyList(),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val preview = remember(note.id, note.modifiedAt, rawContent) {
        NotePreview.from(note, rawContent)
    }

    OneUiCard(
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = OneUi.colors.surface,
        modifier = modifier
    ) {
        // Card Header: Title + Task badge + Indicators (Pinned, Locked)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (note.isLocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OneUi.colors.warning.copy(alpha = 0.16f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Locked",
                                tint = OneUi.colors.warning,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else if (preview.isTodo && preview.todoTotalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OneUi.colors.accent.copy(alpha = 0.16f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "TODO ${preview.todoDoneCount}/${preview.todoTotalCount}",
                            style = OneUi.type.caption,
                            fontWeight = FontWeight.Bold,
                            color = OneUi.colors.accent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (preview.title.isNotBlank()) {
                    Text(
                        text = preview.title,
                        style = OneUi.type.cardTitle,
                        color = OneUi.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = OneUi.colors.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Preview Content
        if (note.isLocked) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Locked note • Tap to unlock",
                style = OneUi.type.caption,
                color = OneUi.colors.textTertiary,
                fontSize = 13.5.sp
            )
        } else if (preview.checklistItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                preview.checklistItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (item.isChecked) OneUi.colors.accent else OneUi.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.text,
                            style = OneUi.type.cardBody,
                            color = if (item.isChecked) OneUi.colors.textSecondary else OneUi.colors.textPrimary,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.reminderText != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔔", fontSize = 10.sp)
                        }
                    }
                }
                if (preview.remainingChecklistCount > 0) {
                    Text(
                        text = "+${preview.remainingChecklistCount} more tasks",
                        style = OneUi.type.caption,
                        color = OneUi.colors.textTertiary,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                    )
                }
            }
        } else if (preview.textSnippet.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preview.textSnippet,
                style = OneUi.type.cardBody,
                color = OneUi.colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Attached image preview
        if (!preview.imageRef.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            AttachedImageThumbnail(
                imageRef = preview.imageRef,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(OneUi.shapes.inner)
            )
        }

        // Voice note preview
        if (!preview.voiceNoteRef.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            VoiceNoteCardBadge(
                audioUriString = preview.voiceNoteRef,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Footer: Bound Apps & Date
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bound apps tags
            if (boundApps.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    boundApps.forEach { appName ->
                        Surface(
                            shape = OneUi.shapes.pill,
                            color = OneUi.colors.accent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = OneUi.colors.accent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = appName,
                                    style = OneUi.type.caption,
                                    color = OneUi.colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Date & indicator icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (preview.voiceNoteRef != null) {
                    Text("🎙️", fontSize = 11.sp)
                }
                if (preview.hasActiveReminder) {
                    Text("🔔", fontSize = 11.sp)
                }
                Text(
                    text = preview.formattedDate,
                    style = OneUi.type.caption,
                    color = OneUi.colors.textTertiary,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
fun AttachedImageThumbnail(
    imageRef: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageRef)
            .crossfade(150)
            .build(),
        contentDescription = "Attached image",
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
fun VoiceNoteCardBadge(
    audioUriString: String,
    palette: NoteColorPalette? = null,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanPath = remember(audioUriString) {
        if (audioUriString.startsWith("file://")) audioUriString.removePrefix("file://") else audioUriString
    }
    val audioFile = remember(cleanPath) { File(cleanPath) }
    val amplitudes = remember(cleanPath) {
        AudioRecorderHelper.readWaveform(audioFile, targetCount = 18)
    }
    val durationText = remember(cleanPath) {
        AudioRecorderHelper.getAudioDuration(audioFile)
    }

    val accent = palette?.iconTint ?: OneUi.colors.accent

    Surface(
        shape = OneUi.shapes.inner,
        color = accent.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.2f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice Note",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
            ) {
                val barCount = amplitudes.size
                if (barCount > 0) {
                    val spacing = 2.dp.toPx()
                    val totalSpacing = (barCount - 1) * spacing
                    val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(1.5.dp.toPx())
                    val maxHeight = size.height

                    amplitudes.forEachIndexed { i, amp ->
                        val x = i * (barWidth + spacing)
                        val scaledAmp = kotlin.math.sqrt(amp.coerceIn(0.08f, 1.0f))
                        val barHeight = (scaledAmp * maxHeight).coerceIn(3.dp.toPx(), maxHeight)
                        val y = (maxHeight - barHeight) / 2f

                        drawRoundRect(
                            color = accent.copy(alpha = 0.85f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                }
            }

            Text(
                text = durationText ?: "Voice Note",
                style = OneUi.type.caption,
                color = accent,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
