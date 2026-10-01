package com.example.naughty.ui.notelist

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
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
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.theme.StealthAppBadgePill
import com.example.naughty.ui.theme.StealthAppBadgeText
import com.example.naughty.ui.theme.StealthTextMuted
import com.example.naughty.ui.theme.getNoteColorPalette
import com.example.naughty.ui.theme.isAppInDarkTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: NoteMetadata,
    rawContent: String,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            val isChecklist = note.cardType == "checklist" || isChecklistContent(rawContent)

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
            val pillColor = if (isDark) StealthAppBadgePill else palette.border.copy(alpha = 0.5f)
            val textColor = if (isDark) StealthAppBadgeText else palette.textSecondary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pillColor
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Bound App",
                        tint = textColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayTitle.isNotBlank()) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.5.sp,
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
                palette.textPrimary
            } else {
                if (isDark) Color(0xFF4A4D56) else palette.textSecondary.copy(alpha = 0.4f)
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

    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val isChecked = line.contains("- [x]", ignoreCase = true)
            val rawItemText = line.substringAfter("] ").trim()
            val inlineReminder = remember(rawItemText) { InlineReminderParser.extractFirstReminder(rawItemText) }
            val itemText = remember(rawItemText) { InlineReminderParser.removeReminderFromLine(rawItemText) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
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
                    tint = if (isChecked) palette.textSecondary else palette.textPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isChecked) palette.textSecondary else palette.textPrimary,
                    textDecoration = if (isChecked) TextDecoration.None else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (inlineReminder != null) {
                    val reminderAccent = Color(0xFFE5A93C)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = reminderAccent.copy(alpha = 0.15f),
                        border = BorderStroke(0.7.dp, reminderAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text("🔔", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(2.5.dp))
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
                palette.textPrimary
            } else {
                if (isDark) Color(0xFF4A4D56) else palette.textSecondary.copy(alpha = 0.4f)
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

    val previewText = remember(rawContent) {
        InlineReminderParser.cleanAllReminders(rawContent)
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // remove image syntax
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
    val bitmap = remember(imageRef) {
        try {
            if (imageRef.startsWith("content://") || imageRef.startsWith("file://")) {
                val uri = Uri.parse(imageRef)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } else if (imageRef.startsWith("/")) {
                BitmapFactory.decodeFile(imageRef)?.asImageBitmap()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Attached image",
            contentScale = contentScale,
            modifier = modifier
        )
    }
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
