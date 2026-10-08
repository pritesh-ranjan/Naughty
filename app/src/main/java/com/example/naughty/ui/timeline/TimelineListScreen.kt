package com.example.naughty.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.BrandTitleStyle
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.ui.timeline.components.CreateTrackerSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineListScreen(
    viewModel: TimelineListViewModel,
    onBack: () -> Unit,
    onTrackerClick: (trackerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AmoledBlack else CrystalWhite)
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) CrystalWhite else AmoledBlack
                    )
                }

                Text(
                    text = "TIMELINE",
                    style = BrandTitleStyle,
                    color = if (isDark) CrystalWhite else AmoledBlack
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            // Stat Summary Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    count = uiState.activeCount,
                    label = "Active",
                    color = ElectricCyan,
                    isDark = isDark
                )
                StatPill(
                    count = uiState.blockedCount,
                    label = "Blocked",
                    color = ElectricPink,
                    isDark = isDark
                )
                StatPill(
                    count = uiState.doneCount,
                    label = "Done",
                    color = ElectricGreen,
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main List or Empty State
            if (uiState.trackers.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Timeline,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "No Timeline Trackers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) CrystalWhite else AmoledBlack
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Track important multi-step goals with milestones, deadlines and stage progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = { showCreateSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) ElectricCyan.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.15f),
                                contentColor = if (isDark) ElectricCyan else Color(0xFF007790)
                            ),
                            elevation = null
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Tracker", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.trackers, key = { it.tracker.id }) { summary ->
                        TrackerCard(
                            summary = summary,
                            onClick = { onTrackerClick(summary.tracker.id) },
                            isDark = isDark
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB to add new tracker
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            containerColor = ElectricGreen,
            contentColor = AmoledBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Tracker", modifier = Modifier.size(24.dp))
        }

        if (showCreateSheet) {
            CreateTrackerSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { title, desc, priority, accent, initialMilestones ->
                    viewModel.createTracker(
                        title = title,
                        description = desc,
                        priority = priority,
                        accent = accent,
                        initialMilestones = initialMilestones,
                        onCreated = { newTrackerId ->
                            onTrackerClick(newTrackerId)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun StatPill(
    count: Int,
    label: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.10f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TrackerCard(
    summary: TrackerSummary,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val tracker = summary.tracker
    val accent = TimelineColors.accentColor(tracker.accent)
    val statusColor = TimelineColors.categoryColor(summary.derivedStatus)
    val priorityColor = TimelineColors.priorityColor(tracker.priority)

    val progressAnim by animateFloatAsState(
        targetValue = summary.progress.fraction,
        label = "progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF9F9FB)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                // Top Row: Title + Priority Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tracker.title.ifBlank { "Untitled Tracker" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CrystalWhite else AmoledBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.16f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = tracker.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (tracker.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tracker.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar & Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom rounded progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnim)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accent)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${summary.progress.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CrystalWhite else AmoledBlack,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Milestone stats + Status chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Milestone count & next up snippet
                    val milestoneText = if (summary.nextUpMilestone != null) {
                        "${summary.progress.done}/${summary.progress.total} • Next: ${summary.nextUpMilestone.title}"
                    } else if (summary.progress.total > 0) {
                        "${summary.progress.done} of ${summary.progress.total} milestones completed"
                    } else {
                        "No milestones yet"
                    }

                    Text(
                        text = milestoneText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Derived status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = if (isDark) 0.16f else 0.12f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = when (summary.derivedStatus) {
                                StageCategory.NOT_STARTED -> "Not started"
                                StageCategory.IN_PROGRESS -> "In progress"
                                StageCategory.BLOCKED -> "Blocked"
                                StageCategory.DONE -> "Done"
                                StageCategory.CANCELLED -> "Cancelled"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
