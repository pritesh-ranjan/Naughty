package com.example.naughty.ui.timeline.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiConfirmDialog
import com.example.naughty.ui.oneui.components.OneUiDialog
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiSwitch
import com.example.naughty.ui.oneui.components.OneUiTextField
import com.example.naughty.ui.oneui.components.OneUiTonalButton
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.timeline.TimelineColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTrackerSheet(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        description: String,
        priority: TrackerPriority,
        accent: String,
        initialMilestones: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TrackerPriority.MEDIUM) }
    var accent by remember { mutableStateOf("green") }
    val initialMilestones = remember { mutableStateListOf("", "") }

    OneUiBottomSheet(
        title = "New Tracker",
        subtitle = "Track milestones and progress over time",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OneUiTextField(
                value = title,
                onValueChange = { title = it },
                label = "Tracker Title",
                placeholder = "e.g. Home Loan Approval, Passport Renewal",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OneUiTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description (Optional)",
                placeholder = "Details or reference numbers...",
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selector
            Text(
                text = "PRIORITY",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerPriority.entries.forEach { p ->
                    val selected = p == priority
                    val pColor = TimelineColors.priorityColor(p)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) pColor.copy(alpha = 0.18f) else OneUi.colors.surfaceElevated)
                            .clickable { priority = p }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.name,
                            style = OneUi.typography.caption,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) pColor else OneUi.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Accent Swatches
            Text(
                text = "ACCENT COLOR",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimelineColors.availableAccents.forEach { acc ->
                    val selected = acc == accent
                    val color = TimelineColors.accentColor(acc)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { accent = acc },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Initial Milestones section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INITIAL MILESTONES (OPTIONAL)",
                    style = OneUi.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = { initialMilestones.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OneUi.colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row", color = OneUi.colors.accent, fontSize = 12.sp)
                }
            }

            initialMilestones.forEachIndexed { idx, msTitle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OneUiTextField(
                        value = msTitle,
                        onValueChange = { initialMilestones[idx] = it },
                        placeholder = "Step ${idx + 1}",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (initialMilestones.size > 1) {
                        IconButton(onClick = { initialMilestones.removeAt(idx) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = OneUi.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OneUiFilledButton(
                text = "Create Tracker",
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            title.trim(),
                            description.trim(),
                            priority,
                            accent,
                            initialMilestones.filter { it.isNotBlank() }
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneEditSheet(
    milestone: TimelineMilestone,
    stages: List<TimelineStage>,
    onDismiss: () -> Unit,
    onSave: (milestone: TimelineMilestone, previousReminderEnabled: Boolean, previousDueAt: Long?) -> Unit,
    onDelete: (milestoneId: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(milestone.title) }
    var note by remember { mutableStateOf(milestone.note) }
    var stageId by remember { mutableStateOf(milestone.stageId) }
    var dueAt by remember { mutableStateOf(milestone.dueAt) }
    var reminderEnabled by remember { mutableStateOf(milestone.reminderEnabled) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    OneUiBottomSheet(
        title = "Edit Milestone",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OneUiTextField(
                value = title,
                onValueChange = { title = it },
                label = "Milestone Title",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OneUiTextField(
                value = note,
                onValueChange = { note = it },
                label = "Notes / Extra Details",
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Stage Selection
            Text(
                text = "STAGE STATUS",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stages.forEach { stage ->
                    val selected = stage.id == stageId
                    val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) stageCol.copy(alpha = 0.2f) else OneUi.colors.surfaceElevated)
                            .clickable { stageId = stage.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = stageCol,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(stageCol)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stage.label,
                                style = OneUi.typography.caption,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) stageCol else OneUi.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Due Date & Time Picker
            Text(
                text = "DUE DATE & REMINDER",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = OneUi.shapes.inner,
                color = OneUi.colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = OneUi.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (dueAt != null) {
                                        SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(dueAt!!))
                                    } else {
                                        "No due date set"
                                    },
                                    style = OneUi.typography.body,
                                    fontWeight = if (dueAt != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = OneUi.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val cal = Calendar.getInstance().apply {
                                    if (dueAt != null) timeInMillis = dueAt!!
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                cal.set(Calendar.MINUTE, minute)
                                                cal.set(Calendar.SECOND, 0)
                                                dueAt = cal.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Text(if (dueAt == null) "Set" else "Change", color = OneUi.colors.accent, fontSize = 12.sp)
                            }

                            if (dueAt != null) {
                                TextButton(onClick = {
                                    dueAt = null
                                    reminderEnabled = false
                                }) {
                                    Text("Clear", color = OneUi.colors.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (dueAt != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (reminderEnabled) OneUi.colors.accent else OneUi.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alarm notification on due time",
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textPrimary
                                )
                            }

                            OneUiSwitch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OneUiFilledButton(
                text = "Save Changes",
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            milestone.copy(
                                title = title.trim(),
                                note = note.trim(),
                                stageId = stageId,
                                dueAt = dueAt,
                                reminderEnabled = reminderEnabled
                            ),
                            milestone.reminderEnabled,
                            milestone.dueAt
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OneUiTonalButton(
                text = "Delete Milestone",
                icon = Icons.Default.Delete,
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        OneUiConfirmDialog(
            title = "Delete Milestone?",
            message = "Are you sure you want to remove '${milestone.title}'? This action cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete(milestone.id)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    milestone: TimelineMilestone,
    stages: List<TimelineStage>,
    onDismiss: () -> Unit,
    onSelectStage: (stageId: String) -> Unit,
    onManageStagesClick: () -> Unit
) {
    OneUiBottomSheet(
        title = "Update Status",
        subtitle = "Milestone: ${milestone.title}",
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    onDismiss()
                    onManageStagesClick()
                }) {
                    Text("Manage Stages", color = OneUi.colors.accent, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stages, key = { it.id }) { stage ->
                    val isSelected = stage.id == milestone.stageId
                    val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OneUi.shapes.inner)
                            .background(if (isSelected) stageCol.copy(alpha = 0.16f) else OneUi.colors.surfaceElevated)
                            .clickable {
                                onSelectStage(stage.id)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(stageCol)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stage.label,
                                        style = OneUi.typography.body,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) stageCol else OneUi.colors.textPrimary
                                    )
                                    Text(
                                        text = if (stage.isBuiltIn) "Standard (${stage.category.name})" else "Custom stage (${stage.category.name})",
                                        style = OneUi.typography.caption,
                                        color = OneUi.colors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = stageCol,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStagesSheet(
    stages: List<TimelineStage>,
    onDismiss: () -> Unit,
    onAddCustomStage: (label: String, colorKey: String, category: StageCategory) -> Unit,
    onDeleteCustomStage: (stageId: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var stageToDelete by remember { mutableStateOf<TimelineStage?>(null) }

    OneUiBottomSheet(
        title = "Stages & Statuses",
        subtitle = "Add custom workflow stages. Built-in stages cannot be deleted.",
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OneUi.colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Stage", color = OneUi.colors.accent, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stages, key = { it.id }) { stage ->
                    val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OneUi.shapes.inner)
                            .background(OneUi.colors.surfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(stageCol)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stage.label,
                                        style = OneUi.typography.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OneUi.colors.textPrimary
                                    )
                                    Text(
                                        text = if (stage.isBuiltIn) "Built-in" else "Custom • Category: ${stage.category.name}",
                                        style = OneUi.typography.caption,
                                        color = OneUi.colors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (!stage.isBuiltIn) {
                                IconButton(
                                    onClick = { stageToDelete = stage },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Stage",
                                        tint = OneUi.colors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("cyan") }
        var selectedCategory by remember { mutableStateOf(StageCategory.IN_PROGRESS) }

        OneUiDialog(
            title = "Add Custom Stage",
            onDismiss = { showAddDialog = false },
            confirmLabel = "Add Stage",
            dismissLabel = "Cancel",
            confirmEnabled = label.isNotBlank(),
            onConfirm = {
                if (label.isNotBlank()) {
                    onAddCustomStage(label.trim(), selectedColor, selectedCategory)
                    showAddDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OneUiTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Stage Label",
                    placeholder = "e.g. Awaiting Bank, In Review",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "BASE CATEGORY",
                    style = OneUi.typography.caption,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = OneUi.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StageCategory.entries.forEach { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) TimelineColors.categoryColor(cat).copy(alpha = 0.2f) else OneUi.colors.surfaceElevated)
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.name.take(4),
                                fontSize = 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) TimelineColors.categoryColor(cat) else OneUi.colors.textSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "STAGE COLOR",
                    style = OneUi.typography.caption,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = OneUi.colors.textSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("green", "cyan", "amber", "pink", "purple").forEach { cKey ->
                        val col = TimelineColors.accentColor(cKey)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = cKey },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == cKey) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (stageToDelete != null) {
        val stage = stageToDelete!!
        OneUiConfirmDialog(
            title = "Delete '${stage.label}'?",
            message = "Any milestones currently in '${stage.label}' will automatically fall back to the built-in ${stage.category.name} stage, and this change will be logged in activity.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                onDeleteCustomStage(stage.id)
                stageToDelete = null
            },
            onDismiss = { stageToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrackerSheet(
    tracker: TimelineTracker,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, priority: TrackerPriority, accent: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(tracker.title) }
    var description by remember { mutableStateOf(tracker.description) }
    var priority by remember { mutableStateOf(tracker.priority) }
    var accent by remember { mutableStateOf(tracker.accent) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    OneUiBottomSheet(
        title = "Edit Tracker",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OneUiTextField(
                value = title,
                onValueChange = { title = it },
                label = "Tracker Title",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OneUiTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PRIORITY",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerPriority.entries.forEach { p ->
                    val selected = p == priority
                    val pColor = TimelineColors.priorityColor(p)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) pColor.copy(alpha = 0.18f) else OneUi.colors.surfaceElevated)
                            .clickable { priority = p }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.name,
                            style = OneUi.typography.caption,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) pColor else OneUi.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ACCENT COLOR",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                color = OneUi.colors.textSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimelineColors.availableAccents.forEach { acc ->
                    val selected = acc == accent
                    val color = TimelineColors.accentColor(acc)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { accent = acc },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OneUiFilledButton(
                text = "Save Changes",
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), priority, accent)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OneUiTonalButton(
                text = "Delete Tracker",
                icon = Icons.Default.Delete,
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        OneUiConfirmDialog(
            title = "Delete Tracker?",
            message = "This will permanently delete '${tracker.title}' and all of its milestones and activity logs.",
            confirmLabel = "Delete Permanently",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
