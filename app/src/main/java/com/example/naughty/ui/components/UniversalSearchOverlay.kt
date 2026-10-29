package com.example.naughty.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.editor.MentionParser
import com.example.naughty.ui.oneui.components.OneUiCardGroup
import com.example.naughty.ui.oneui.components.OneUiChipFlow
import com.example.naughty.ui.oneui.components.OneUiEmptyState
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiLeading
import com.example.naughty.ui.oneui.components.OneUiListItem
import com.example.naughty.ui.oneui.components.OneUiSearchField
import com.example.naughty.ui.oneui.components.OneUiSectionHeader
import com.example.naughty.ui.oneui.components.OneUiTrailing
import com.example.naughty.ui.oneui.theme.OneUi
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
    trackers: List<TimelineTracker> = emptyList(),
    milestones: List<TimelineMilestone> = emptyList(),
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
    val keyboardController = LocalSoftwareKeyboardController.current

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
            notes.take(5)
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
            emptyList<Pair<TimelineTracker, String>>()
        } else {
            val trackerMap = trackers.associateBy { it.id }
            val results = mutableListOf<Pair<TimelineTracker, String>>()
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

    val recentKeywords = remember(notes) {
        notes.mapNotNull { it.title.trim().takeIf { t -> t.isNotBlank() && !t.equals("Untitled", ignoreCase = true) } }
            .distinct()
            .take(6)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUi.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Close search",
                        tint = OneUi.colors.textPrimary
                    )
                }
                Text(
                    text = "Search",
                    style = OneUi.type.barTitle,
                    color = OneUi.colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // When query is blank: show recent keywords and recent notes
                if (query.isBlank()) {
                    if (recentKeywords.isNotEmpty()) {
                        item(key = "search_recent_chips_header") {
                            Text(
                                text = "Recently modified",
                                style = OneUi.type.groupLabel,
                                color = OneUi.colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        item(key = "search_recent_chips") {
                            OneUiChipFlow(
                                items = recentKeywords,
                                onItemClick = { query = it }
                            )
                        }
                    }

                    if (filteredNotes.isNotEmpty()) {
                        item(key = "recent_notes_group") {
                            OneUiCardGroup(label = "Recent notes") {
                                filteredNotes.forEach { note ->
                                    item(key = note.id) {
                                        val snippet = if (note.isLocked) "Locked note"
                                        else {
                                            val raw = rawContents[note.id] ?: ""
                                            val cleaned = InlineReminderParser.cleanAllReminders(raw)
                                            MentionParser.cleanMentions(cleaned).replace(Regex("[#*_~`>\\[\\]()]"), "").trim()
                                        }
                                        OneUiListItem(
                                            title = note.title.ifBlank { "Untitled" },
                                            subtitle = snippet.take(80),
                                            leading = if (note.isLocked) {
                                                OneUiLeading.IconBadge(Icons.Outlined.Lock, OneUi.colors.warning)
                                            } else {
                                                OneUiLeading.IconBadge(Icons.AutoMirrored.Outlined.StickyNote2, OneUi.colors.accent)
                                            },
                                            trailing = OneUiTrailing.Chevron,
                                            onClick = {
                                                if (note.isLocked) {
                                                    if (activity != null) {
                                                        BiometricAuthHelper.authenticate(
                                                            activity = activity,
                                                            title = "Unlock Note",
                                                            subtitle = "Authenticate to view this note",
                                                            onSuccess = {
                                                                NoteLockSession.unlock(note.id)
                                                                onNoteClick(note.id)
                                                            },
                                                            onError = { err ->
                                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    } else {
                                                        onNoteClick(note.id)
                                                    }
                                                } else {
                                                    onNoteClick(note.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Query is active
                    val hasNotes = filteredNotes.isNotEmpty()
                    val hasTrackers = matchingTrackers.isNotEmpty()

                    if (!hasNotes && !hasTrackers) {
                        item(key = "search_empty") {
                            OneUiEmptyState(
                                title = "No matches found",
                                message = "No notes or timeline items match \"$query\"",
                                icon = Icons.Outlined.Search
                            )
                        }
                    } else {
                        if (hasNotes) {
                            item(key = "matching_notes_group") {
                                OneUiCardGroup(label = "Notes (${filteredNotes.size})") {
                                    filteredNotes.forEach { note ->
                                        item(key = note.id) {
                                            val snippet = if (note.isLocked) "Locked note"
                                            else {
                                                val raw = rawContents[note.id] ?: ""
                                                val cleaned = InlineReminderParser.cleanAllReminders(raw)
                                                MentionParser.cleanMentions(cleaned).replace(Regex("[#*_~`>\\[\\]()]"), "").trim()
                                            }
                                            OneUiListItem(
                                                title = note.title.ifBlank { "Untitled" },
                                                subtitle = snippet.take(80),
                                                leading = if (note.isLocked) {
                                                    OneUiLeading.IconBadge(Icons.Outlined.Lock, OneUi.colors.warning)
                                                } else {
                                                    OneUiLeading.IconBadge(Icons.AutoMirrored.Outlined.StickyNote2, OneUi.colors.accent)
                                                },
                                                trailing = OneUiTrailing.Chevron,
                                                onClick = {
                                                    if (note.isLocked) {
                                                        if (activity != null) {
                                                            BiometricAuthHelper.authenticate(
                                                                activity = activity,
                                                                title = "Unlock Note",
                                                                subtitle = "Authenticate to view this note",
                                                                onSuccess = {
                                                                    NoteLockSession.unlock(note.id)
                                                                    onNoteClick(note.id)
                                                                },
                                                                onError = { err ->
                                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            onNoteClick(note.id)
                                                        }
                                                    } else {
                                                        onNoteClick(note.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (hasTrackers) {
                            item(key = "matching_trackers_group") {
                                OneUiCardGroup(label = "Timeline (${matchingTrackers.size})") {
                                    matchingTrackers.forEach { (tracker, snippet) ->
                                        item(key = tracker.id) {
                                            OneUiListItem(
                                                title = tracker.title,
                                                subtitle = snippet,
                                                leading = OneUiLeading.IconBadge(Icons.Outlined.Timeline, OneUi.colors.badge.teal),
                                                trailing = OneUiTrailing.Chevron,
                                                onClick = { onTrackerClick(tracker.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Space for floating search input at bottom
                item(key = "search_bottom_space") {
                    Spacer(
                        modifier = Modifier
                            .height(80.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }

            // Bottom Search Input Docked Above Keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OneUiSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search notes or timeline...",
                    focusRequester = focusRequester,
                    onSearch = { keyboardController?.hide() }
                )
            }
        }
    }
}
