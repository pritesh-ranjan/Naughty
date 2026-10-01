package com.example.naughty.ui.binding

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.binding.BindingDetectorService
import com.example.naughty.data.local.AppBinding
import com.example.naughty.data.local.BindingType
import com.example.naughty.data.repository.BindingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class InstalledApp(
    val packageName: String,
    val label: String
)

enum class ExpirationOption {
    INDEFINITE,
    DURATION,
    DATE_PICKER
}

enum class BindingModeOption(val title: String, val subtitle: String) {
    ALWAYS_ACTIVE("Always active", "Binding never expires • Show every time"),
    DURATION("For a set duration", "Expires after days, weeks, or months"),
    DATE_PICKER("Until a specific date", "Expires on a selected date"),
    ONE_SHOT("One-shot", "Fires once on next open, then expires")
}

enum class DurationUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months")
}

data class BindingSheetUiState(
    val bindings: List<AppBinding> = emptyList(),
    val displayedApps: List<InstalledApp> = emptyList(),
    val isShowingOpenApps: Boolean = true,
    val openAppsCount: Int = 0,
    val allAppsCount: Int = 0,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedType: BindingType = BindingType.PERSISTENT,
    val expirationOption: ExpirationOption = ExpirationOption.INDEFINITE,
    val durationNumber: Int = 3,
    val durationUnit: DurationUnit = DurationUnit.DAYS,
    val selectedDateMillis: Long? = null
) {
    val currentMode: BindingModeOption
        get() = if (selectedType == BindingType.ONE_SHOT) {
            BindingModeOption.ONE_SHOT
        } else {
            when (expirationOption) {
                ExpirationOption.INDEFINITE -> BindingModeOption.ALWAYS_ACTIVE
                ExpirationOption.DURATION -> BindingModeOption.DURATION
                ExpirationOption.DATE_PICKER -> BindingModeOption.DATE_PICKER
            }
        }
}

class BindingViewModel(
    private val bindingRepository: BindingRepository,
    private val packageManager: PackageManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BindingSheetUiState())
    val uiState: StateFlow<BindingSheetUiState> = _uiState.asStateFlow()

    private var currentNoteId: String = ""
    private var allApps: List<InstalledApp> = emptyList()
    private var openApps: List<InstalledApp> = emptyList()

    fun loadForNote(noteId: String) {
        currentNoteId = noteId

        // Default date to tomorrow if not set
        if (_uiState.value.selectedDateMillis == null) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            _uiState.value = _uiState.value.copy(selectedDateMillis = cal.timeInMillis)
        }

        // Load apps and open apps on IO
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            withContext(Dispatchers.IO) {
                // 1. Query user-facing launchable apps + user installed apps
                allApps = loadAllUserApps()

                // 2. Query open / running apps using UsageStatsManager and ActivityManager
                openApps = detectOpenAndRunningApps(allApps)
            }

            val query = _uiState.value.searchQuery
            val displayed = if (query.isBlank()) {
                if (openApps.isNotEmpty()) openApps else allApps
            } else {
                allApps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
            }

            _uiState.value = _uiState.value.copy(
                displayedApps = displayed,
                isShowingOpenApps = query.isBlank() && openApps.isNotEmpty(),
                openAppsCount = openApps.size,
                allAppsCount = allApps.size,
                isLoading = false
            )
        }

        // Collect bindings for this note
        viewModelScope.launch {
            bindingRepository.getByNoteId(noteId).collect { bindings ->
                _uiState.value = _uiState.value.copy(bindings = bindings)
            }
        }
    }

    private fun loadAllUserApps(): List<InstalledApp> {
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherActivities = packageManager.queryIntentActivities(launcherIntent, 0)
            val launcherPackages = launcherActivities.map { it.activityInfo.packageName }.toSet()

            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    appInfo.packageName != context.packageName &&
                        (launcherPackages.contains(appInfo.packageName) ||
                            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0))
                }
                .map { appInfo ->
                    InstalledApp(
                        packageName = appInfo.packageName,
                        label = packageManager.getApplicationLabel(appInfo).toString()
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun detectOpenAndRunningApps(allUserApps: List<InstalledApp>): List<InstalledApp> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val now = System.currentTimeMillis()
        val lookback = now - (24 * 60 * 60 * 1000L) // Past 24 hours

        val recentPackageTimes = mutableMapOf<String, Long>()

        // 1. Check UsageEvents for recent foreground / activity resume
        if (usm != null) {
            try {
                val events = usm.queryEvents(lookback, now)
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    // 1 = MOVE_TO_FOREGROUND, 15 = ACTIVITY_RESUMED
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || event.eventType == 15) {
                        if (event.packageName != context.packageName) {
                            recentPackageTimes[event.packageName] = event.timeStamp
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. Check UsageStats lastTimeUsed
            try {
                val statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, lookback, now)
                statsList?.forEach { stat ->
                    if (stat.packageName != context.packageName && stat.lastTimeUsed > 0) {
                        val existing = recentPackageTimes[stat.packageName] ?: 0L
                        if (stat.lastTimeUsed > existing) {
                            recentPackageTimes[stat.packageName] = stat.lastTimeUsed
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Check running app processes from ActivityManager
        try {
            am?.runningAppProcesses?.forEach { proc ->
                proc.pkgList?.forEach { pkg ->
                    if (pkg != context.packageName && !recentPackageTimes.containsKey(pkg)) {
                        recentPackageTimes[pkg] = 1L
                    }
                }
            }
        } catch (_: Exception) {}

        // Match against user apps and order by most recently used first
        val allAppsMap = allUserApps.associateBy { it.packageName }
        return recentPackageTimes.entries
            .mapNotNull { (pkg, timestamp) ->
                allAppsMap[pkg]?.let { app -> app to timestamp }
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun updateSearch(query: String) {
        val displayed = if (query.isBlank()) {
            if (openApps.isNotEmpty()) openApps else allApps
        } else {
            // When user searches, search in whole app list!
            allApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            displayedApps = displayed,
            isShowingOpenApps = query.isBlank() && openApps.isNotEmpty()
        )
    }

    fun setBindingType(type: BindingType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun setExpirationOption(option: ExpirationOption) {
        _uiState.value = _uiState.value.copy(expirationOption = option)
    }

    fun setBindingMode(mode: BindingModeOption) {
        when (mode) {
            BindingModeOption.ALWAYS_ACTIVE -> {
                _uiState.value = _uiState.value.copy(
                    selectedType = BindingType.PERSISTENT,
                    expirationOption = ExpirationOption.INDEFINITE
                )
            }
            BindingModeOption.DURATION -> {
                _uiState.value = _uiState.value.copy(
                    selectedType = BindingType.PERSISTENT,
                    expirationOption = ExpirationOption.DURATION
                )
            }
            BindingModeOption.DATE_PICKER -> {
                _uiState.value = _uiState.value.copy(
                    selectedType = BindingType.PERSISTENT,
                    expirationOption = ExpirationOption.DATE_PICKER
                )
            }
            BindingModeOption.ONE_SHOT -> {
                _uiState.value = _uiState.value.copy(
                    selectedType = BindingType.ONE_SHOT
                )
            }
        }
    }

    fun setDurationNumber(number: Int) {
        _uiState.value = _uiState.value.copy(durationNumber = number.coerceIn(1, 365))
    }

    fun setDurationUnit(unit: DurationUnit) {
        _uiState.value = _uiState.value.copy(durationUnit = unit)
    }

    fun setSelectedDate(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(selectedDateMillis = dateMillis)
    }

    fun addBinding(app: InstalledApp) {
        viewModelScope.launch {
            val expiresAt: Long? = if (_uiState.value.selectedType == BindingType.ONE_SHOT) {
                null
            } else {
                when (_uiState.value.expirationOption) {
                    ExpirationOption.INDEFINITE -> null
                    ExpirationOption.DURATION -> {
                        val cal = Calendar.getInstance()
                        when (_uiState.value.durationUnit) {
                            DurationUnit.DAYS -> cal.add(Calendar.DAY_OF_YEAR, _uiState.value.durationNumber)
                            DurationUnit.WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, _uiState.value.durationNumber)
                            DurationUnit.MONTHS -> cal.add(Calendar.MONTH, _uiState.value.durationNumber)
                        }
                        cal.timeInMillis
                    }
                    ExpirationOption.DATE_PICKER -> {
                        val targetMillis = _uiState.value.selectedDateMillis ?: (System.currentTimeMillis() + 86400000L)
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = targetMillis
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        cal.timeInMillis
                    }
                }
            }

            bindingRepository.createBinding(
                noteId = currentNoteId,
                packageName = app.packageName,
                appLabel = app.label,
                type = _uiState.value.selectedType,
                expiresAt = expiresAt
            )

            // Start foreground detector service
            try {
                BindingDetectorService.start(context)
            } catch (_: Exception) {}
        }
    }

    fun removeBinding(bindingId: String) {
        viewModelScope.launch {
            bindingRepository.removeBinding(bindingId)
        }
    }
}
