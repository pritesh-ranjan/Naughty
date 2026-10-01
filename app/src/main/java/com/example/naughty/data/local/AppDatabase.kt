package com.example.naughty.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [NoteMetadata::class, AppBinding::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteMetadataDao(): NoteMetadataDao
    abstract fun appBindingDao(): AppBindingDao
}
