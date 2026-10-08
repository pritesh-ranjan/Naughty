package com.example.naughty.data.local.timeline

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrackerPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class StageCategory {
    NOT_STARTED,
    IN_PROGRESS,
    BLOCKED,
    DONE,
    CANCELLED
}

enum class TimelineUpdateType {
    NOTE,
    STATUS_CHANGE,
    MILESTONE_ADDED,
    MILESTONE_REMOVED
}

@Entity(tableName = "timeline_tracker")
data class TimelineTracker(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    val priority: TrackerPriority = TrackerPriority.MEDIUM,
    @ColumnInfo(defaultValue = "green") val accent: String = "green",
    val createdAt: Long,
    val modifiedAt: Long
)

@Entity(
    tableName = "timeline_stage",
    foreignKeys = [
        ForeignKey(
            entity = TimelineTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class TimelineStage(
    @PrimaryKey val id: String,
    val trackerId: String,
    val label: String,
    val colorKey: String,
    val category: StageCategory,
    val position: Int,
    val isBuiltIn: Boolean
)

@Entity(
    tableName = "timeline_milestone",
    foreignKeys = [
        ForeignKey(
            entity = TimelineTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class TimelineMilestone(
    @PrimaryKey val id: String,
    val trackerId: String,
    val title: String,
    @ColumnInfo(defaultValue = "") val note: String = "",
    val stageId: String,
    val position: Int,
    @ColumnInfo(defaultValue = "NULL") val dueAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val reminderEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val completedAt: Long? = null,
    val createdAt: Long,
    val modifiedAt: Long
)

@Entity(
    tableName = "timeline_update",
    foreignKeys = [
        ForeignKey(
            entity = TimelineTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class TimelineUpdate(
    @PrimaryKey val id: String,
    val trackerId: String,
    @ColumnInfo(defaultValue = "NULL") val milestoneId: String? = null,
    val type: TimelineUpdateType,
    val text: String,
    val createdAt: Long
)
