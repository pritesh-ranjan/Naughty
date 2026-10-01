package com.example.naughty.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BindingType { PERSISTENT, ONE_SHOT }

@Entity(
    tableName = "app_binding",
    foreignKeys = [ForeignKey(
        entity = NoteMetadata::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("noteId"), Index("packageName")]
)
data class AppBinding(
    @PrimaryKey val id: String,
    val noteId: String,
    val packageName: String,
    val appLabel: String,
    val type: BindingType,
    val isActive: Boolean = true,
    val createdAt: Long,
    val lastTriggeredAt: Long? = null,
    val expiresAt: Long? = null
)
