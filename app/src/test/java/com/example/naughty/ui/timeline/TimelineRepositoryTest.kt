package com.example.naughty.ui.timeline

import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineDao
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TimelineUpdate
import com.example.naughty.data.local.timeline.TimelineUpdateType
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.data.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTimelineDao : TimelineDao() {
    val trackers = mutableMapOf<String, TimelineTracker>()
    val stages = mutableMapOf<String, TimelineStage>()
    val milestones = mutableMapOf<String, TimelineMilestone>()
    val updates = mutableMapOf<String, TimelineUpdate>()

    override fun observeTrackers(): Flow<List<TimelineTracker>> = flowOf(trackers.values.toList())
    override fun observeTracker(id: String): Flow<TimelineTracker?> = flowOf(trackers[id])
    override suspend fun getTracker(id: String): TimelineTracker? = trackers[id]
    override suspend fun insertTracker(tracker: TimelineTracker) { trackers[tracker.id] = tracker }
    override suspend fun updateTracker(tracker: TimelineTracker) { trackers[tracker.id] = tracker }
    override suspend fun deleteTracker(tracker: TimelineTracker) { trackers.remove(tracker.id) }
    override suspend fun deleteTrackerById(id: String) { trackers.remove(id) }
    override fun countTrackers(): Flow<Int> = flowOf(trackers.size)

    override fun observeMilestones(trackerId: String): Flow<List<TimelineMilestone>> =
        flowOf(milestones.values.filter { it.trackerId == trackerId }.sortedBy { it.position })
    override suspend fun getMilestones(trackerId: String): List<TimelineMilestone> =
        milestones.values.filter { it.trackerId == trackerId }.sortedBy { it.position }
    override fun observeAllMilestones(): Flow<List<TimelineMilestone>> = flowOf(milestones.values.toList())
    override suspend fun getMilestone(id: String): TimelineMilestone? = milestones[id]
    override suspend fun insertMilestone(milestone: TimelineMilestone) { milestones[milestone.id] = milestone }
    override suspend fun updateMilestone(milestone: TimelineMilestone) { milestones[milestone.id] = milestone }
    override suspend fun deleteMilestone(milestone: TimelineMilestone) { milestones.remove(milestone.id) }
    override suspend fun deleteMilestoneById(id: String) { milestones.remove(id) }
    override suspend fun getMilestonesWithPendingReminders(now: Long): List<TimelineMilestone> =
        milestones.values.filter { it.reminderEnabled && it.dueAt != null && it.dueAt > now }

    override fun observeStages(trackerId: String): Flow<List<TimelineStage>> =
        flowOf(stages.values.filter { it.trackerId == trackerId }.sortedBy { it.position })
    override suspend fun getStages(trackerId: String): List<TimelineStage> =
        stages.values.filter { it.trackerId == trackerId }.sortedBy { it.position }
    override fun observeAllStages(): Flow<List<TimelineStage>> = flowOf(stages.values.toList())
    override suspend fun getStage(id: String): TimelineStage? = stages[id]
    override suspend fun insertStage(stage: TimelineStage) { stages[stage.id] = stage }
    override suspend fun insertStages(stages: List<TimelineStage>) { stages.forEach { this.stages[it.id] = it } }
    override suspend fun updateStage(stage: TimelineStage) { stages[stage.id] = stage }
    override suspend fun deleteStage(stage: TimelineStage) { stages.remove(stage.id) }
    override suspend fun deleteStageById(id: String) { stages.remove(id) }

    override fun observeUpdates(trackerId: String): Flow<List<TimelineUpdate>> =
        flowOf(updates.values.filter { it.trackerId == trackerId }.sortedByDescending { it.createdAt })
    override suspend fun insertUpdate(update: TimelineUpdate) { updates[update.id] = update }
    override suspend fun deleteUpdate(update: TimelineUpdate) { updates.remove(update.id) }
    override suspend fun deleteUpdateById(id: String) { updates.remove(id) }

    override suspend fun reassignStage(trackerId: String, fromStageId: String, toStageId: String) {
        milestones.values.filter { it.trackerId == trackerId && it.stageId == fromStageId }.forEach { m ->
            milestones[m.id] = m.copy(stageId = toStageId)
        }
    }
    override suspend fun updateMilestonePosition(id: String, newPosition: Int) {
        val m = milestones[id]
        if (m != null) milestones[id] = m.copy(position = newPosition)
    }

    override suspend fun deleteUpdatesForTracker(trackerId: String) {
        updates.values.filter { it.trackerId == trackerId }.forEach { updates.remove(it.id) }
    }
    override suspend fun deleteMilestonesForTracker(trackerId: String) {
        milestones.values.filter { it.trackerId == trackerId }.forEach { milestones.remove(it.id) }
    }
    override suspend fun deleteStagesForTracker(trackerId: String) {
        stages.values.filter { it.trackerId == trackerId }.forEach { stages.remove(it.id) }
    }
}

class TimelineRepositoryTest {

    private lateinit var dao: FakeTimelineDao
    private lateinit var repository: TimelineRepository

    @Before
    fun setUp() {
        dao = FakeTimelineDao()
        repository = TimelineRepository(dao)
    }

    @Test
    fun testCreateTrackerSeedsDefaultsAndInitialMilestones() = runTest {
        val tracker = repository.createTracker(
            title = "Visa Application",
            description = "Consulate process",
            priority = TrackerPriority.HIGH,
            accent = "cyan",
            initialMilestoneTitles = listOf("Prepare docs", "Biometrics", "Interview")
        )

        assertEquals("Visa Application", tracker.title)
        assertEquals(TrackerPriority.HIGH, tracker.priority)
        assertEquals(1, dao.trackers.size)

        // Stages check: 5 default stages
        assertEquals(5, dao.stages.size)

        // Milestones check: 3 initial milestones
        assertEquals(3, dao.milestones.size)
        val ms = dao.milestones.values.sortedBy { it.position }
        assertEquals("Prepare docs", ms[0].title)
        assertEquals("Biometrics", ms[1].title)
        assertEquals("Interview", ms[2].title)
        assertEquals(0, ms[0].position)
        assertEquals(1, ms[1].position)
        assertEquals(2, ms[2].position)

        // Initial system note logged
        assertEquals(1, dao.updates.size)
        assertEquals(TimelineUpdateType.NOTE, dao.updates.values.first().type)
    }

    @Test
    fun testSetMilestoneStageLogsStatusChangeAndCompletedAt() = runTest {
        val tracker = repository.createTracker(title = "Loan", initialMilestoneTitles = listOf("Check Credit"))
        val milestone = dao.milestones.values.first()
        val doneStage = dao.stages.values.first { it.category == StageCategory.DONE }

        repository.setMilestoneStage(milestone.id, doneStage.id)

        val updated = dao.milestones[milestone.id]
        assertEquals(doneStage.id, updated?.stageId)
        assertNotNull(updated?.completedAt)

        // A STATUS_CHANGE update must be recorded
        val statusUpdates = dao.updates.values.filter { it.type == TimelineUpdateType.STATUS_CHANGE }
        assertEquals(1, statusUpdates.size)
        assertTrue(statusUpdates.first().text.contains("Check Credit"))
        assertTrue(statusUpdates.first().text.contains("Done"))
    }

    @Test
    fun testMoveMilestoneSwapsPositions() = runTest {
        val tracker = repository.createTracker(
            title = "Project",
            initialMilestoneTitles = listOf("Phase 1", "Phase 2")
        )
        val ms = dao.milestones.values.sortedBy { it.position }
        val m1 = ms[0]
        val m2 = ms[1]

        // Move m2 UP
        repository.moveMilestone(m2.id, moveUp = true)

        val updated1 = dao.milestones[m1.id]
        val updated2 = dao.milestones[m2.id]
        assertEquals(1, updated1?.position)
        assertEquals(0, updated2?.position)
    }

    @Test
    fun testDeleteCustomStageFallbackToBuiltInStage() = runTest {
        val tracker = repository.createTracker(title = "Hiring")
        val customStage = repository.addCustomStage(
            trackerId = tracker.id,
            label = "Screening Call",
            colorKey = "purple",
            category = StageCategory.IN_PROGRESS
        )

        // Add milestone assigned to this custom stage
        val milestone = repository.addMilestone(
            trackerId = tracker.id,
            title = "Candidate Interview",
            stageId = customStage.id
        )

        assertEquals(customStage.id, dao.milestones[milestone.id]?.stageId)

        // Delete the custom stage
        repository.deleteCustomStage(tracker.id, customStage.id)

        // Stage is deleted
        assertNull(dao.stages[customStage.id])

        // Milestone reassigned to the built-in stage of the same category (IN_PROGRESS)
        val builtInInProgress = dao.stages.values.first { it.isBuiltIn && it.category == StageCategory.IN_PROGRESS }
        val updatedMilestone = dao.milestones[milestone.id]
        assertEquals(builtInInProgress.id, updatedMilestone?.stageId)

        // Fallback logged in updates
        val fallbackLog = dao.updates.values.firstOrNull { it.text.contains("Screening Call") && it.text.contains("deleted") }
        assertNotNull(fallbackLog)
    }
}
