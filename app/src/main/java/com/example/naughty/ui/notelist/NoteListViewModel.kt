package com.example.naughty.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naughty.data.local.NoteMetadata
import com.example.naughty.data.repository.BindingRepository
import com.example.naughty.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map

data class NoteListUiState(
    val notes: List<NoteMetadata> = emptyList(),
    val rawContents: Map<String, String> = emptyMap(),
    val boundApps: Map<String, List<String>> = emptyMap(),
    val searchQuery: String = "",
    val selectedPageIndex: Int = 0, // 0 = All, 1 = Tasks, 2 = Notes, 3 = Pinned
    val isLoading: Boolean = true
)

class NoteListViewModel(
    private val noteRepository: NoteRepository,
    private val bindingRepository: BindingRepository,
    private val timelineRepository: com.example.naughty.data.repository.TimelineRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    val deletedNotes: StateFlow<List<NoteMetadata>> = noteRepository.getAllDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = noteRepository.countActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deletedCount: StateFlow<Int> = noteRepository.countDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trackerCount: StateFlow<Int> = timelineRepository?.countTrackers()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        ?: MutableStateFlow(0)

    val ctsClipCount: StateFlow<Int> = noteRepository.countCtsCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var lastHandledClip: String? = null

    suspend fun isClipRecorded(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank() || trimmed == lastHandledClip) return true
        return noteRepository.isClipAlreadyRecorded(trimmed)
    }

    fun markClipAsHandled(text: String) {
        lastHandledClip = text.trim()
    }

    suspend fun getOrCreateCtsNoteId(): String {
        return noteRepository.getOrCreateDedicatedCtsNote().metadata.id
    }

    suspend fun appendCtsClip(sourceAppName: String, text: String): String {
        val updated = noteRepository.appendCtsCard(sourceAppName, text)
        markClipAsHandled(text)
        return updated.id
    }

    private var allActiveNotes: List<NoteMetadata> = emptyList()
    private var allContents: MutableMap<String, String> = mutableMapOf()

    init {
        viewModelScope.launch {
            noteRepository.adoptExistingTodoNoteIfPresent()
            noteRepository.getOrCreateDedicatedCtsNote()
        }

        viewModelScope.launch {
            noteRepository.getAllActive().collect { notes ->
                allActiveNotes = notes
                val contents = mutableMapOf<String, String>()
                for (note in notes) {
                    val content = noteRepository.getNoteContent(note.id)
                    contents[note.id] = content
                }
                allContents = contents
                applyFilter()
            }
        }

        viewModelScope.launch {
            bindingRepository.getAllActive().collect { bindings ->
                val map = bindings.groupBy({ it.noteId }, { it.appLabel })
                _uiState.value = _uiState.value.copy(boundApps = map)
                applyFilter()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    fun onSelectPage(pageIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedPageIndex = pageIndex)
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val page = _uiState.value.selectedPageIndex
        val boundApps = _uiState.value.boundApps

        val filtered = allActiveNotes.filter { note ->
            // Hide dedicated Circle to Search note from normal note list / home page
            if (note.cardType == "circle_to_search" || note.title.trim().equals("circle to search", ignoreCase = true)) {
                return@filter false
            }

            val content = allContents[note.id] ?: ""
            val apps = boundApps[note.id] ?: emptyList()
            val matchesQuery = query.isEmpty() ||
                    note.title.lowercase().contains(query) ||
                    content.lowercase().contains(query) ||
                    apps.any { it.lowercase().contains(query) }

            val isTodoOrChecklist = note.cardType == "todo" || note.cardType == "checklist" || content.contains("- [")
            val matchesPage = when (page) {
                1 -> isTodoOrChecklist
                2 -> !isTodoOrChecklist
                3 -> note.isPinned
                else -> true // Page 0: All
            }

            matchesQuery && matchesPage
        }

        _uiState.value = _uiState.value.copy(
            notes = filtered,
            rawContents = allContents,
            isLoading = false
        )
    }

    fun createNote(
        title: String = "",
        content: String = "",
        colorTheme: String? = null,
        cardType: String = "modular",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val metadata = noteRepository.createNote(
                title = title,
                content = content,
                colorTheme = colorTheme,
                cardType = cardType
            )
            onCreated(metadata.id)
        }
    }

    fun updateNoteContent(id: String, newContent: String) {
        allContents[id] = newContent
        _uiState.value = _uiState.value.copy(rawContents = HashMap(allContents))
        viewModelScope.launch {
            noteRepository.updateNote(id = id, content = newContent)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(id)
        }
    }

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

    fun togglePin(id: String) {
        viewModelScope.launch {
            noteRepository.togglePin(id)
        }
    }

    fun toggleLock(id: String, isLocked: Boolean) {
        viewModelScope.launch {
            noteRepository.toggleLock(id, isLocked)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            noteRepository.deleteAllNotes()
        }
    }
}
