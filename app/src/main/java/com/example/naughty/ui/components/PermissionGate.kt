package com.example.naughty.ui.components

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naughty.ui.oneui.components.OneUiCard
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiIconBadge
import com.example.naughty.ui.oneui.components.OneUiTonalButton
import com.example.naughty.ui.oneui.theme.OneUi

@Suppress("DEPRECATION")
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps?.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    if (mode == AppOpsManager.MODE_ALLOWED) {
        return true
    }

    // Direct functional check fallback: queryUsageStats within the last minute
    return try {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now)
        !stats.isNullOrEmpty()
    } catch (_: Exception) {
        false
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else true
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

fun hasMicrophonePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasCalendarPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED
}

fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        am?.canScheduleExactAlarms() ?: true
    } else true
}

fun requestIgnoreBatteryOptimization(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun PermissionGate(
    bypass: Boolean = false,
    onAllGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsageStats by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(hasNotificationPermission(context)) }
    var hasBatteryOptimization by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var dismissed by remember { mutableStateOf(false) }

    // Re-check permissions every time MainActivity is resumed (e.g. after returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = hasUsageStatsPermission(context)
                hasNotifications = hasNotificationPermission(context)
                hasBatteryOptimization = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifications = granted
    }

    val usageStatsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasUsageStats = hasUsageStatsPermission(context)
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasBatteryOptimization = isBatteryOptimizationIgnored(context)
    }

    if ((hasUsageStats && hasNotifications) || dismissed || bypass) {
        onAllGranted()
    } else {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OneUi.colors.background)
                    .padding(horizontal = OneUi.dimens.horizontalMargin, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OneUiIconBadge(
                    icon = Icons.Outlined.Security,
                    iconTint = OneUi.colors.accent,
                    backgroundColor = OneUi.colors.accent.copy(alpha = 0.16f),
                    size = 56.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Permissions Needed",
                    style = OneUi.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OneUi.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Naughty needs Usage Access to detect which app you're using, and Notification permission to surface your notes at the right moment.",
                    style = OneUi.typography.body,
                    color = OneUi.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                OneUiCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = OneUi.shapes.card,
                    containerColor = OneUi.colors.surfaceElevated
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!hasUsageStats) {
                            OneUiFilledButton(
                                text = "Grant Usage Access",
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        usageStatsLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        usageStatsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OneUiFilledButton(
                                text = "Allow Notifications",
                                onClick = {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!hasBatteryOptimization) {
                            OneUiTonalButton(
                                text = "Unrestricted Battery (Recommended)",
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        batteryLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        try {
                                            batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { dismissed = true }
                ) {
                    Text(
                        "Skip for now",
                        style = OneUi.typography.subhead,
                        color = OneUi.colors.textSecondary
                    )
                }
            }
        }
    }
}
