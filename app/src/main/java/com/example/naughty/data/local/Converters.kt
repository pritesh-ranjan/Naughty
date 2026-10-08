package com.example.naughty.data.local

import androidx.room.TypeConverter
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TimelineUpdateType
import com.example.naughty.data.local.timeline.TrackerPriority

class Converters {
    @TypeConverter
    fun fromBindingType(value: BindingType): String = value.name

    @TypeConverter
    fun toBindingType(value: String): BindingType = BindingType.valueOf(value)

    @TypeConverter
    fun fromTrackerPriority(value: TrackerPriority): String = value.name

    @TypeConverter
    fun toTrackerPriority(value: String): TrackerPriority = try {
        TrackerPriority.valueOf(value)
    } catch (_: Exception) {
        TrackerPriority.MEDIUM
    }

    @TypeConverter
    fun fromStageCategory(value: StageCategory): String = value.name

    @TypeConverter
    fun toStageCategory(value: String): StageCategory = try {
        StageCategory.valueOf(value)
    } catch (_: Exception) {
        StageCategory.NOT_STARTED
    }

    @TypeConverter
    fun fromTimelineUpdateType(value: TimelineUpdateType): String = value.name

    @TypeConverter
    fun toTimelineUpdateType(value: String): TimelineUpdateType = try {
        TimelineUpdateType.valueOf(value)
    } catch (_: Exception) {
        TimelineUpdateType.NOTE
    }
}
