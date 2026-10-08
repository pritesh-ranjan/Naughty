package com.example.naughty.ui.timeline

import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineProgressTest {

    private val stages = listOf(
        TimelineStage("s_not_started", "t1", "Not started", "gray", StageCategory.NOT_STARTED, 0, true),
        TimelineStage("s_in_progress", "t1", "In progress", "cyan", StageCategory.IN_PROGRESS, 1, true),
        TimelineStage("s_blocked", "t1", "Blocked", "pink", StageCategory.BLOCKED, 2, true),
        TimelineStage("s_done", "t1", "Done", "green", StageCategory.DONE, 3, true),
        TimelineStage("s_cancelled", "t1", "Cancelled", "muted", StageCategory.CANCELLED, 4, true),
        TimelineStage("s_custom_bank", "t1", "Awaiting Bank", "purple", StageCategory.BLOCKED, 5, false)
    ).associateBy { it.id }

    @Test
    fun testEmptyMilestones() {
        val progress = TimelineProgress.compute(emptyList(), stages)
        assertEquals(0, progress.done)
        assertEquals(0, progress.countable)
        assertEquals(0, progress.total)
        assertEquals(0f, progress.fraction, 0.001f)
        assertEquals(0, progress.percent)

        assertEquals(StageCategory.NOT_STARTED, TimelineProgress.derivedStatus(emptyList(), stages))
        assertNull(TimelineProgress.nextUp(emptyList(), stages))
    }

    @Test
    fun testProgressExcludesCancelledMilestones() {
        val milestones = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_done", 0, null, false, 100L, 0L, 0L),
            TimelineMilestone("m2", "t1", "Step 2", "", "s_done", 1, null, false, 100L, 0L, 0L),
            TimelineMilestone("m3", "t1", "Step 3", "", "s_in_progress", 2, null, false, null, 0L, 0L),
            TimelineMilestone("m4", "t1", "Step 4", "", "s_blocked", 3, null, false, null, 0L, 0L),
            TimelineMilestone("m5", "t1", "Step 5", "", "s_cancelled", 4, null, false, null, 0L, 0L)
        )

        val progress = TimelineProgress.compute(milestones, stages)
        assertEquals(5, progress.total)
        assertEquals(4, progress.countable) // Cancelled is subtracted
        assertEquals(2, progress.done)
        assertEquals(0.5f, progress.fraction, 0.001f)
        assertEquals(50, progress.percent)
    }

    @Test
    fun testDerivedStatusPrecedence() {
        // 1. All done -> DONE
        val allDone = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_done", 0, null, false, 100L, 0L, 0L),
            TimelineMilestone("m2", "t1", "Step 2", "", "s_cancelled", 1, null, false, null, 0L, 0L) // Cancelled doesn't block Done
        )
        assertEquals(StageCategory.DONE, TimelineProgress.derivedStatus(allDone, stages))

        // 2. Any blocked (even custom stage) -> BLOCKED
        val blocked = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_done", 0, null, false, 100L, 0L, 0L),
            TimelineMilestone("m2", "t1", "Step 2", "", "s_custom_bank", 1, null, false, null, 0L, 0L),
            TimelineMilestone("m3", "t1", "Step 3", "", "s_in_progress", 2, null, false, null, 0L, 0L)
        )
        assertEquals(StageCategory.BLOCKED, TimelineProgress.derivedStatus(blocked, stages))

        // 3. Partially done / in progress -> IN_PROGRESS
        val inProgress = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_done", 0, null, false, 100L, 0L, 0L),
            TimelineMilestone("m2", "t1", "Step 2", "", "s_not_started", 1, null, false, null, 0L, 0L)
        )
        assertEquals(StageCategory.IN_PROGRESS, TimelineProgress.derivedStatus(inProgress, stages))

        // 4. All cancelled -> CANCELLED
        val cancelled = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_cancelled", 0, null, false, null, 0L, 0L)
        )
        assertEquals(StageCategory.CANCELLED, TimelineProgress.derivedStatus(cancelled, stages))

        // 5. All not started -> NOT_STARTED
        val notStarted = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_not_started", 0, null, false, null, 0L, 0L)
        )
        assertEquals(StageCategory.NOT_STARTED, TimelineProgress.derivedStatus(notStarted, stages))
    }

    @Test
    fun testNextUpMilestone() {
        val milestones = listOf(
            TimelineMilestone("m1", "t1", "Step 1", "", "s_done", 0, null, false, 100L, 0L, 0L),
            TimelineMilestone("m2", "t1", "Step 2", "", "s_cancelled", 1, null, false, null, 0L, 0L),
            TimelineMilestone("m3", "t1", "Step 3 (Next)", "", "s_in_progress", 2, null, false, null, 0L, 0L),
            TimelineMilestone("m4", "t1", "Step 4", "", "s_not_started", 3, null, false, null, 0L, 0L)
        )

        val next = TimelineProgress.nextUp(milestones, stages)
        assertNotNull(next)
        assertEquals("m3", next?.id)
        assertEquals("Step 3 (Next)", next?.title)
    }

    @Test
    fun testIsOverdueLogic() {
        val past = System.currentTimeMillis() - 100_000L
        val future = System.currentTimeMillis() + 100_000L

        val overdueMilestone = TimelineMilestone("m1", "t1", "Step", "", "s_in_progress", 0, past, true, null, 0L, 0L)
        assertTrue(TimelineProgress.isOverdue(overdueMilestone, StageCategory.IN_PROGRESS))

        // When milestone is done, it is NEVER overdue
        assertFalse(TimelineProgress.isOverdue(overdueMilestone, StageCategory.DONE))

        // When milestone is cancelled, it is NEVER overdue
        assertFalse(TimelineProgress.isOverdue(overdueMilestone, StageCategory.CANCELLED))

        // Future due date is not overdue
        val futureMilestone = TimelineMilestone("m2", "t1", "Step", "", "s_in_progress", 0, future, true, null, 0L, 0L)
        assertFalse(TimelineProgress.isOverdue(futureMilestone, StageCategory.IN_PROGRESS))
    }

    @Test
    fun testDefaultStagesCreation() {
        val defaults = TimelineProgress.defaultStages("tracker_xyz")
        assertEquals(5, defaults.size)
        assertEquals(StageCategory.NOT_STARTED, defaults[0].category)
        assertEquals(StageCategory.IN_PROGRESS, defaults[1].category)
        assertEquals(StageCategory.BLOCKED, defaults[2].category)
        assertEquals(StageCategory.DONE, defaults[3].category)
        assertEquals(StageCategory.CANCELLED, defaults[4].category)
        assertTrue(defaults.all { it.isBuiltIn })
        assertTrue(defaults.all { it.trackerId == "tracker_xyz" })
    }

    @Test
    fun testReminderIdIsDeterministicAndUnique() {
        val id1 = TimelineProgress.reminderId("ms-123")
        val id2 = TimelineProgress.reminderId("ms-123")
        val id3 = TimelineProgress.reminderId("ms-456")

        assertEquals(id1, id2)
        assertTrue(id1 != id3)
        // Ensure prefix namespace "timeline:..." doesn't directly collide with bare "ms-123"
        assertTrue(id1 != "ms-123".hashCode())
    }
}
