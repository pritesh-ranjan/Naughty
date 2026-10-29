package com.example.naughty.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiTextField
import com.example.naughty.ui.oneui.components.OneUiTonalButton
import com.example.naughty.ui.oneui.theme.OneUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderBottomSheet(
    initialTimeMillis: Long? = null,
    targetContextText: String = "",
    onDismiss: () -> Unit,
    onDeleteReminder: (() -> Unit)? = null,
    onSetReminder: (timeMillis: Long, isCalendarExport: Boolean, reminderText: String) -> Unit
) {
    val context = LocalContext.current
    val isEditing = onDeleteReminder != null || (initialTimeMillis != null && initialTimeMillis > 0)
    val now = System.currentTimeMillis()

    var selectedMillis by remember {
        mutableLongStateOf(
            if (initialTimeMillis != null && initialTimeMillis > 0) {
                initialTimeMillis
            } else {
                now + (60 * 60 * 1000)
            }
        )
    }

    val selectedCal = remember(selectedMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedMillis }
    }

    // Android DatePickerDialog
    val datePickerDialog = remember(context, selectedMillis) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedMillis = cal.timeInMillis
            },
            selectedCal.get(Calendar.YEAR),
            selectedCal.get(Calendar.MONTH),
            selectedCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Android TimePickerDialog
    val timePickerDialog = remember(context, selectedMillis) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                selectedMillis = cal.timeInMillis
            },
            selectedCal.get(Calendar.HOUR_OF_DAY),
            selectedCal.get(Calendar.MINUTE),
            false
        )
    }

    var reminderText by remember(targetContextText) {
        mutableStateOf(targetContextText.trim())
    }

    OneUiBottomSheet(
        title = if (isEditing) "Edit Reminder" else "Set Reminder",
        subtitle = "System alert & Calendar sync",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            OneUiTextField(
                value = reminderText,
                onValueChange = { reminderText = it },
                label = "Reminder Text",
                placeholder = "Enter reminder context or task...",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Preset Suggestions
            Text(
                text = "QUICK PRESETS",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = OneUi.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = "⚡ In 1 Hour",
                    onClick = {
                        selectedMillis = System.currentTimeMillis() + (60 * 60 * 1000)
                    }
                )

                PresetChip(
                    label = "🌅 Tomorrow 9 AM",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        selectedMillis = cal.timeInMillis
                    }
                )

                PresetChip(
                    label = "🌙 Tonight 8 PM",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 20)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        selectedMillis = cal.timeInMillis
                    }
                )

                PresetChip(
                    label = "📅 This Weekend",
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                            val daysUntilSat = (Calendar.SATURDAY - dayOfWeek + 7) % 7
                            add(Calendar.DAY_OF_YEAR, if (daysUntilSat == 0) 7 else daysUntilSat)
                            set(Calendar.HOUR_OF_DAY, 10)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        selectedMillis = cal.timeInMillis
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pickers Row (Date Card & Time Card)
            Text(
                text = "DATE & TIME",
                style = OneUi.typography.caption,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = OneUi.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date picker trigger card
                Surface(
                    shape = OneUi.shapes.inner,
                    color = OneUi.colors.surfaceElevated,
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(OneUi.shapes.inner)
                        .clickable { datePickerDialog.show() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OneUi.colors.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = OneUi.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = OneUi.typography.caption,
                                color = OneUi.colors.textSecondary
                            )
                            val dateString = remember(selectedMillis) {
                                SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(selectedMillis))
                            }
                            Text(
                                text = dateString,
                                style = OneUi.typography.body,
                                fontWeight = FontWeight.Bold,
                                color = OneUi.colors.textPrimary
                            )
                        }
                    }
                }

                // Time picker trigger card
                Surface(
                    shape = OneUi.shapes.inner,
                    color = OneUi.colors.surfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .clip(OneUi.shapes.inner)
                        .clickable { timePickerDialog.show() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OneUi.badgePalette.purple.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = OneUi.badgePalette.purple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Time",
                                style = OneUi.typography.caption,
                                color = OneUi.colors.textSecondary
                            )
                            val timeString = remember(selectedMillis) {
                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(selectedMillis))
                            }
                            Text(
                                text = timeString,
                                style = OneUi.typography.body,
                                fontWeight = FontWeight.Bold,
                                color = OneUi.colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OneUiTonalButton(
                    text = "Calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = {
                        onSetReminder(selectedMillis, true, reminderText.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )

                OneUiFilledButton(
                    text = if (isEditing) "Update Alert" else "Set Alert",
                    icon = Icons.Outlined.Notifications,
                    onClick = {
                        onSetReminder(selectedMillis, false, reminderText.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.3f)
                )
            }

            if (onDeleteReminder != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OneUiTonalButton(
                    text = "Remove Reminder",
                    icon = Icons.Outlined.Delete,
                    onClick = {
                        onDeleteReminder()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(OneUi.shapes.pill)
            .background(OneUi.colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = OneUi.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = OneUi.colors.textPrimary
        )
    }
}
