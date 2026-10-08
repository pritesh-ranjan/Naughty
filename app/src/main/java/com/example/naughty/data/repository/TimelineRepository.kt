package com.example.naughty.data.repository

import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineDao
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TimelineUpdate
import com.example.naughty.data.local.timeline.TimelineUpdateType
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.ui.timeline.TimelineProgress
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TimelineRepository(
    private val dao: TimelineDao
) {

    // --- Trackers ---

    fun observeTrackers(): Flow<List<TimelineTracker>> = dao.observeTrackers()

    fun observeTracker(id: String): Flow<TimelineTracker?> = dao.observeTracker(id)

    suspend fun getTracker(id: String): TimelineTracker? = dao.getTracker(id)

    fun countTrackers(): Flow<Int> = dao.countTrackers()

    suspend fun createTracker(
        title: String,
        description: String = "",
        priority: TrackerPriority = TrackerPriority.MEDIUM,
        accent: String = "green",
        initialMilestoneTitles: List<String> = emptyList()
    ): TimelineTracker {
        val now = System.currentTimeMillis()
        val trackerId = UUID.randomUUID().toString()
        val tracker = TimelineTracker(
            id = trackerId,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            accent = accent,
            createdAt = now,
            modifiedAt = now
        )

        dao.insertTracker(tracker)

        // Seed 5 default stages
        val defaults = TimelineProgress.defaultStages(trackerId)
        dao.insertStages(defaults)

        val notStartedStage = defaults.first { it.category == StageCategory.NOT_STARTED }

        // Insert initial milestones if provided
        initialMilestoneTitles.filter { it.isNotBlank() }.forEachIndexed { index, milestoneTitle ->
            val milestoneId = UUID.randomUUID().toString()
            val milestone = TimelineMilestone(
                id = milestoneId,
                trackerId = trackerId,
                title = milestoneTitle.trim(),
                note = "",
                stageId = notStartedStage.id,
                position = index,
                dueAt = null,
                reminderEnabled = false,
                completedAt = null,
                createdAt = now + index,
                modifiedAt = now + index
            )
            dao.insertMilestone(milestone)
        }

        // Initial system note
        val initialNote = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            milestoneId = null,
            type = TimelineUpdateType.NOTE,
            text = "Tracker created with ${defaults.size} stages",
            createdAt = now
        )
        dao.insertUpdate(initialNote)

        return tracker
    }

    suspend fun updateTracker(tracker: TimelineTracker) {
        dao.updateTracker(tracker.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTracker(trackerId: String) {
        dao.deleteTrackerCascade(trackerId)
    }

    // --- Milestones ---

    fun observeMilestones(trackerId: String): Flow<List<TimelineMilestone>> = dao.observeMilestones(trackerId)

    suspend fun getMilestones(trackerId: String): List<TimelineMilestone> = dao.getMilestones(trackerId)

    fun observeAllMilestones(): Flow<List<TimelineMilestone>> = dao.observeAllMilestones()

    suspend fun getMilestone(id: String): TimelineMilestone? = dao.getMilestone(id)

    suspend fun addMilestone(
        trackerId: String,
        title: String,
        note: String = "",
        stageId: String? = null,
        dueAt: Long? = null,
        reminderEnabled: Boolean = false
    ): TimelineMilestone {
        val now = System.currentTimeMillis()
        val currentMilestones = dao.getMilestones(trackerId)
        val nextPos = if (currentMilestones.isEmpty()) 0 else (currentMilestones.maxOf { it.position } + 1)

        val targetStageId = if (stageId != null) {
            stageId
        } else {
            val stages = dao.getStages(trackerId)
            stages.firstOrNull { it.category == StageCategory.NOT_STARTED }?.id
                ?: stages.firstOrNull()?.id
                ?: "${trackerId}_stage_not_started"
        }

        val milestone = TimelineMilestone(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            title = title.trim(),
            note = note.trim(),
            stageId = targetStageId,
            position = nextPos,
            dueAt = dueAt,
            reminderEnabled = reminderEnabled,
            completedAt = null,
            createdAt = now,
            modifiedAt = now
        )

        dao.insertMilestone(milestone)

        // Log activity
        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            milestoneId = milestone.id,
            type = TimelineUpdateType.MILESTONE_ADDED,
            text = "Added milestone: '${milestone.title}'",
            createdAt = now
        )
        dao.insertUpdate(update)

        touchTracker(trackerId)
        return milestone
    }

    suspend fun updateMilestone(milestone: TimelineMilestone) {
        val now = System.currentTimeMillis()
        val oldMilestone = dao.getMilestone(milestone.id)
        if (oldMilestone != null && oldMilestone.stageId != milestone.stageId) {
            // Stage was also changed
            val oldStage = dao.getStage(oldMilestone.stageId)
            val newStage = dao.getStage(milestone.stageId)
            val completedTime = if (newStage?.category == StageCategory.DONE) {
                oldMilestone.completedAt ?: now
            } else {
                null
            }

            val updated = milestone.copy(
                completedAt = completedTime,
                modifiedAt = now
            )
            dao.updateMilestone(updated)

            val oldLabel = oldStage?.label ?: "Unknown"
            val newLabel = newStage?.label ?: "Unknown"
            val update = TimelineUpdate(
                id = UUID.randomUUID().toString(),
                trackerId = milestone.trackerId,
                milestoneId = milestone.id,
                type = TimelineUpdateType.STATUS_CHANGE,
                text = "${milestone.title}: $oldLabel → $newLabel",
                createdAt = now
            )
            dao.insertUpdate(update)
        } else {
            dao.updateMilestone(milestone.copy(modifiedAt = now))
        }
        touchTracker(milestone.trackerId)
    }

    suspend fun setMilestoneStage(milestoneId: String, newStageId: String) {
        val milestone = dao.getMilestone(milestoneId) ?: return
        if (milestone.stageId == newStageId) return

        val now = System.currentTimeMillis()
        val oldStage = dao.getStage(milestone.stageId)
        val newStage = dao.getStage(newStageId)

        val completedTime = if (newStage?.category == StageCategory.DONE) {
            milestone.completedAt ?: now
        } else {
            null
        }

        val updatedMilestone = milestone.copy(
            stageId = newStageId,
            completedAt = completedTime,
            modifiedAt = now
        )
        dao.updateMilestone(updatedMilestone)

        val oldLabel = oldStage?.label ?: "Unknown"
        val newLabel = newStage?.label ?: "Unknown"
        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = milestone.trackerId,
            milestoneId = milestone.id,
            type = TimelineUpdateType.STATUS_CHANGE,
            text = "${milestone.title}: $oldLabel → $newLabel",
            createdAt = now
        )
        dao.insertUpdate(update)
        touchTracker(milestone.trackerId)
    }

    suspend fun moveMilestone(milestoneId: String, moveUp: Boolean) {
        val milestone = dao.getMilestone(milestoneId) ?: return
        val all = dao.getMilestones(milestone.trackerId).sortedBy { it.position }
        val index = all.indexOfFirst { it.id == milestoneId }
        if (index == -1) return

        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex in all.indices) {
            val other = all[targetIndex]
            dao.swapMilestones(
                milestoneAId = milestone.id,
                positionA = milestone.position,
                milestoneBId = other.id,
                positionB = other.position
            )
            touchTracker(milestone.trackerId)
        }
    }

    suspend fun deleteMilestone(milestoneId: String) {
        val milestone = dao.getMilestone(milestoneId) ?: return
        val now = System.currentTimeMillis()
        dao.deleteMilestoneById(milestoneId)

        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = milestone.trackerId,
            milestoneId = null,
            type = TimelineUpdateType.MILESTONE_REMOVED,
            text = "Removed milestone: '${milestone.title}'",
            createdAt = now
        )
        dao.insertUpdate(update)
        touchTracker(milestone.trackerId)
    }

    suspend fun getMilestonesWithPendingReminders(now: Long): List<TimelineMilestone> {
        return dao.getMilestonesWithPendingReminders(now)
    }

    // --- Stages ---

    fun observeStages(trackerId: String): Flow<List<TimelineStage>> = dao.observeStages(trackerId)

    suspend fun getStages(trackerId: String): List<TimelineStage> = dao.getStages(trackerId)

    fun observeAllStages(): Flow<List<TimelineStage>> = dao.observeAllStages()

    suspend fun addCustomStage(
        trackerId: String,
        label: String,
        colorKey: String,
        category: StageCategory
    ): TimelineStage {
        val existing = dao.getStages(trackerId)
        val nextPos = if (existing.isEmpty()) 0 else (existing.maxOf { it.position } + 1)
        val stage = TimelineStage(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            label = label.trim(),
            colorKey = colorKey,
            category = category,
            position = nextPos,
            isBuiltIn = false
        )
        dao.insertStage(stage)

        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            milestoneId = null,
            type = TimelineUpdateType.NOTE,
            text = "Created stage: '$label' (${category.name})",
            createdAt = System.currentTimeMillis()
        )
        dao.insertUpdate(update)
        touchTracker(trackerId)
        return stage
    }

    suspend fun updateStage(stage: TimelineStage) {
        dao.updateStage(stage)
        touchTracker(stage.trackerId)
    }

    /**
     * Fallback milestones to the built-in stage of the same category,
     * log the activity transition, then delete the custom stage.
     */
    suspend fun deleteCustomStage(trackerId: String, stageId: String) {
        val stageToDelete = dao.getStage(stageId) ?: return
        if (stageToDelete.isBuiltIn) return // Built-ins cannot be deleted

        val allStages = dao.getStages(trackerId)
        val fallbackStage = allStages.firstOrNull { it.isBuiltIn && it.category == stageToDelete.category }
            ?: allStages.firstOrNull { it.isBuiltIn }

        if (fallbackStage != null) {
            dao.reassignStage(
                trackerId = trackerId,
                fromStageId = stageId,
                toStageId = fallbackStage.id
            )
        }

        dao.deleteStageById(stageId)

        val now = System.currentTimeMillis()
        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            milestoneId = null,
            type = TimelineUpdateType.STATUS_CHANGE,
            text = "Stage '${stageToDelete.label}' deleted; affected milestones moved to '${fallbackStage?.label ?: "Default"}'",
            createdAt = now
        )
        dao.insertUpdate(update)
        touchTracker(trackerId)
    }

    // --- Updates / Activity Log ---

    fun observeUpdates(trackerId: String): Flow<List<TimelineUpdate>> = dao.observeUpdates(trackerId)

    suspend fun postUpdate(
        trackerId: String,
        text: String,
        milestoneId: String? = null
    ): TimelineUpdate {
        val now = System.currentTimeMillis()
        val update = TimelineUpdate(
            id = UUID.randomUUID().toString(),
            trackerId = trackerId,
            milestoneId = milestoneId,
            type = TimelineUpdateType.NOTE,
            text = text.trim(),
            createdAt = now
        )
        dao.insertUpdate(update)
        touchTracker(trackerId)
        return update
    }

    suspend fun deleteUpdate(updateId: String) {
        dao.deleteUpdateById(updateId)
    }

    private suspend fun touchTracker(trackerId: String) {
        val tracker = dao.getTracker(trackerId) ?: return
        dao.updateTracker(tracker.copy(modifiedAt = System.currentTimeMillis()))
    }
}
