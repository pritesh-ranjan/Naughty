package com.example.naughty.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.data.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TrackerSummary(
    val tracker: TimelineTracker,
    val progress: TrackerProgress,
    val derivedStatus: StageCategory,
    val nextUpMilestone: TimelineMilestone?,
    val overdueCount: Int
)

data class TimelineListUiState(
    val trackers: List<TrackerSummary> = emptyList(),
    val activeCount: Int = 0,
    val blockedCount: Int = 0,
    val doneCount: Int = 0,
    val isLoading: Boolean = true
)

class TimelineListViewModel(
    private val repository: TimelineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineListUiState())
    val uiState: StateFlow<TimelineListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeTrackers(),
                repository.observeAllMilestones(),
                repository.observeAllStages()
            ) { trackers, allMilestones, allStages ->
                val stageMap = allStages.associateBy { it.id }
                val milestonesByTracker = allMilestones.groupBy { it.trackerId }
                val now = System.currentTimeMillis()

                val summaries = trackers.map { tracker ->
                    val milestones = milestonesByTracker[tracker.id] ?: emptyList()
                    val progress = TimelineProgress.compute(milestones, stageMap)
                    val derived = TimelineProgress.derivedStatus(milestones, stageMap)
                    val nextUp = TimelineProgress.nextUp(milestones, stageMap)
                    val overdue = milestones.count { m ->
                        val cat = stageMap[m.stageId]?.category ?: StageCategory.NOT_STARTED
                        TimelineProgress.isOverdue(m, cat, now)
                    }

                    TrackerSummary(
                        tracker = tracker,
                        progress = progress,
                        derivedStatus = derived,
                        nextUpMilestone = nextUp,
                        overdueCount = overdue
                    )
                }

                val active = summaries.count { it.derivedStatus != StageCategory.DONE && it.derivedStatus != StageCategory.CANCELLED }
                val blocked = summaries.count { it.derivedStatus == StageCategory.BLOCKED }
                val done = summaries.count { it.derivedStatus == StageCategory.DONE }

                TimelineListUiState(
                    trackers = summaries,
                    activeCount = active,
                    blockedCount = blocked,
                    doneCount = done,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createTracker(
        title: String,
        description: String = "",
        priority: TrackerPriority = TrackerPriority.MEDIUM,
        accent: String = "green",
        initialMilestones: List<String> = emptyList(),
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val tracker = repository.createTracker(
                title = title,
                description = description,
                priority = priority,
                accent = accent,
                initialMilestoneTitles = initialMilestones
            )
            onCreated(tracker.id)
        }
    }

    fun deleteTracker(id: String) {
        viewModelScope.launch {
            repository.deleteTracker(id)
        }
    }
}
