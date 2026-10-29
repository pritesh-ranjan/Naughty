package com.example.naughty.ui.timeline.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.timeline.TimelineColors
import com.example.naughty.ui.timeline.TimelineProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineStepper(
    milestones: List<TimelineMilestone>,
    stages: Map<String, TimelineStage>,
    onMilestoneClick: (TimelineMilestone) -> Unit,
    onStatusClick: (TimelineMilestone) -> Unit,
    onMoveMilestone: (milestoneId: String, moveUp: Boolean) -> Unit,
    onDeleteMilestone: (TimelineMilestone) -> Unit,
    modifier: Modifier = Modifier
) {
    if (milestones.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No milestones added yet.\nTap '+ Add Milestone' below to get started.",
                style = OneUi.typography.body,
                color = OneUi.colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    val now = System.currentTimeMillis()

    Column(modifier = modifier.fillMaxWidth()) {
        milestones.forEachIndexed { index, milestone ->
            val stage = stages[milestone.stageId]
            val category = stage?.category ?: StageCategory.NOT_STARTED
            val isLast = index == milestones.lastIndex
            val isOverdue = TimelineProgress.isOverdue(milestone, category, now)

            TimelineNodeRow(
                milestone = milestone,
                stage = stage,
                category = category,
                isFirst = index == 0,
                isLast = isLast,
                isOverdue = isOverdue,
                onMilestoneClick = { onMilestoneClick(milestone) },
                onStatusClick = { onStatusClick(milestone) },
                canMoveUp = index > 0,
                canMoveDown = index < milestones.lastIndex,
                onMoveUp = { onMoveMilestone(milestone.id, true) },
                onMoveDown = { onMoveMilestone(milestone.id, false) },
                onDelete = { onDeleteMilestone(milestone) }
            )
        }
    }
}

@Composable
private fun TimelineNodeRow(
    milestone: TimelineMilestone,
    stage: TimelineStage?,
    category: StageCategory,
    isFirst: Boolean,
    isLast: Boolean,
    isOverdue: Boolean,
    onMilestoneClick: () -> Unit,
    onStatusClick: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val nodeColor = if (stage != null) {
        TimelineColors.stageColor(stage.colorKey, category)
    } else {
        TimelineColors.categoryColor(category)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Stepper Line & Node Column
        val defaultLineColor = OneUi.colors.hairline
        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Connecting line
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .padding(top = 18.dp)
                ) {
                    val lineColor = if (category == StageCategory.DONE) {
                        nodeColor.copy(alpha = 0.6f)
                    } else {
                        defaultLineColor
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(x = size.width / 2, y = 0f),
                        end = Offset(x = size.width / 2, y = size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Stepper Icon Node
            TimelineNodeIcon(
                category = category,
                nodeColor = nodeColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Milestone Content Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(OneUi.shapes.inner)
                .clickable(onClick = onMilestoneClick),
            shape = OneUi.shapes.inner,
            color = OneUi.colors.surfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = milestone.title.ifBlank { "Untitled Milestone" },
                        style = OneUi.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (category == StageCategory.CANCELLED) {
                            OneUi.colors.textTertiary
                        } else {
                            OneUi.colors.textPrimary
                        },
                        textDecoration = if (category == StageCategory.CANCELLED) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Options menu button
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = OneUi.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = OneUi.shapes.menu,
                            containerColor = OneUi.colors.surfaceElevated
                        ) {
                            if (canMoveUp) {
                                DropdownMenuItem(
                                    text = { Text("Move Up", color = OneUi.colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = OneUi.colors.textPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveUp()
                                    }
                                )
                            }
                            if (canMoveDown) {
                                DropdownMenuItem(
                                    text = { Text("Move Down", color = OneUi.colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = OneUi.colors.textPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveDown()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit Details", color = OneUi.colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = OneUi.colors.textPrimary, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMilestoneClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = OneUi.colors.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = OneUi.colors.error, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                if (milestone.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = milestone.note,
                        style = OneUi.typography.caption,
                        color = OneUi.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom row: Status chip (clickable to change) + Due date pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable Stage Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(nodeColor.copy(alpha = 0.16f))
                            .clickable(onClick = onStatusClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stage?.label ?: category.name,
                            style = OneUi.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = nodeColor,
                            fontSize = 11.sp
                        )
                    }

                    // Due date display
                    if (milestone.dueAt != null) {
                        val formattedDate = remember(milestone.dueAt) {
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(milestone.dueAt))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            if (milestone.reminderEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Reminder active",
                                    tint = if (isOverdue) OneUi.colors.error else OneUi.colors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = if (isOverdue) "Overdue • $formattedDate" else "Due $formattedDate",
                                style = OneUi.typography.caption,
                                color = if (isOverdue) OneUi.colors.error else OneUi.colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeIcon(
    category: StageCategory,
    nodeColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val notStartedColor = OneUi.colors.hairline
    when (category) {
        StageCategory.DONE -> {
            Surface(
                shape = CircleShape,
                color = nodeColor,
                modifier = modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        StageCategory.IN_PROGRESS -> {
            Box(
                modifier = modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glowing outer ring
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(
                        color = nodeColor.copy(alpha = pulseAlpha * 0.35f),
                        radius = size.minDimension / 2
                    )
                }
                // Inner ring
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(
                        color = nodeColor,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                // Core center dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(nodeColor)
                )
            }
        }
        StageCategory.BLOCKED -> {
            Surface(
                shape = CircleShape,
                color = nodeColor,
                modifier = modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Blocked",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        StageCategory.CANCELLED -> {
            Surface(
                shape = CircleShape,
                color = OneUi.colors.hairline,
                modifier = modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelled",
                        tint = OneUi.colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        StageCategory.NOT_STARTED -> {
            Canvas(modifier = modifier.size(20.dp)) {
                drawCircle(
                    color = notStartedColor,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
