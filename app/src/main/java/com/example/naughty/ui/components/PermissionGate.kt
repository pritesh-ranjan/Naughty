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
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Permissions needed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Naughty needs Usage Access to detect which app you're using, and Notification permission to surface your notes at the right moment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!hasUsageStats) {
                    Button(
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Grant Usage Access", modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Allow Notifications", modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!hasBatteryOptimization) {
                    Button(
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Unrestricted Battery (Recommended)", modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextButton(
                    onClick = { dismissed = true }
                ) {
                    Text(
                        "Skip for now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
