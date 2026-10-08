package com.example.naughty.data.local.timeline

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TimelineDao {

    // --- Trackers ---

    @Query("SELECT * FROM timeline_tracker ORDER BY modifiedAt DESC")
    abstract fun observeTrackers(): Flow<List<TimelineTracker>>

    @Query("SELECT * FROM timeline_tracker WHERE id = :id")
    abstract fun observeTracker(id: String): Flow<TimelineTracker?>

    @Query("SELECT * FROM timeline_tracker WHERE id = :id")
    abstract suspend fun getTracker(id: String): TimelineTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTracker(tracker: TimelineTracker)

    @Update
    abstract suspend fun updateTracker(tracker: TimelineTracker)

    @Delete
    abstract suspend fun deleteTracker(tracker: TimelineTracker)

    @Query("DELETE FROM timeline_tracker WHERE id = :id")
    abstract suspend fun deleteTrackerById(id: String)

    @Query("SELECT COUNT(*) FROM timeline_tracker")
    abstract fun countTrackers(): Flow<Int>

    // --- Milestones ---

    @Query("SELECT * FROM timeline_milestone WHERE trackerId = :trackerId ORDER BY position ASC, createdAt ASC")
    abstract fun observeMilestones(trackerId: String): Flow<List<TimelineMilestone>>

    @Query("SELECT * FROM timeline_milestone WHERE trackerId = :trackerId ORDER BY position ASC, createdAt ASC")
    abstract suspend fun getMilestones(trackerId: String): List<TimelineMilestone>

    @Query("SELECT * FROM timeline_milestone ORDER BY position ASC")
    abstract fun observeAllMilestones(): Flow<List<TimelineMilestone>>

    @Query("SELECT * FROM timeline_milestone WHERE id = :id")
    abstract suspend fun getMilestone(id: String): TimelineMilestone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMilestone(milestone: TimelineMilestone)

    @Update
    abstract suspend fun updateMilestone(milestone: TimelineMilestone)

    @Delete
    abstract suspend fun deleteMilestone(milestone: TimelineMilestone)

    @Query("DELETE FROM timeline_milestone WHERE id = :id")
    abstract suspend fun deleteMilestoneById(id: String)

    @Query("SELECT * FROM timeline_milestone WHERE reminderEnabled = 1 AND dueAt IS NOT NULL AND dueAt > :now")
    abstract suspend fun getMilestonesWithPendingReminders(now: Long): List<TimelineMilestone>

    // --- Stages ---

    @Query("SELECT * FROM timeline_stage WHERE trackerId = :trackerId ORDER BY position ASC")
    abstract fun observeStages(trackerId: String): Flow<List<TimelineStage>>

    @Query("SELECT * FROM timeline_stage WHERE trackerId = :trackerId ORDER BY position ASC")
    abstract suspend fun getStages(trackerId: String): List<TimelineStage>

    @Query("SELECT * FROM timeline_stage")
    abstract fun observeAllStages(): Flow<List<TimelineStage>>

    @Query("SELECT * FROM timeline_stage WHERE id = :id")
    abstract suspend fun getStage(id: String): TimelineStage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStage(stage: TimelineStage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStages(stages: List<TimelineStage>)

    @Update
    abstract suspend fun updateStage(stage: TimelineStage)

    @Delete
    abstract suspend fun deleteStage(stage: TimelineStage)

    @Query("DELETE FROM timeline_stage WHERE id = :id")
    abstract suspend fun deleteStageById(id: String)

    // --- Activity / Updates ---

    @Query("SELECT * FROM timeline_update WHERE trackerId = :trackerId ORDER BY createdAt DESC")
    abstract fun observeUpdates(trackerId: String): Flow<List<TimelineUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUpdate(update: TimelineUpdate)

    @Delete
    abstract suspend fun deleteUpdate(update: TimelineUpdate)

    @Query("DELETE FROM timeline_update WHERE id = :id")
    abstract suspend fun deleteUpdateById(id: String)

    // --- Transactions ---

    @Query("UPDATE timeline_milestone SET stageId = :toStageId WHERE trackerId = :trackerId AND stageId = :fromStageId")
    abstract suspend fun reassignStage(trackerId: String, fromStageId: String, toStageId: String)

    @Query("UPDATE timeline_milestone SET position = :newPosition WHERE id = :id")
    abstract suspend fun updateMilestonePosition(id: String, newPosition: Int)

    @Transaction
    open suspend fun swapMilestones(
        milestoneAId: String,
        positionA: Int,
        milestoneBId: String,
        positionB: Int
    ) {
        updateMilestonePosition(milestoneAId, positionB)
        updateMilestonePosition(milestoneBId, positionA)
    }

    @Transaction
    open suspend fun deleteTrackerCascade(trackerId: String) {
        // Cascade delete child entities explicitly
        deleteUpdatesForTracker(trackerId)
        deleteMilestonesForTracker(trackerId)
        deleteStagesForTracker(trackerId)
        deleteTrackerById(trackerId)
    }

    @Query("DELETE FROM timeline_update WHERE trackerId = :trackerId")
    abstract suspend fun deleteUpdatesForTracker(trackerId: String)

    @Query("DELETE FROM timeline_milestone WHERE trackerId = :trackerId")
    abstract suspend fun deleteMilestonesForTracker(trackerId: String)

    @Query("DELETE FROM timeline_stage WHERE trackerId = :trackerId")
    abstract suspend fun deleteStagesForTracker(trackerId: String)
}
