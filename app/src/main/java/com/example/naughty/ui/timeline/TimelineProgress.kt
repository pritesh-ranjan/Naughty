package com.example.naughty.ui.timeline

import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage

data class TrackerProgress(
    val done: Int,
    val countable: Int,
    val total: Int
) {
    val fraction: Float
        get() = if (countable == 0) 0f else (done.toFloat() / countable.toFloat()).coerceIn(0f, 1f)

    val percent: Int
        get() = (fraction * 100).toInt()
}

object TimelineProgress {

    fun compute(
        milestones: List<TimelineMilestone>,
        stages: Map<String, TimelineStage>
    ): TrackerProgress {
        val total = milestones.size
        if (total == 0) return TrackerProgress(0, 0, 0)

        var doneCount = 0
        var cancelledCount = 0

        for (m in milestones) {
            val stage = stages[m.stageId]
            val category = stage?.category ?: StageCategory.NOT_STARTED
            when (category) {
                StageCategory.DONE -> doneCount++
                StageCategory.CANCELLED -> cancelledCount++
                else -> {}
            }
        }

        val countable = (total - cancelledCount).coerceAtLeast(0)
        return TrackerProgress(
            done = doneCount,
            countable = countable,
            total = total
        )
    }

    /**
     * Precedence:
     * 1. DONE if countable > 0 and done == countable
     * 2. BLOCKED if any non-cancelled milestone is BLOCKED
     * 3. IN_PROGRESS if any non-cancelled milestone is IN_PROGRESS or DONE (partially done)
     * 4. CANCELLED if all milestones are CANCELLED (and total > 0)
     * 5. Otherwise NOT_STARTED
     */
    fun derivedStatus(
        milestones: List<TimelineMilestone>,
        stages: Map<String, TimelineStage>
    ): StageCategory {
        if (milestones.isEmpty()) return StageCategory.NOT_STARTED

        val activeCategories = milestones.map { stages[it.stageId]?.category ?: StageCategory.NOT_STARTED }
        val nonCancelled = activeCategories.filter { it != StageCategory.CANCELLED }

        if (nonCancelled.isEmpty()) {
            return StageCategory.CANCELLED
        }

        val allDone = nonCancelled.all { it == StageCategory.DONE }
        if (allDone) return StageCategory.DONE

        val hasBlocked = nonCancelled.any { it == StageCategory.BLOCKED }
        if (hasBlocked) return StageCategory.BLOCKED

        val hasProgressOrDone = nonCancelled.any { it == StageCategory.IN_PROGRESS || it == StageCategory.DONE }
        if (hasProgressOrDone) return StageCategory.IN_PROGRESS

        return StageCategory.NOT_STARTED
    }

    /**
     * Next up milestone: the first milestone (by position order) that is NOT DONE and NOT CANCELLED
     */
    fun nextUp(
        milestones: List<TimelineMilestone>,
        stages: Map<String, TimelineStage>
    ): TimelineMilestone? {
        val sorted = milestones.sortedBy { it.position }
        return sorted.firstOrNull {
            val cat = stages[it.stageId]?.category ?: StageCategory.NOT_STARTED
            cat != StageCategory.DONE && cat != StageCategory.CANCELLED
        }
    }

    fun isOverdue(
        milestone: TimelineMilestone,
        category: StageCategory,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (category == StageCategory.DONE || category == StageCategory.CANCELLED) return false
        val due = milestone.dueAt ?: return false
        return due < now
    }

    fun defaultStages(trackerId: String): List<TimelineStage> {
        return listOf(
            TimelineStage(
                id = "${trackerId}_stage_not_started",
                trackerId = trackerId,
                label = "Not started",
                colorKey = "gray",
                category = StageCategory.NOT_STARTED,
                position = 0,
                isBuiltIn = true
            ),
            TimelineStage(
                id = "${trackerId}_stage_in_progress",
                trackerId = trackerId,
                label = "In progress",
                colorKey = "cyan",
                category = StageCategory.IN_PROGRESS,
                position = 1,
                isBuiltIn = true
            ),
            TimelineStage(
                id = "${trackerId}_stage_blocked",
                trackerId = trackerId,
                label = "Blocked",
                colorKey = "pink",
                category = StageCategory.BLOCKED,
                position = 2,
                isBuiltIn = true
            ),
            TimelineStage(
                id = "${trackerId}_stage_done",
                trackerId = trackerId,
                label = "Done",
                colorKey = "green",
                category = StageCategory.DONE,
                position = 3,
                isBuiltIn = true
            ),
            TimelineStage(
                id = "${trackerId}_stage_cancelled",
                trackerId = trackerId,
                label = "Cancelled",
                colorKey = "muted",
                category = StageCategory.CANCELLED,
                position = 4,
                isBuiltIn = true
            )
        )
    }

    fun reminderId(milestoneId: String): Int {
        return "timeline:$milestoneId".hashCode()
    }
}
