package com.example.naughty.ui.binding

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.local.BindingType
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiTextField
import com.example.naughty.ui.oneui.theme.OneUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BindingSheet(
    viewModel: BindingViewModel,
    noteId: String,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(noteId) {
        viewModel.loadForNote(noteId)
    }

    val calendar = Calendar.getInstance()
    if (uiState.selectedDateMillis != null) {
        calendar.timeInMillis = uiState.selectedDateMillis!!
    }
    val datePickerDialog = remember(context, uiState.selectedDateMillis) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                viewModel.setSelectedDate(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also {
            it.datePicker.minDate = System.currentTimeMillis()
        }
    }

    OneUiBottomSheet(
        title = "App Context Binding",
        subtitle = "Surface note when launching selected app",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Binding mode selector dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "UNTIL WHEN TO BIND",
                    style = OneUi.typography.caption,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                var showExpirationDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OneUi.shapes.inner)
                            .clickable { showExpirationDropdown = true },
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (uiState.currentMode) {
                                        BindingModeOption.ALWAYS_ACTIVE -> Icons.Outlined.AllInclusive
                                        BindingModeOption.DURATION -> Icons.Outlined.Timer
                                        BindingModeOption.DATE_PICKER -> Icons.Outlined.CalendarMonth
                                        BindingModeOption.ONE_SHOT -> Icons.Outlined.LooksOne
                                    },
                                    contentDescription = null,
                                    tint = OneUi.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Binding mode",
                                        style = OneUi.typography.caption,
                                        color = OneUi.colors.textSecondary
                                    )
                                    Text(
                                        text = when (uiState.currentMode) {
                                            BindingModeOption.ALWAYS_ACTIVE -> "Always active"
                                            BindingModeOption.DURATION -> "For a set duration"
                                            BindingModeOption.DATE_PICKER -> "Until a specific date"
                                            BindingModeOption.ONE_SHOT -> "One-shot (fires once)"
                                        },
                                        style = OneUi.typography.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OneUi.colors.textPrimary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = "Change",
                                tint = OneUi.colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showExpirationDropdown,
                        onDismissRequest = { showExpirationDropdown = false },
                        shape = OneUi.shapes.menu,
                        containerColor = OneUi.colors.surfaceElevated
                    ) {
                        DropdownMenuItem(
                            text = { Text("Always active", color = OneUi.colors.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.AllInclusive, null, tint = OneUi.colors.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                viewModel.setBindingMode(BindingModeOption.ALWAYS_ACTIVE)
                                showExpirationDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("For a set duration", color = OneUi.colors.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Timer, null, tint = OneUi.colors.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                viewModel.setBindingMode(BindingModeOption.DURATION)
                                showExpirationDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Until a specific date", color = OneUi.colors.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = OneUi.colors.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                viewModel.setBindingMode(BindingModeOption.DATE_PICKER)
                                showExpirationDropdown = false
                                datePickerDialog.show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("One-shot (fires once)", color = OneUi.colors.textPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.LooksOne, null, tint = OneUi.colors.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                viewModel.setBindingMode(BindingModeOption.ONE_SHOT)
                                showExpirationDropdown = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration Controls
                if (uiState.currentMode == BindingModeOption.DURATION) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surfaceElevated
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Duration amount:",
                                    style = OneUi.typography.body,
                                    color = OneUi.colors.textPrimary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(OneUi.colors.divider)
                                            .clickable {
                                                if (uiState.durationNumber > 1) {
                                                    viewModel.setDurationNumber(uiState.durationNumber - 1)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Remove,
                                            contentDescription = "Decrease",
                                            modifier = Modifier.size(16.dp),
                                            tint = OneUi.colors.textPrimary
                                        )
                                    }

                                    Text(
                                        text = "${uiState.durationNumber}",
                                        style = OneUi.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUi.colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(OneUi.colors.divider)
                                            .clickable {
                                                viewModel.setDurationNumber(uiState.durationNumber + 1)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "Increase",
                                            modifier = Modifier.size(16.dp),
                                            tint = OneUi.colors.textPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DurationUnit.entries.forEach { unit ->
                                    val isUnitSelected = unit == uiState.durationUnit
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(OneUi.shapes.pill)
                                            .background(
                                                if (isUnitSelected) OneUi.colors.accent
                                                else OneUi.colors.divider
                                            )
                                            .clickable { viewModel.setDurationUnit(unit) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unit.label,
                                            style = OneUi.typography.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isUnitSelected) OneUi.colors.onAccent else OneUi.colors.textPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val expDate = calculateDurationDate(uiState.durationNumber, uiState.durationUnit)
                            Text(
                                text = "Expires in ${uiState.durationNumber} ${uiState.durationUnit.label.lowercase()} • $expDate",
                                style = OneUi.typography.caption,
                                color = OneUi.colors.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Date Picker Controls
                if (uiState.currentMode == BindingModeOption.DATE_PICKER) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OneUi.shapes.inner)
                            .clickable { datePickerDialog.show() },
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Select Date",
                                    tint = OneUi.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bound until date:",
                                        style = OneUi.typography.caption,
                                        color = OneUi.colors.textSecondary
                                    )
                                    Text(
                                        text = uiState.selectedDateMillis?.let { formatDate(it) } ?: "Select date",
                                        style = OneUi.typography.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OneUi.colors.textPrimary
                                    )
                                }
                            }

                            Text(
                                text = "Change",
                                style = OneUi.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = OneUi.colors.accent
                            )
                        }
                    }
                }

                // One-shot hint card
                if (uiState.currentMode == BindingModeOption.ONE_SHOT) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = OneUi.badgePalette.orange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This note will be shown once when you open the target app, and won't appear again.",
                                style = OneUi.typography.caption,
                                color = OneUi.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active bindings
            if (uiState.bindings.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = OneUi.colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVE BINDINGS",
                        style = OneUi.typography.caption,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUi.colors.textSecondary
                    )
                }

                uiState.bindings.forEach { binding ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = OneUi.colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = binding.appLabel,
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUi.colors.textPrimary
                                )
                            }
                            val detail = when (binding.type) {
                                BindingType.PERSISTENT -> {
                                    if (binding.expiresAt != null) {
                                        val isExpired = binding.expiresAt < System.currentTimeMillis()
                                        if (isExpired) {
                                            "Expired on ${formatDate(binding.expiresAt)}"
                                        } else {
                                            "Until ${formatDate(binding.expiresAt)} (${formatRemainingTime(binding.expiresAt)})"
                                        }
                                    } else {
                                        "Persistent • Always active"
                                    }
                                }
                                BindingType.ONE_SHOT -> {
                                    if (binding.isActive) "One-shot • Fires once on next open"
                                    else "One-shot • Triggered"
                                }
                            }
                            Text(
                                text = detail,
                                style = OneUi.typography.caption,
                                color = if (binding.expiresAt != null && binding.expiresAt < System.currentTimeMillis())
                                    OneUi.colors.error
                                else OneUi.colors.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeBinding(binding.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove",
                                tint = OneUi.colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Search Bar
            OneUiTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                placeholder = "Search all installed apps…",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App list section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isShowingOpenApps) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OneUi.badgePalette.green)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (uiState.isShowingOpenApps) "CURRENTLY OPEN & RUNNING APPS"
                        else if (uiState.searchQuery.isNotBlank()) "SEARCH RESULTS (${uiState.displayedApps.size})"
                        else "ALL INSTALLED APPS (${uiState.displayedApps.size})",
                        style = OneUi.typography.caption,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isShowingOpenApps) OneUi.badgePalette.green
                        else OneUi.colors.textSecondary
                    )
                }

                if (uiState.isShowingOpenApps) {
                    Text(
                        text = "Search to view all",
                        style = OneUi.typography.caption,
                        color = OneUi.colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = OneUi.colors.accent
                    )
                }
            } else if (uiState.displayedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isShowingOpenApps)
                            "No recently open apps detected.\nType in the search bar above to select from all apps."
                        else "No apps found matching '${uiState.searchQuery}'",
                        style = OneUi.typography.body,
                        color = OneUi.colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(260.dp)
                ) {
                    items(uiState.displayedApps, key = { it.packageName }) { app ->
                        val alreadyBound = uiState.bindings.any { it.packageName == app.packageName }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(OneUi.shapes.inner)
                                .clickable(enabled = !alreadyBound) { viewModel.addBinding(app) }
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.Medium,
                                    color = if (alreadyBound) OneUi.colors.textTertiary else OneUi.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (alreadyBound) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OneUi.badgePalette.green.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = OneUi.badgePalette.green,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Bound",
                                            style = OneUi.typography.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OneUi.badgePalette.green
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun calculateDurationDate(number: Int, unit: DurationUnit): String {
    val cal = Calendar.getInstance()
    when (unit) {
        DurationUnit.DAYS -> cal.add(Calendar.DAY_OF_YEAR, number)
        DurationUnit.WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, number)
        DurationUnit.MONTHS -> cal.add(Calendar.MONTH, number)
    }
    return formatDate(cal.timeInMillis)
}

private fun formatRemainingTime(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    if (diff <= 0) return "Expired"
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    val hours = ((diff / (1000 * 60 * 60)) % 24).toInt()
    return when {
        days > 30 -> "${days / 30}mo left"
        days > 7 -> "${days / 7}w left"
        days > 0 -> "${days}d left"
        hours > 0 -> "${hours}h left"
        else -> "<1h left"
    }
}
