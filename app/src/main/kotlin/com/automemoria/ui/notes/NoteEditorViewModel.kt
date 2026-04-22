package com.automemoria.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["id"]

    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var tags by mutableStateOf(emptyList<String>())
    var isPinned by mutableStateOf(false)

    init {
        if (noteId != null) {
            viewModelScope.launch {
                repository.observeNote(noteId).collect { note ->
                    if (note != null) {
                        title = note.title
                        content = note.content ?: ""
                        tags = note.tags
                        isPinned = note.isPinned
                    }
                }
            }
        }
    }

    fun saveNote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.save(
                id = noteId,
                title = title,
                content = content,
                tags = tags,
                isPinned = isPinned
            )
            onSuccess()
        }
    }
}
