package com.example.naughty.ui.timeline

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Timeline
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
import com.example.naughty.ui.oneui.components.OneUiCard
import com.example.naughty.ui.oneui.components.OneUiDonut
import com.example.naughty.ui.oneui.components.OneUiDonutSegment
import com.example.naughty.ui.oneui.components.OneUiEmptyState
import com.example.naughty.ui.oneui.components.OneUiFab
import com.example.naughty.ui.oneui.components.OneUiHeaderStyle
import com.example.naughty.ui.oneui.components.OneUiProgressBar
import com.example.naughty.ui.oneui.components.OneUiScaffold
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.timeline.components.CreateTrackerSheet

@Composable
fun TimelineListScreen(
    viewModel: TimelineListViewModel,
    onBack: () -> Unit,
    onTrackerClick: (trackerId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }

    OneUiScaffold(
        title = "Timeline",
        subtitle = if (uiState.trackers.isNotEmpty()) "${uiState.trackers.size} active goals" else null,
        headerStyle = OneUiHeaderStyle.Expanded,
        onBack = onBack,
        floatingAction = {
            OneUiFab(
                icon = Icons.Default.Add,
                contentDescription = "New Tracker",
                onClick = { showCreateSheet = true }
            )
        },
        modifier = modifier
    ) {
        if (uiState.trackers.isEmpty() && !uiState.isLoading) {
            item(key = "empty_state") {
                OneUiEmptyState(
                    icon = Icons.Outlined.Timeline,
                    title = "No Timeline Trackers",
                    subtitle = "Track multi-step goals with milestones, custom stages, and progress analytics.",
                    actionLabel = "New Tracker",
                    onAction = { showCreateSheet = true },
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        } else {
            // Overview Donut Analytics Card
            item(key = "overview_card") {
                Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter, vertical = 6.dp)) {
                    TimelineOverviewCard(
                        activeCount = uiState.activeCount,
                        blockedCount = uiState.blockedCount,
                        doneCount = uiState.doneCount,
                        totalCount = uiState.trackers.size
                    )
                }
            }

            // Tracker Cards
            items(uiState.trackers, key = { it.tracker.id }) { summary ->
                Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter, vertical = 6.dp)) {
                    TimelineTrackerCard(
                        summary = summary,
                        onClick = { onTrackerClick(summary.tracker.id) }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
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

/**
 * One UI 9 Donut Overview Card modeled directly on Samsung Health / Digital Wellbeing stats cards.
 */
@Composable
private fun TimelineOverviewCard(
    activeCount: Int,
    blockedCount: Int,
    doneCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val cyan = OneUi.badgePalette.blue
    val pink = OneUi.badgePalette.rose
    val green = OneUi.badgePalette.green
    val gray = OneUi.colors.hairline

    val segments = remember(activeCount, blockedCount, doneCount, totalCount) {
        if (totalCount == 0) {
            listOf(OneUiDonutSegment(1f, gray))
        } else {
            buildList {
                if (activeCount > 0) add(OneUiDonutSegment(activeCount.toFloat(), cyan))
                if (blockedCount > 0) add(OneUiDonutSegment(blockedCount.toFloat(), pink))
                if (doneCount > 0) add(OneUiDonutSegment(doneCount.toFloat(), green))
                if (isEmpty()) add(OneUiDonutSegment(1f, gray))
            }
        }
    }

    OneUiCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Goal Progress",
                    style = OneUi.typography.subhead,
                    color = OneUi.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalCount total",
                    style = OneUi.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats breakdown legend
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(label = "Active", count = activeCount, color = cyan)
                    LegendItem(label = "Blocked", count = blockedCount, color = pink)
                    LegendItem(label = "Done", count = doneCount, color = green)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            OneUiDonut(
                segments = segments,
                size = 100.dp,
                strokeWidth = 14.dp
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = OneUi.typography.caption,
            color = OneUi.colors.textSecondary,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = "$count",
            style = OneUi.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = OneUi.colors.textPrimary
        )
    }
}

@Composable
private fun TimelineTrackerCard(
    summary: TrackerSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tracker = summary.tracker
    val accent = TimelineColors.accentColor(tracker.accent)
    val statusColor = TimelineColors.categoryColor(summary.derivedStatus)
    val priorityColor = TimelineColors.priorityColor(tracker.priority)

    OneUiCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Priority Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tracker.title.ifBlank { "Untitled Goal" },
                    style = OneUi.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tracker.priority.name,
                        style = OneUi.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        fontSize = 10.sp
                    )
                }
            }

            if (tracker.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tracker.description,
                    style = OneUi.typography.body,
                    color = OneUi.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OneUiProgressBar(
                    progress = summary.progress.fraction,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${summary.progress.percent}%",
                    style = OneUi.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Milestone summary and status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val milestoneText = if (summary.nextUpMilestone != null) {
                    "${summary.progress.done}/${summary.progress.total} • Next: ${summary.nextUpMilestone.title}"
                } else if (summary.progress.total > 0) {
                    "${summary.progress.done} of ${summary.progress.total} completed"
                } else {
                    "No milestones yet"
                }

                Text(
                    text = milestoneText,
                    style = OneUi.typography.caption,
                    color = OneUi.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (summary.derivedStatus) {
                            StageCategory.NOT_STARTED -> "Not started"
                            StageCategory.IN_PROGRESS -> "In progress"
                            StageCategory.BLOCKED -> "Blocked"
                            StageCategory.DONE -> "Done"
                            StageCategory.CANCELLED -> "Cancelled"
                        },
                        style = OneUi.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
