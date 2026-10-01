package com.example.naughty.data.repository

import com.example.naughty.data.local.AppBinding
import com.example.naughty.data.local.AppBindingDao
import com.example.naughty.data.local.BindingType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BindingRepository(
    private val dao: AppBindingDao
) {

    fun getAllActive(): Flow<List<AppBinding>> = dao.getAllActive(System.currentTimeMillis())

    fun getByNoteId(noteId: String): Flow<List<AppBinding>> = dao.getByNoteId(noteId)

    fun getActivePackageNames(): Flow<List<String>> = dao.getActivePackageNames(System.currentTimeMillis())

    fun activeCount(): Flow<Int> = dao.activeCount(System.currentTimeMillis())

    suspend fun getActiveByPackage(packageName: String): List<AppBinding> =
        dao.getActiveByPackage(packageName, System.currentTimeMillis())

    suspend fun createBinding(
        noteId: String,
        packageName: String,
        appLabel: String,
        type: BindingType,
        expiresAt: Long? = null
    ): AppBinding {
        val binding = AppBinding(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            packageName = packageName,
            appLabel = appLabel,
            type = type,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = expiresAt
        )
        dao.insert(binding)
        return binding
    }

    suspend fun deactivateBinding(id: String) {
        dao.deactivate(id)
    }

    suspend fun removeBinding(id: String) {
        dao.deleteById(id)
    }

    suspend fun updateLastTriggered(id: String) {
        dao.updateLastTriggered(id, System.currentTimeMillis())
    }
}
