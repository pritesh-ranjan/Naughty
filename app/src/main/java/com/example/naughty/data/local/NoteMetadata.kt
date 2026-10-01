package com.example.naughty.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_metadata")
data class NoteMetadata(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val fileName: String,
    @ColumnInfo(defaultValue = "") val tag: String = "",
    @ColumnInfo(defaultValue = "peach") val colorTheme: String = "peach",
    @ColumnInfo(defaultValue = "modular") val cardType: String = "modular",
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val reminderTime: Long? = null
)
