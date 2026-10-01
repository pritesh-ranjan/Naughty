package com.example.naughty.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteMetadataDao {

    @Query("SELECT * FROM note_metadata WHERE isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun getAllActive(): Flow<List<NoteMetadata>>

    @Query("SELECT * FROM note_metadata WHERE isArchived = 1 AND isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllArchived(): Flow<List<NoteMetadata>>

    @Query("SELECT * FROM note_metadata WHERE isDeleted = 1 ORDER BY modifiedAt DESC")
    fun getAllDeleted(): Flow<List<NoteMetadata>>

    @Query("SELECT * FROM note_metadata WHERE isDeleted = 1 ORDER BY modifiedAt DESC")
    suspend fun getAllDeletedDirect(): List<NoteMetadata>

    @Query("SELECT * FROM note_metadata WHERE isArchived = 1 AND isDeleted = 0 ORDER BY modifiedAt DESC")
    suspend fun getAllArchivedDirect(): List<NoteMetadata>

    @Query("SELECT COUNT(*) FROM note_metadata WHERE isArchived = 0 AND isDeleted = 0")
    fun countActive(): Flow<Int>

    @Query("SELECT COUNT(*) FROM note_metadata WHERE isArchived = 1 AND isDeleted = 0")
    fun countArchived(): Flow<Int>

    @Query("SELECT COUNT(*) FROM note_metadata WHERE isDeleted = 1")
    fun countDeleted(): Flow<Int>

    @Query("SELECT * FROM note_metadata WHERE id = :id")
    suspend fun getById(id: String): NoteMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: NoteMetadata)

    @Update
    suspend fun update(metadata: NoteMetadata)

    @Delete
    suspend fun delete(metadata: NoteMetadata)

    @Query("DELETE FROM note_metadata WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM note_metadata WHERE isDeleted = 1")
    suspend fun deletePermanentlyWhereDeleted()

    @Query("DELETE FROM note_metadata WHERE isArchived = 1")
    suspend fun deletePermanentlyWhereArchived()

    @Query("DELETE FROM note_metadata")
    suspend fun deleteAll()

    @Query("UPDATE note_metadata SET reminderTime = :reminderTime, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun updateReminderTime(id: String, reminderTime: Long?, modifiedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM note_metadata WHERE reminderTime IS NOT NULL AND reminderTime > :now AND isDeleted = 0")
    suspend fun getNotesWithPendingReminders(now: Long = System.currentTimeMillis()): List<NoteMetadata>

    @Query("SELECT COUNT(*) FROM note_metadata WHERE isArchived = 0 AND isDeleted = 0")
    suspend fun count(): Int
}
