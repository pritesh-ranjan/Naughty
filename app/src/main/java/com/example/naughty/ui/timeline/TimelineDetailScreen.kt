package com.example.naughty.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineUpdate
import com.example.naughty.data.local.timeline.TimelineUpdateType
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.BrandTitleStyle
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.ui.timeline.TimelineColors
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
    val isDark = isAppInDarkTheme()
    val tracker = uiState.tracker

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Timeline, 1 = Activity
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
                .background(if (isDark) AmoledBlack else CrystalWhite),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading tracker...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val accent = TimelineColors.accentColor(tracker.accent)
    val priorityColor = TimelineColors.priorityColor(tracker.priority)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AmoledBlack else CrystalWhite)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = tracker.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CrystalWhite else AmoledBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.16f)
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

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = if (isDark) CrystalWhite else AmoledBlack
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Tracker") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                showEditTrackerSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Stages") },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                showManageStagesSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Tracker", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // Summary Header Card with Circular Progress
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF9F9FB),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Progress Dial
                    CircularProgressDial(
                        progressFraction = uiState.progress.fraction,
                        percent = uiState.progress.percent,
                        accent = accent,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (uiState.derivedStatus) {
                                StageCategory.DONE -> "All Milestones Completed"
                                StageCategory.BLOCKED -> "Action Blocked / On Hold"
                                StageCategory.IN_PROGRESS -> "In Progress"
                                StageCategory.CANCELLED -> "Cancelled"
                                StageCategory.NOT_STARTED -> "Not Started"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TimelineColors.categoryColor(uiState.derivedStatus)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${uiState.progress.done} of ${uiState.progress.total} milestones finished",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (tracker.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tracker.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Segmented Tab Toggle: Timeline | Activity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF141418) else Color(0xFFF0F0F2))
                    .padding(3.dp)
            ) {
                // Timeline Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) (if (isDark) Color(0xFF26262E) else CrystalWhite) else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) (if (isDark) CrystalWhite else AmoledBlack) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Activity Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) (if (isDark) Color(0xFF26262E) else CrystalWhite) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Activity (${uiState.updates.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) (if (isDark) CrystalWhite else AmoledBlack) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Area based on Tab
            if (selectedTab == 0) {
                // TIMELINE TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    item {
                        TimelineStepper(
                            milestones = uiState.milestones,
                            stages = uiState.stageMap,
                            onMilestoneClick = { editingMilestone = it },
                            onStatusClick = { pickingStatusMilestone = it },
                            onMoveMilestone = { id, up -> viewModel.moveMilestone(id, up) },
                            onDeleteMilestone = { viewModel.deleteMilestone(it.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddMilestoneDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF141418) else Color(0xFFEEEEF0),
                                contentColor = if (isDark) CrystalWhite else AmoledBlack
                            ),
                            elevation = null
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Milestone", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            } else {
                // ACTIVITY TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.updates, key = { it.id }) { update ->
                            ActivityUpdateRow(update = update, isDark = isDark)
                        }

                        if (uiState.updates.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No updates recorded yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Note Input Bar
                    Surface(
                        color = if (isDark) Color(0xFF0E0E12) else Color(0xFFF4F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                placeholder = { Text("Log a progress note or update...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFEBEBEF),
                                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFEBEBEF)
                                )
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
                                    tint = if (newNoteText.isNotBlank()) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
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
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("New Milestone") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Milestone Title") },
                        placeholder = { Text("e.g. Valuation report, Sanction letter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4),
                            unfocusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4),
                            unfocusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addMilestone(newTitle.trim(), newNote.trim())
                            showAddMilestoneDialog = false
                        }
                    },
                    enabled = newTitle.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Tracker?") },
            text = { Text("Are you sure you want to permanently delete '${tracker.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTracker(onDeleted = onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CircularProgressDial(
    progressFraction: Float,
    percent: Int,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val progressAnim by animateFloatAsState(
        targetValue = progressFraction,
        label = "dial"
    )

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val trackColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFE4E4E7)

            // Background track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progressAnim,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) CrystalWhite else AmoledBlack,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ActivityUpdateRow(
    update: TimelineUpdate,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(update.createdAt) {
        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(update.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF9F9FB),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (update.type) {
                        TimelineUpdateType.STATUS_CHANGE -> Color(0xFF00E5FF).copy(alpha = 0.16f)
                        TimelineUpdateType.MILESTONE_ADDED -> Color(0xFF00F59B).copy(alpha = 0.16f)
                        TimelineUpdateType.MILESTONE_REMOVED -> Color(0xFFFF3366).copy(alpha = 0.16f)
                        TimelineUpdateType.NOTE -> if (isDark) Color(0xFF26262E) else Color(0xFFE4E4E7)
                    }
                ) {
                    Text(
                        text = when (update.type) {
                            TimelineUpdateType.STATUS_CHANGE -> "STATUS CHANGE"
                            TimelineUpdateType.MILESTONE_ADDED -> "ADDED"
                            TimelineUpdateType.MILESTONE_REMOVED -> "REMOVED"
                            TimelineUpdateType.NOTE -> "NOTE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (update.type) {
                            TimelineUpdateType.STATUS_CHANGE -> Color(0xFF00E5FF)
                            TimelineUpdateType.MILESTONE_ADDED -> Color(0xFF00F59B)
                            TimelineUpdateType.MILESTONE_REMOVED -> Color(0xFFFF3366)
                            TimelineUpdateType.NOTE -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = update.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}
