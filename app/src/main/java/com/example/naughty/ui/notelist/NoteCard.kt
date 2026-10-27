package com.example.naughty.ui.notelist

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.editor.EditorBlockParser
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.editor.MentionActionHandler
import com.example.naughty.ui.editor.MentionChip
import com.example.naughty.ui.editor.MentionParser
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.StealthTextMuted
import com.example.naughty.ui.theme.getNoteColorPalette
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.AudioRecorderHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteMetadata,
    rawContent: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    boundApps: List<String> = emptyList(),
    onContentChange: ((String) -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val palette = getNoteColorPalette(note.colorTheme, isDark)
    val formattedTime = remember(note.modifiedAt) { formatNoteTimestamp(note.modifiedAt) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            if (note.isLocked) {
                LockedCardContent(
                    note = note,
                    palette = palette,
                    isDark = isDark,
                    formattedTime = formattedTime,
                    boundApps = boundApps,
                    onTogglePin = onTogglePin
                )
            } else {
                val isChecklist = note.cardType == "todo" || note.cardType == "checklist" || isChecklistContent(rawContent)

                if (isChecklist) {
                    ChecklistCardContent(
                        note = note,
                        rawContent = rawContent,
                        palette = palette,
                        isDark = isDark,
                        formattedTime = formattedTime,
                        boundApps = boundApps,
                        onContentChange = onContentChange,
                        onTogglePin = onTogglePin,
                        onImageClick = onImageClick
                    )
                } else {
                    MarkdownNoteCardContent(
                        note = note,
                        rawContent = rawContent,
                        palette = palette,
                        isDark = isDark,
                        formattedTime = formattedTime,
                        boundApps = boundApps,
                        onTogglePin = onTogglePin,
                        onImageClick = onImageClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BoundAppsRow(
    boundApps: List<String>,
    palette: com.example.naughty.ui.theme.NoteColorPalette,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (boundApps.isEmpty()) {
        Spacer(modifier = Modifier.width(1.dp))
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        boundApps.forEach { appName ->
            val pillColor = if (isDark) ElectricCyan.copy(alpha = 0.14f) else ElectricCyan.copy(alpha = 0.1f)
            val textColor = if (isDark) ElectricCyan else Color(0xFF007799)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = pillColor
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Bound App",
                        tint = textColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------------- Locked Note Card ----------------
@Composable
private fun LockedCardContent(
    note: NoteMetadata,
    palette: com.example.naughty.ui.theme.NoteColorPalette,
    isDark: Boolean,
    formattedTime: String,
    boundApps: List<String> = emptyList(),
    onTogglePin: (() -> Unit)?
) {
    val displayTitle = if (note.title.equals("New thought", ignoreCase = true) || note.title.equals("Untitled", ignoreCase = true)) "Untitled" else note.title

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF241C0A) else Color(0xFFFEF3C7),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = ElectricAmber,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.5.sp,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { onTogglePin?.invoke() },
            modifier = Modifier.size(24.dp)
        ) {
            val pinTint = if (note.isPinned) {
                ElectricGreen
            } else {
                if (isDark) Color(0xFF3F3F46) else palette.textSecondary.copy(alpha = 0.35f)
            }
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pin",
                tint = pinTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // Preview is DISABLED: Only title is visible.
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "Locked note • Tap to unlock",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) StealthTextMuted else palette.textSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoundAppsRow(boundApps = boundApps, palette = palette, isDark = isDark)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val hasActiveReminder = (note.reminderTime != null && note.reminderTime > System.currentTimeMillis())
            if (hasActiveReminder) {
                Text("🔔", fontSize = 11.sp)
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) StealthTextMuted else palette.textSecondary.copy(alpha = 0.8f),
                fontSize = 11.5.sp
            )
        }
    }
}

// ---------------- Checklist Card ----------------
@Composable
private fun ChecklistCardContent(
    note: NoteMetadata,
    rawContent: String,
    palette: com.example.naughty.ui.theme.NoteColorPalette,
    isDark: Boolean,
    formattedTime: String,
    boundApps: List<String> = emptyList(),
    onContentChange: ((String) -> Unit)?,
    onTogglePin: (() -> Unit)?,
    onImageClick: ((String) -> Unit)? = null
) {
    val displayTitle = if (note.title.equals("New thought", ignoreCase = true) || note.title.equals("Untitled", ignoreCase = true)) "" else note.title
    val isTodo = note.cardType == "todo"
    val totalCount = remember(rawContent) {
        rawContent.lines().count { it.trim().startsWith("- [") && it.substringAfter("] ").isNotBlank() }
    }
    val doneCount = remember(rawContent) {
        rawContent.lines().count { it.trim().startsWith("- [x]", ignoreCase = true) && it.substringAfter("] ").isNotBlank() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isTodo) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) ElectricGreen.copy(alpha = 0.16f) else ElectricGreen.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (totalCount > 0) "TODO $doneCount/$totalCount" else "TODO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricGreen,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (displayTitle.isNotBlank()) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = { onTogglePin?.invoke() },
            modifier = Modifier.size(24.dp)
        ) {
            val pinTint = if (note.isPinned) {
                ElectricGreen
            } else {
                if (isDark) Color(0xFF3F3F46) else palette.textSecondary.copy(alpha = 0.35f)
            }
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pin",
                tint = pinTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    val lines = remember(rawContent) {
        rawContent.lines().filter {
            it.trim().startsWith("- [") && it.substringAfter("] ").isNotBlank()
        }.take(6)
    }

    if (lines.isEmpty()) {
        val nonChecklistPreview = remember(rawContent) {
            rawContent.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("![") && !it.startsWith("[🎤") }
                .take(3)
                .joinToString("\n")
        }
        if (nonChecklistPreview.isNotBlank()) {
            Text(
                text = nonChecklistPreview,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val isChecked = line.contains("- [x]", ignoreCase = true)
            val rawItemText = line.substringAfter("] ").trim()
            val inlineReminder = remember(rawItemText) { InlineReminderParser.extractFirstReminder(rawItemText) }
            val itemAnnotatedText = remember(rawItemText) {
                val withoutReminder = InlineReminderParser.removeReminderFromLine(rawItemText)
                MentionParser.buildInlineAnnotatedString(withoutReminder)
            }

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .clickable {
                        if (onContentChange != null) {
                            val toggledPrefix = if (isChecked) "- [ ] " else "- [x] "
                            val toggledLine = line.replaceFirst(Regex("""^-\s*\[[ xX]\]\s*"""), toggledPrefix)
                            val newContent = rawContent.replace(line, toggledLine)
                            onContentChange(newContent)
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isChecked) ElectricGreen else palette.textPrimary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = itemAnnotatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isChecked) palette.textSecondary else palette.textPrimary,
                        textDecoration = if (isChecked) TextDecoration.None else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (inlineReminder != null) {
                        val reminderAccent = ElectricAmber
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = reminderAccent.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🔔", fontSize = 9.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = inlineReminder.displayText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = reminderAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Check if there is an attached image in the content
    val attachedImageUri = remember(rawContent) {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        regex.find(rawContent)?.groupValues?.getOrNull(1)
    }

    if (!attachedImageUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        AttachedImageThumbnail(
            imageRef = attachedImageUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (onImageClick != null) Modifier.clickable { onImageClick(attachedImageUri) }
                    else Modifier
                )
        )
    }

    val firstVoiceNote = remember(rawContent) {
        EditorBlockParser.extractVoiceNotes(rawContent).firstOrNull()
    }
    if (!firstVoiceNote.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        VoiceNoteCardBadge(
            audioUriString = firstVoiceNote,
            palette = palette,
            isDark = isDark
        )
    }


    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoundAppsRow(boundApps = boundApps, palette = palette, isDark = isDark)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val hasActiveReminder = (note.reminderTime != null && note.reminderTime > System.currentTimeMillis()) ||
                rawContent.contains("](reminder:")
            val hasVoiceNote = remember(rawContent) { EditorBlockParser.extractVoiceNotes(rawContent).isNotEmpty() }
            if (hasVoiceNote) {
                Text("🎙️", fontSize = 11.sp)
            }
            if (hasActiveReminder) {
                Text("🔔", fontSize = 11.sp)
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) StealthTextMuted else palette.textSecondary.copy(alpha = 0.8f),
                fontSize = 11.5.sp
            )
        }
    }
}

// ---------------- Markdown Note Card ----------------
@Composable
private fun MarkdownNoteCardContent(
    note: NoteMetadata,
    rawContent: String,
    palette: com.example.naughty.ui.theme.NoteColorPalette,
    isDark: Boolean,
    formattedTime: String,
    boundApps: List<String> = emptyList(),
    onTogglePin: (() -> Unit)?,
    onImageClick: ((String) -> Unit)? = null
) {
    val displayTitle = if (note.title.equals("New thought", ignoreCase = true) || note.title.equals("Untitled", ignoreCase = true)) "" else note.title
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayTitle.isNotBlank()) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.5.sp,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            onClick = { onTogglePin?.invoke() },
            modifier = Modifier.size(24.dp)
        ) {
            val pinTint = if (note.isPinned) {
                ElectricGreen
            } else {
                if (isDark) Color(0xFF3F3F46) else palette.textSecondary.copy(alpha = 0.35f)
            }
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pin",
                tint = pinTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // Check if there is an attached image in the content
    val attachedImageUri = remember(rawContent) {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        regex.find(rawContent)?.groupValues?.getOrNull(1)
    }

    if (!attachedImageUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        AttachedImageThumbnail(
            imageRef = attachedImageUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (onImageClick != null) Modifier.clickable { onImageClick(attachedImageUri) }
                    else Modifier
                )
        )
    }

    val firstVoiceNote = remember(rawContent) {
        EditorBlockParser.extractVoiceNotes(rawContent).firstOrNull()
    }
    if (!firstVoiceNote.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        VoiceNoteCardBadge(
            audioUriString = firstVoiceNote,
            palette = palette,
            isDark = isDark
        )
    }

    val previewText = remember(rawContent) {
        val withoutReminders = InlineReminderParser.cleanAllReminders(rawContent)
        val withoutMedia = withoutReminders
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // remove image syntax
            .replace(Regex("\\[🎤\\s*Voice Note\\]\\(.*?\\)", RegexOption.IGNORE_CASE), "") // remove voice note syntax
        val withoutMentions = MentionParser.cleanMentions(withoutMedia)
        withoutMentions
            .replace(Regex("[#*_~`>\\[\\]()]"), "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString("\n")
    }

    if (previewText.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = previewText,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }


    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoundAppsRow(boundApps = boundApps, palette = palette, isDark = isDark)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val hasActiveReminder = (note.reminderTime != null && note.reminderTime > System.currentTimeMillis()) ||
                rawContent.contains("](reminder:")
            val hasVoiceNote = remember(rawContent) { EditorBlockParser.extractVoiceNotes(rawContent).isNotEmpty() }
            if (hasVoiceNote) {
                Text("🎙️", fontSize = 11.sp)
            }
            if (hasActiveReminder) {
                Text("🔔", fontSize = 11.sp)
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) StealthTextMuted else palette.textSecondary.copy(alpha = 0.8f),
                fontSize = 11.5.sp
            )
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

private fun isChecklistContent(content: String): Boolean =
    content.contains("- [ ]") || content.contains("- [x]")

private fun formatNoteTimestamp(timestamp: Long): String {
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

@Composable
fun VoiceNoteCardBadge(
    audioUriString: String,
    palette: com.example.naughty.ui.theme.NoteColorPalette,
    isDark: Boolean,
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

    val accent = palette.iconTint

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) accent.copy(alpha = 0.12f) else accent.copy(alpha = 0.08f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.18f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice Note",
                        tint = accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
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
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

