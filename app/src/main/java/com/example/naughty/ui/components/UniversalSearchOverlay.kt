package com.example.naughty.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.getNoteColorPalette
import com.example.naughty.ui.theme.isAppInDarkTheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.naughty.util.BiometricAuthHelper
import com.example.naughty.util.NoteLockSession
import com.example.naughty.util.findFragmentActivity
import kotlinx.coroutines.delay

@Composable
fun UniversalSearchOverlay(
    isOpen: Boolean,
    notes: List<NoteMetadata>,
    rawContents: Map<String, String>,
    boundApps: Map<String, String>,
    trackers: List<com.example.naughty.data.local.timeline.TimelineTracker> = emptyList(),
    milestones: List<com.example.naughty.data.local.timeline.TimelineMilestone> = emptyList(),
    onNoteClick: (String) -> Unit,
    onTrackerClick: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    BackHandler { onDismiss() }

    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = isAppInDarkTheme()

    LaunchedEffect(Unit) {
        delay(120)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val filteredNotes = remember(query, notes, rawContents, boundApps) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            notes.take(5) // Show top recent notes when query is blank
        } else {
            notes.filter { note ->
                val content = rawContents[note.id] ?: ""
                val bound = boundApps[note.id] ?: ""
                note.title.lowercase().contains(q) ||
                        content.lowercase().contains(q) ||
                        bound.lowercase().contains(q)
            }
        }
    }

    val matchingTrackers = remember(query, trackers, milestones) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            emptyList<Pair<com.example.naughty.data.local.timeline.TimelineTracker, String>>()
        } else {
            val trackerMap = trackers.associateBy { it.id }
            val results = mutableListOf<Pair<com.example.naughty.data.local.timeline.TimelineTracker, String>>()
            val seenTrackerIds = mutableSetOf<String>()

            for (t in trackers) {
                if (t.title.lowercase().contains(q) || t.description.lowercase().contains(q)) {
                    val snippet = if (t.description.isNotBlank()) t.description else "Timeline Tracker"
                    results.add(Pair(t, snippet))
                    seenTrackerIds.add(t.id)
                }
            }

            for (m in milestones) {
                if (m.trackerId !in seenTrackerIds) {
                    if (m.title.lowercase().contains(q) || m.note.lowercase().contains(q)) {
                        val parent = trackerMap[m.trackerId]
                        if (parent != null) {
                            results.add(Pair(parent, "Milestone: ${m.title}"))
                            seenTrackerIds.add(parent.id)
                        }
                    }
                }
            }

            results
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
            .imePadding()
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent click-through to scrim
                    .shadow(24.dp, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                color = if (isDark) AmoledBlack else CrystalWhite,
                border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(bottom = 16.dp)
                ) {
                    // Pull indicator bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                var totalDragY = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { totalDragY = 0f },
                                    onDragEnd = {
                                        if (totalDragY < -30f) {
                                            onDismiss()
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        totalDragY += dragAmount
                                        if (totalDragY < -35f) {
                                            change.consume()
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0))
                        )
                    }

                    // Search input bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF4F4F6),
                        border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = if (isDark) ElectricGreen else AmoledBlack,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isDark) CrystalWhite else AmoledBlack
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(if (isDark) ElectricGreen else AmoledBlack),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = "Search notes, timeline tasks, text...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isDark) Color(0xFF666666) else Color(0xFF999999),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = if (isDark) Color(0xFF888888) else Color(0xFF666666),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Close",
                                    tint = if (isDark) Color(0xFF888888) else Color(0xFF666666),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Results List
                    if (filteredNotes.isEmpty() && matchingTrackers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (query.isBlank()) "No notes yet" else "No matching notes or timeline tasks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFF666666) else Color(0xFF888888)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .padding(horizontal = 14.dp)
                        ) {
                            // Timeline matches section
                            if (matchingTrackers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "TIMELINE TASKS (${matchingTrackers.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }

                                items(matchingTrackers, key = { "tracker_${it.first.id}" }) { pair ->
                                    val tracker = pair.first
                                    val matchSnippet = pair.second
                                    val accent = com.example.naughty.ui.timeline.TimelineColors.accentColor(tracker.accent)
                                    val pColor = com.example.naughty.ui.timeline.TimelineColors.priorityColor(tracker.priority)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                onTrackerClick(tracker.id)
                                                onDismiss()
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF9F9FB)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(60.dp)
                                                    .background(accent)
                                            )
                                            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = tracker.title.ifBlank { "Untitled Tracker" },
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) CrystalWhite else AmoledBlack,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = pColor.copy(alpha = 0.16f),
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = tracker.priority.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = pColor,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = matchSnippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Notes section
                            if (filteredNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (query.isBlank()) "RECENT NOTES" else "NOTES (${filteredNotes.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) ElectricGreen else AmoledBlack,
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }
                            items(filteredNotes, key = { it.id }) { note ->
                                val palette = getNoteColorPalette(note.colorTheme, isDark)
                                val boundApp = boundApps[note.id]
                                val content = rawContents[note.id] ?: ""
                                val snippet = content
                                    .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
                                    .replace(Regex("\\[🎤\\s*Voice Note\\]\\(.*?\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("[#*_~`>\\[\\]()]"), "")
                                    .trim()
                                    .take(90)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (note.isLocked) {
                                                if (activity != null) {
                                                    BiometricAuthHelper.authenticate(
                                                        activity = activity,
                                                        title = "Unlock Note",
                                                        subtitle = "Authenticate to view and edit this note",
                                                        onSuccess = {
                                                            NoteLockSession.unlock(note.id)
                                                            onNoteClick(note.id)
                                                            onDismiss()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                } else {
                                                    onNoteClick(note.id)
                                                    onDismiss()
                                                }
                                            } else {
                                                onNoteClick(note.id)
                                                onDismiss()
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF0C0C0E) else palette.surface,
                                    border = BorderStroke(0.6.dp, if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                                    Icon(
                                                        imageVector = Icons.Outlined.Lock,
                                                        contentDescription = "Locked",
                                                        tint = ElectricAmber,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .padding(end = 4.dp)
                                                    )
                                                }
                                                Text(
                                                    text = note.title.ifBlank { "Untitled" },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDark) CrystalWhite else palette.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (!boundApp.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ElectricCyan.copy(alpha = 0.12f),
                                                    border = BorderStroke(0.5.dp, ElectricCyan.copy(alpha = 0.25f)),
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Link,
                                                            contentDescription = null,
                                                            tint = ElectricCyan,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = boundApp,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ElectricCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (note.isLocked) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Locked note • Tap to unlock",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ElectricAmber.copy(alpha = 0.85f),
                                                fontSize = 12.sp
                                            )
                                        } else if (snippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = snippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color(0xFF999999) else palette.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
