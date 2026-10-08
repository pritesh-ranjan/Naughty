package com.example.naughty.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TimelineUpdate
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.data.repository.TimelineRepository
import com.example.naughty.notification.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TimelineDetailUiState(
    val tracker: TimelineTracker? = null,
    val milestones: List<TimelineMilestone> = emptyList(),
    val stages: List<TimelineStage> = emptyList(),
    val stageMap: Map<String, TimelineStage> = emptyMap(),
    val updates: List<TimelineUpdate> = emptyList(),
    val progress: TrackerProgress = TrackerProgress(0, 0, 0),
    val derivedStatus: StageCategory = StageCategory.NOT_STARTED,
    val isLoading: Boolean = true
)

class TimelineDetailViewModel(
    val trackerId: String,
    private val repository: TimelineRepository,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineDetailUiState())
    val uiState: StateFlow<TimelineDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeTracker(trackerId),
                repository.observeMilestones(trackerId),
                repository.observeStages(trackerId),
                repository.observeUpdates(trackerId)
            ) { tracker, milestones, stages, updates ->
                val stageMap = stages.associateBy { it.id }
                val progress = TimelineProgress.compute(milestones, stageMap)
                val derived = TimelineProgress.derivedStatus(milestones, stageMap)

                TimelineDetailUiState(
                    tracker = tracker,
                    milestones = milestones,
                    stages = stages,
                    stageMap = stageMap,
                    updates = updates,
                    progress = progress,
                    derivedStatus = derived,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setMilestoneStage(milestoneId: String, newStageId: String) {
        viewModelScope.launch {
            repository.setMilestoneStage(milestoneId, newStageId)
            val stage = _uiState.value.stageMap[newStageId]
            if (stage?.category == StageCategory.DONE || stage?.category == StageCategory.CANCELLED) {
                // Cancel scheduled reminder if milestone is completed or cancelled
                reminderManager.cancelReminder(TimelineProgress.reminderId(milestoneId))
            }
        }
    }

    fun addMilestone(
        title: String,
        note: String = "",
        stageId: String? = null,
        dueAt: Long? = null,
        reminderEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val milestone = repository.addMilestone(
                trackerId = trackerId,
                title = title,
                note = note,
                stageId = stageId,
                dueAt = dueAt,
                reminderEnabled = reminderEnabled
            )

            if (reminderEnabled && dueAt != null && dueAt > System.currentTimeMillis()) {
                val trackerTitle = _uiState.value.tracker?.title ?: "Timeline"
                reminderManager.scheduleReminder(
                    noteId = "",
                    reminderId = TimelineProgress.reminderId(milestone.id),
                    title = "⏱ $trackerTitle: ${milestone.title}",
                    contentSnippet = milestone.note.ifBlank { "Milestone is due." },
                    triggerAtMillis = dueAt,
                    trackerId = trackerId
                )
            }
        }
    }

    fun updateMilestone(
        milestone: TimelineMilestone,
        previousReminderEnabled: Boolean,
        previousDueAt: Long?
    ) {
        viewModelScope.launch {
            repository.updateMilestone(milestone)

            val reminderId = TimelineProgress.reminderId(milestone.id)
            if (milestone.reminderEnabled && milestone.dueAt != null && milestone.dueAt > System.currentTimeMillis()) {
                val trackerTitle = _uiState.value.tracker?.title ?: "Timeline"
                reminderManager.scheduleReminder(
                    noteId = "",
                    reminderId = reminderId,
                    title = "⏱ $trackerTitle: ${milestone.title}",
                    contentSnippet = milestone.note.ifBlank { "Milestone is due." },
                    triggerAtMillis = milestone.dueAt,
                    trackerId = trackerId
                )
            } else if (previousReminderEnabled && !milestone.reminderEnabled) {
                reminderManager.cancelReminder(reminderId)
            } else if (milestone.dueAt == null && previousDueAt != null) {
                reminderManager.cancelReminder(reminderId)
            }
        }
    }

    fun moveMilestone(milestoneId: String, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveMilestone(milestoneId, moveUp)
        }
    }

    fun deleteMilestone(milestoneId: String) {
        viewModelScope.launch {
            reminderManager.cancelReminder(TimelineProgress.reminderId(milestoneId))
            repository.deleteMilestone(milestoneId)
        }
    }

    fun addCustomStage(label: String, colorKey: String, category: StageCategory) {
        viewModelScope.launch {
            repository.addCustomStage(
                trackerId = trackerId,
                label = label,
                colorKey = colorKey,
                category = category
            )
        }
    }

    fun updateStage(stage: TimelineStage) {
        viewModelScope.launch {
            repository.updateStage(stage)
        }
    }

    fun deleteCustomStage(stageId: String) {
        viewModelScope.launch {
            repository.deleteCustomStage(trackerId, stageId)
        }
    }

    fun postUpdate(text: String, milestoneId: String? = null) {
        viewModelScope.launch {
            repository.postUpdate(trackerId, text, milestoneId)
        }
    }

    fun updateTracker(title: String, description: String, priority: TrackerPriority, accent: String) {
        viewModelScope.launch {
            val current = _uiState.value.tracker ?: return@launch
            repository.updateTracker(
                current.copy(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    accent = accent
                )
            )
        }
    }

    fun deleteTracker(onDeleted: () -> Unit) {
        viewModelScope.launch {
            // Cancel any reminders for milestones under this tracker
            _uiState.value.milestones.forEach { m ->
                if (m.reminderEnabled) {
                    reminderManager.cancelReminder(TimelineProgress.reminderId(m.id))
                }
            }
            repository.deleteTracker(trackerId)
            onDeleted()
        }
    }
}
