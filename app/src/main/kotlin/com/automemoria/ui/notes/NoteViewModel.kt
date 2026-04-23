package com.automemoria.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.NoteRepository
import com.automemoria.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NotesUiState> = searchQuery
        .flatMapLatest { query ->
            val source = if (query.isBlank()) {
                repository.observeAll()
            } else {
                repository.search(query.trim())
            }
            source.map { notes ->
                NotesUiState(
                    notes = notes,
                    isLoading = false,
                    searchQuery = query
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesUiState(isLoading = true)
        )

    fun createNote(title: String, content: String = "") {
        viewModelScope.launch {
            repository.save(title = title, content = content)
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }
}
