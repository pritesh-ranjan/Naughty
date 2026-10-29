package com.example.naughty.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineUpdate
import com.example.naughty.data.local.timeline.TimelineUpdateType
import com.example.naughty.ui.oneui.components.OneUiCard
import com.example.naughty.ui.oneui.components.OneUiConfirmDialog
import com.example.naughty.ui.oneui.components.OneUiDialog
import com.example.naughty.ui.oneui.components.OneUiDonut
import com.example.naughty.ui.oneui.components.OneUiDonutSegment
import com.example.naughty.ui.oneui.components.OneUiHeaderStyle
import com.example.naughty.ui.oneui.components.OneUiScaffold
import com.example.naughty.ui.oneui.components.OneUiSegmentedControl
import com.example.naughty.ui.oneui.components.OneUiTextField
import com.example.naughty.ui.oneui.components.OneUiTonalButton
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.timeline.components.EditTrackerSheet
import com.example.naughty.ui.timeline.components.ManageStagesSheet
import com.example.naughty.ui.timeline.components.MilestoneEditSheet
import com.example.naughty.ui.timeline.components.StatusPickerSheet
import com.example.naughty.ui.timeline.components.TimelineStepper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineDetailScreen(
    viewModel: TimelineDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tracker = uiState.tracker

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Milestones, 1 = Activity
    var menuExpanded by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showEditTrackerSheet by remember { mutableStateOf(false) }
    var showManageStagesSheet by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<TimelineMilestone?>(null) }
    var pickingStatusMilestone by remember { mutableStateOf<TimelineMilestone?>(null) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Activity note input
    var newNoteText by remember { mutableStateOf("") }

    if (tracker == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OneUi.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading tracker...", color = OneUi.colors.textSecondary, style = OneUi.typography.body)
        }
        return
    }

    val accent = TimelineColors.accentColor(tracker.accent)
    val priorityColor = TimelineColors.priorityColor(tracker.priority)

    OneUiScaffold(
        title = tracker.title,
        headerStyle = OneUiHeaderStyle.Compact,
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = OneUi.colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = OneUi.shapes.menu,
                    containerColor = OneUi.colors.surfaceElevated
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Tracker", color = OneUi.colors.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OneUi.colors.textPrimary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            showEditTrackerSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Manage Stages", color = OneUi.colors.textPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = OneUi.colors.textPrimary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            showManageStagesSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Tracker", color = OneUi.colors.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = OneUi.colors.error, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        },
        bottomDock = if (selectedTab == 1) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OneUi.colors.surfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OneUiTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = "Log a progress note or update...",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.postUpdate(newNoteText.trim())
                                    newNoteText = ""
                                }
                            },
                            enabled = newNoteText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Post",
                                tint = if (newNoteText.isNotBlank()) OneUi.colors.accent else OneUi.colors.textTertiary
                            )
                        }
                    }
                }
            }
        } else null,
        modifier = modifier.imePadding()
    ) {
        // Summary Header Card with Donut Chart
        item(key = "header_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OneUi.dimens.screenGutter, vertical = 6.dp)
            ) {
                OneUiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val donutSegments = remember(uiState.progress.fraction, accent) {
                            val fraction = uiState.progress.fraction.coerceIn(0f, 1f)
                            if (fraction <= 0f) {
                                listOf(OneUiDonutSegment(1f, Color(0x33888888)))
                            } else if (fraction >= 1f) {
                                listOf(OneUiDonutSegment(1f, accent))
                            } else {
                                listOf(
                                    OneUiDonutSegment(fraction, accent),
                                    OneUiDonutSegment(1f - fraction, Color(0x33888888))
                                )
                            }
                        }

                        Box(contentAlignment = Alignment.Center) {
                            OneUiDonut(
                                segments = donutSegments,
                                size = 64.dp,
                                strokeWidth = 9.dp
                            )
                            Text(
                                text = "${uiState.progress.percent}%",
                                style = OneUi.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = OneUi.colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (uiState.derivedStatus) {
                                        StageCategory.DONE -> "All Milestones Completed"
                                        StageCategory.BLOCKED -> "Action Blocked / On Hold"
                                        StageCategory.IN_PROGRESS -> "In Progress"
                                        StageCategory.CANCELLED -> "Cancelled"
                                        StageCategory.NOT_STARTED -> "Not Started"
                                    },
                                    style = OneUi.typography.subhead,
                                    fontWeight = FontWeight.Bold,
                                    color = TimelineColors.categoryColor(uiState.derivedStatus),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(priorityColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
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

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${uiState.progress.done} of ${uiState.progress.total} milestones finished",
                                style = OneUi.typography.caption,
                                color = OneUi.colors.textSecondary
                            )

                            if (tracker.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tracker.description,
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Segmented Control
        item(key = "tab_control") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OneUi.dimens.screenGutter, vertical = 4.dp)
            ) {
                OneUiSegmentedControl(
                    items = listOf("Milestones", "Activity (${uiState.updates.size})"),
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (selectedTab == 0) {
            // MILESTONES TAB
            item(key = "stepper") {
                Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter, vertical = 6.dp)) {
                    TimelineStepper(
                        milestones = uiState.milestones,
                        stages = uiState.stageMap,
                        onMilestoneClick = { editingMilestone = it },
                        onStatusClick = { pickingStatusMilestone = it },
                        onMoveMilestone = { id, up -> viewModel.moveMilestone(id, up) },
                        onDeleteMilestone = { viewModel.deleteMilestone(it.id) }
                    )
                }
            }

            item(key = "add_milestone_button") {
                Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter, vertical = 8.dp)) {
                    OneUiTonalButton(
                        text = "Add Milestone",
                        icon = Icons.Default.Add,
                        onClick = { showAddMilestoneDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        } else {
            // ACTIVITY TAB
            items(uiState.updates, key = { it.id }) { update ->
                Box(modifier = Modifier.padding(horizontal = OneUi.dimens.screenGutter, vertical = 4.dp)) {
                    ActivityUpdateCard(update = update)
                }
            }

            if (uiState.updates.isEmpty()) {
                item(key = "empty_activity") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No updates recorded yet.",
                            style = OneUi.typography.caption,
                            color = OneUi.colors.textSecondary
                        )
                    }
                }
            }

            item(key = "bottom_activity_spacer") {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Sheets & Dialogs
    if (editingMilestone != null) {
        MilestoneEditSheet(
            milestone = editingMilestone!!,
            stages = uiState.stages,
            onDismiss = { editingMilestone = null },
            onSave = { updated, prevRem, prevDue ->
                viewModel.updateMilestone(updated, prevRem, prevDue)
                editingMilestone = null
            },
            onDelete = { id ->
                viewModel.deleteMilestone(id)
                editingMilestone = null
            }
        )
    }

    if (pickingStatusMilestone != null) {
        StatusPickerSheet(
            milestone = pickingStatusMilestone!!,
            stages = uiState.stages,
            onDismiss = { pickingStatusMilestone = null },
            onSelectStage = { stageId ->
                viewModel.setMilestoneStage(pickingStatusMilestone!!.id, stageId)
                pickingStatusMilestone = null
            },
            onManageStagesClick = {
                showManageStagesSheet = true
            }
        )
    }

    if (showAddMilestoneDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newNote by remember { mutableStateOf("") }

        OneUiDialog(
            title = "New Milestone",
            onDismiss = { showAddMilestoneDialog = false },
            confirmLabel = "Add",
            dismissLabel = "Cancel",
            confirmEnabled = newTitle.isNotBlank(),
            onConfirm = {
                if (newTitle.isNotBlank()) {
                    viewModel.addMilestone(newTitle.trim(), newNote.trim())
                    showAddMilestoneDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OneUiTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = "Milestone Title",
                    placeholder = "e.g. Valuation report, Sanction letter",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OneUiTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    label = "Notes (Optional)",
                    placeholder = "Additional context or reference...",
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showEditTrackerSheet) {
        EditTrackerSheet(
            tracker = tracker,
            onDismiss = { showEditTrackerSheet = false },
            onSave = { title, desc, prio, acc ->
                viewModel.updateTracker(title, desc, prio, acc)
                showEditTrackerSheet = false
            },
            onDelete = {
                viewModel.deleteTracker(onDeleted = onBack)
            }
        )
    }

    if (showManageStagesSheet) {
        ManageStagesSheet(
            stages = uiState.stages,
            onDismiss = { showManageStagesSheet = false },
            onAddCustomStage = { label, colorKey, category ->
                viewModel.addCustomStage(label, colorKey, category)
            },
            onDeleteCustomStage = { stageId ->
                viewModel.deleteCustomStage(stageId)
            }
        )
    }

    if (showDeleteConfirm) {
        OneUiConfirmDialog(
            title = "Delete Tracker?",
            message = "Are you sure you want to permanently delete '${tracker.title}'?",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteTracker(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun ActivityUpdateCard(
    update: TimelineUpdate,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(update.createdAt) {
        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(update.createdAt))
    }

    OneUiCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (badgeBg, badgeFg, label) = when (update.type) {
                    TimelineUpdateType.STATUS_CHANGE -> Triple(OneUi.badgePalette.blue.copy(alpha = 0.16f), OneUi.badgePalette.blue, "STATUS CHANGE")
                    TimelineUpdateType.MILESTONE_ADDED -> Triple(OneUi.badgePalette.green.copy(alpha = 0.16f), OneUi.badgePalette.green, "ADDED")
                    TimelineUpdateType.MILESTONE_REMOVED -> Triple(OneUi.badgePalette.rose.copy(alpha = 0.16f), OneUi.badgePalette.rose, "REMOVED")
                    TimelineUpdateType.NOTE -> Triple(OneUi.colors.hairline, OneUi.colors.textSecondary, "NOTE")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = OneUi.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text = formattedTime,
                    style = OneUi.typography.caption,
                    color = OneUi.colors.textTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = update.text,
                style = OneUi.typography.body,
                color = OneUi.colors.textPrimary
            )
        }
    }
}
