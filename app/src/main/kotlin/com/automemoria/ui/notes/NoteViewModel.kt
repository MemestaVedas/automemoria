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
    val isLoading: Boolean = false
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<NotesUiState> = repository.observeAll()
        .map { NotesUiState(notes = it, isLoading = false) }
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
}
