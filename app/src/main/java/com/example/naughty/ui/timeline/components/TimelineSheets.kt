package com.example.naughty.ui.timeline.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.isAppInDarkTheme
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TrackerPriority.MEDIUM) }
    var accent by remember { mutableStateOf("green") }
    val initialMilestones = remember { mutableStateListOf("", "") }
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "New Timeline Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) CrystalWhite else AmoledBlack
            )
            Text(
                text = "Track important milestones and progress over time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tracker Title") },
                placeholder = { Text("e.g. Home Loan Approval, Passport Renewal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Details or reference numbers...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(
                text = "PRIORITY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { priority = p },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) pColor.copy(alpha = 0.18f) else if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) pColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accent Swatches
            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                tint = AmoledBlack,
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = { initialMilestones.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row", fontSize = 12.sp)
                }
            }

            initialMilestones.forEachIndexed { idx, msTitle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = msTitle,
                        onValueChange = { initialMilestones[idx] = it },
                        placeholder = { Text("Step ${idx + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                            unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                        )
                    )
                    if (initialMilestones.size > 1) {
                        IconButton(onClick = { initialMilestones.removeAt(idx) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TimelineColors.accentColor(accent), contentColor = AmoledBlack)
            ) {
                Text("Create Tracker", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(milestone.title) }
    var note by remember { mutableStateOf(milestone.note) }
    var stageId by remember { mutableStateOf(milestone.stageId) }
    var dueAt by remember { mutableStateOf(milestone.dueAt) }
    var reminderEnabled by remember { mutableStateOf(milestone.reminderEnabled) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Milestone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CrystalWhite else AmoledBlack
                )

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Milestone Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes / Extra Details") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stage Selection
            Text(
                text = "STAGE STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (stages.size <= 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stages.forEach { stage ->
                        val selected = stage.id == stageId
                        val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { stageId = stage.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) stageCol.copy(alpha = 0.2f) else if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                                    .padding(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stage.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) stageCol else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stages.forEach { stage ->
                        val selected = stage.id == stageId
                        val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)
                        Surface(
                            modifier = Modifier
                                .width(74.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { stageId = stage.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) stageCol.copy(alpha = 0.2f) else if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                                    .padding(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stage.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) stageCol else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Due Date & Time Picker
            Text(
                text = "DUE DATE & REMINDER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF0E0E12) else Color(0xFFF4F4F6),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (dueAt != null) {
                                        SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(dueAt!!))
                                    } else {
                                        "No due date set"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (dueAt != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row {
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
                                        // Then open time picker
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
                                Text(if (dueAt == null) "Set" else "Change", fontSize = 12.sp)
                            }

                            if (dueAt != null) {
                                TextButton(onClick = {
                                    dueAt = null
                                    reminderEnabled = false
                                }) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                                    tint = if (reminderEnabled) ElectricGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alarm notification on due time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmoledBlack,
                                    checkedTrackColor = ElectricGreen
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Milestone?") },
            text = { Text("Are you sure you want to remove '${milestone.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(milestone.id)
                        onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    milestone: TimelineMilestone,
    stages: List<TimelineStage>,
    onDismiss: () -> Unit,
    onSelectStage: (stageId: String) -> Unit,
    onManageStagesClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Update Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CrystalWhite else AmoledBlack
                )

                TextButton(onClick = {
                    onDismiss()
                    onManageStagesClick()
                }) {
                    Text("Manage Stages", fontSize = 12.sp)
                }
            }

            Text(
                text = "Milestone: ${milestone.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stages, key = { it.id }) { stage ->
                    val isSelected = stage.id == milestone.stageId
                    val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectStage(stage.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) stageCol.copy(alpha = 0.16f) else if (isDark) Color(0xFF101014) else Color(0xFFF4F4F6)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) stageCol else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (stage.isBuiltIn) "Standard (${stage.category.name})" else "Custom stage (${stage.category.name})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }
    var stageToDelete by remember { mutableStateOf<TimelineStage?>(null) }
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stages & Statuses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CrystalWhite else AmoledBlack
                )

                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Stage", fontSize = 12.sp)
                }
            }

            Text(
                text = "Add custom workflow stages. Built-in stages cannot be deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stages, key = { it.id }) { stage ->
                    val stageCol = TimelineColors.stageColor(stage.colorKey, stage.category)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (stage.isBuiltIn) "Built-in" else "Custom • Category: ${stage.category.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("cyan") }
        var selectedCategory by remember { mutableStateOf(StageCategory.IN_PROGRESS) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Stage") },
            text = {
                Column {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Stage Label") },
                        placeholder = { Text("e.g. Awaiting Bank, In Review") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4),
                            unfocusedContainerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF2F2F4)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "BASE CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StageCategory.entries.forEach { cat ->
                            val isSel = cat == selectedCategory
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedCategory = cat },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) TimelineColors.categoryColor(cat).copy(alpha = 0.2f) else if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat.name.take(4),
                                        fontSize = 9.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) TimelineColors.categoryColor(cat) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "STAGE COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

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
                                        tint = AmoledBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (label.isNotBlank()) {
                            onAddCustomStage(label.trim(), selectedColor, selectedCategory)
                            showAddDialog = false
                        }
                    },
                    enabled = label.isNotBlank()
                ) {
                    Text("Add Stage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (stageToDelete != null) {
        val stage = stageToDelete!!
        AlertDialog(
            onDismissRequest = { stageToDelete = null },
            title = { Text("Delete '${stage.label}'?") },
            text = {
                Text("Any milestones currently in '${stage.label}' will automatically fall back to the built-in ${stage.category.name} stage, and this change will be logged in activity.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomStage(stage.id)
                        stageToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stageToDelete = null }) {
                    Text("Cancel")
                }
            }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(tracker.title) }
    var description by remember { mutableStateOf(tracker.description) }
    var priority by remember { mutableStateOf(tracker.priority) }
    var accent by remember { mutableStateOf(tracker.accent) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) CrystalWhite else AmoledBlack
                )

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Tracker",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tracker Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4),
                    unfocusedContainerColor = if (isDark) Color(0xFF141418) else Color(0xFFF2F2F4)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PRIORITY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { priority = p },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) pColor.copy(alpha = 0.18f) else if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) pColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                tint = AmoledBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), priority, accent)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Tracker?") },
            text = { Text("This will permanently delete '${tracker.title}' and all of its milestones and activity logs.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
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
