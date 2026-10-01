package com.example.naughty.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBindingDao {

    @Query("SELECT * FROM app_binding WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :now) ORDER BY createdAt DESC")
    fun getAllActive(now: Long): Flow<List<AppBinding>>

    @Query("SELECT * FROM app_binding WHERE packageName = :packageName AND isActive = 1 AND (expiresAt IS NULL OR expiresAt > :now)")
    suspend fun getActiveByPackage(packageName: String, now: Long): List<AppBinding>

    @Query("SELECT * FROM app_binding WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getByNoteId(noteId: String): Flow<List<AppBinding>>

    @Query("SELECT DISTINCT packageName FROM app_binding WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :now)")
    fun getActivePackageNames(now: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(binding: AppBinding)

    @Update
    suspend fun update(binding: AppBinding)

    @Delete
    suspend fun delete(binding: AppBinding)

    @Query("UPDATE app_binding SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("UPDATE app_binding SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: String, timestamp: Long)

    @Query("DELETE FROM app_binding WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM app_binding WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :now)")
    fun activeCount(now: Long): Flow<Int>
}
