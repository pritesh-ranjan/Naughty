package com.example.naughty.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NoteListKey : NavKey

@Serializable
data class NoteEditorKey(val noteId: String) : NavKey

@Serializable
data object TimelineListKey : NavKey

@Serializable
data class TimelineDetailKey(val trackerId: String) : NavKey
