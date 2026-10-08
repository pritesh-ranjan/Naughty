package com.example.naughty.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.naughty.data.local.timeline.TimelineDao
import com.example.naughty.data.local.timeline.TimelineMilestone
import com.example.naughty.data.local.timeline.TimelineStage
import com.example.naughty.data.local.timeline.TimelineTracker
import com.example.naughty.data.local.timeline.TimelineUpdate

@Database(
    entities = [
        NoteMetadata::class,
        AppBinding::class,
        TimelineTracker::class,
        TimelineStage::class,
        TimelineMilestone::class,
        TimelineUpdate::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteMetadataDao(): NoteMetadataDao
    abstract fun appBindingDao(): AppBindingDao
    abstract fun timelineDao(): TimelineDao
}
