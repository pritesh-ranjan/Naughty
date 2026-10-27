package com.example.naughty.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.repository.BindingRepository
import com.example.naughty.data.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val noteRepository: NoteRepository,
    private val bindingRepository: BindingRepository
) : ViewModel() {

    val deletedNotes: StateFlow<List<NoteMetadata>> = noteRepository.getAllDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedCount: StateFlow<Int> = noteRepository.countDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeCount: StateFlow<Int> = noteRepository.countActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val boundAppsCount: StateFlow<Int> = bindingRepository.activeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun restoreNote(id: String) {
        viewModelScope.launch {
            noteRepository.restoreNote(id)
        }
    }

    fun permanentlyDeleteNote(id: String) {
        viewModelScope.launch {
            noteRepository.permanentlyDeleteNote(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.permanentlyDeleteAllDeleted()
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            noteRepository.deleteAllNotes()
        }
    }

    fun purgeDeletedAndBlankNotes(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = noteRepository.purgeDeletedAndBlankNotes()
            onComplete(count)
        }
    }
}

