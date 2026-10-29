package com.example.naughty.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naughty.ui.components.canScheduleExactAlarms
import com.example.naughty.ui.components.hasCalendarPermission
import com.example.naughty.ui.components.hasContactsPermission
import com.example.naughty.ui.components.hasMicrophonePermission
import com.example.naughty.ui.components.hasNotificationPermission
import com.example.naughty.ui.components.hasUsageStatsPermission
import com.example.naughty.ui.components.isBatteryOptimizationIgnored
import com.example.naughty.ui.components.requestIgnoreBatteryOptimization
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiCardGroup
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiLeading
import com.example.naughty.ui.oneui.components.OneUiListItem
import com.example.naughty.ui.oneui.components.OneUiTrailing
import com.example.naughty.ui.oneui.theme.OneUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAndPrivacyDialog(
    versionName: String,
    versionCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsageStats by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(hasNotificationPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var hasMicrophone by remember { mutableStateOf(hasMicrophonePermission(context)) }
    var canScheduleExactAlarm by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var hasContacts by remember { mutableStateOf(hasContactsPermission(context)) }
    var hasCalendar by remember { mutableStateOf(hasCalendarPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = hasUsageStatsPermission(context)
                hasNotifications = hasNotificationPermission(context)
                hasBatteryExemption = isBatteryOptimizationIgnored(context)
                hasMicrophone = hasMicrophonePermission(context)
                canScheduleExactAlarm = canScheduleExactAlarms(context)
                hasContacts = hasContactsPermission(context)
                hasCalendar = hasCalendarPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    OneUiBottomSheet(
        title = "About Naughty",
        subtitle = "v$versionName (Build $versionCode) • 100% Offline & Private",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Technical Specs Island
            OneUiCardGroup(label = "Technical Architecture") {
                item {
                    OneUiListItem(
                        title = "Offline Engine",
                        subtitle = "100% On-Device • Zero Network Access",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Security, OneUi.colors.badge.green)
                    )
                }
                item {
                    OneUiListItem(
                        title = "Storage",
                        subtitle = "Room SQLite Metadata • Physical Markdown (.md)",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Info, OneUi.colors.badge.teal)
                    )
                }
                item {
                    OneUiListItem(
                        title = "Audio Architecture",
                        subtitle = "Audx RNNoise Neural Suppression • 48 kHz Opus",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Info, OneUi.colors.badge.purple)
                    )
                }
                item {
                    OneUiListItem(
                        title = "On-Device AI",
                        subtitle = "Google ML Kit OCR • Android SpeechRecognizer",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Info, OneUi.colors.badge.blue)
                    )
                }
            }

            // System Permissions Island
            OneUiCardGroup(label = "System Permissions") {
                item {
                    OneUiListItem(
                        title = "Usage Access",
                        subtitle = if (hasUsageStats) "Granted • App binding detection" else "Denied • Required for app-linked notes",
                        subtitleColor = if (hasUsageStats) OneUi.colors.textSecondary else OneUi.colors.warning,
                        trailing = OneUiTrailing.CountBadge(if (hasUsageStats) "Granted" else "Grant", isHighlight = !hasUsageStats),
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Notifications",
                        subtitle = if (hasNotifications) "Granted • Surfacing contextual notes" else "Denied • Cannot post note notifications",
                        subtitleColor = if (hasNotifications) OneUi.colors.textSecondary else OneUi.colors.warning,
                        trailing = OneUiTrailing.CountBadge(if (hasNotifications) "Granted" else "Grant", isHighlight = !hasNotifications),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                })
                            } else {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                })
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Battery Exemption",
                        subtitle = if (hasBatteryExemption) "Active • Unrestricted background detection" else "Optimized • OS may freeze detection",
                        subtitleColor = if (hasBatteryExemption) OneUi.colors.textSecondary else OneUi.colors.warning,
                        trailing = OneUiTrailing.CountBadge(if (hasBatteryExemption) "Active" else "Fix", isHighlight = !hasBatteryExemption),
                        onClick = { requestIgnoreBatteryOptimization(context) }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Exact Alarms",
                        subtitle = if (canScheduleExactAlarm) "Granted • Scheduled reminders" else "Denied • Exact timing limited",
                        subtitleColor = if (canScheduleExactAlarm) OneUi.colors.textSecondary else OneUi.colors.warning,
                        trailing = OneUiTrailing.CountBadge(if (canScheduleExactAlarm) "Granted" else "Grant", isHighlight = !canScheduleExactAlarm),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                })
                            }
                        }
                    )
                }
                item {
                    OneUiListItem(
                        title = "Microphone",
                        subtitle = if (hasMicrophone) "Granted • Voice notes recording" else "Denied • Voice memos unavailable",
                        subtitleColor = if (hasMicrophone) OneUi.colors.textSecondary else OneUi.colors.warning,
                        trailing = OneUiTrailing.CountBadge(if (hasMicrophone) "Granted" else "Grant", isHighlight = !hasMicrophone),
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            })
                        }
                    )
                }
            }

            // License Island
            OneUiCardGroup(label = "Credits & License") {
                item {
                    OneUiListItem(
                        title = "MIT License",
                        subtitle = "Audx (RNNoise) • Google ML Kit • 100% Open Source",
                        leading = OneUiLeading.IconBadge(Icons.Outlined.Code, OneUi.colors.badge.orange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OneUiFilledButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
