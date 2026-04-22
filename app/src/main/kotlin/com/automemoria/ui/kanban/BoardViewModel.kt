package com.automemoria.ui.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.BoardRepository
import com.automemoria.domain.model.Board
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardsUiState(
    val boards: List<Board> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val repository: BoardRepository
) : ViewModel() {

    val uiState: StateFlow<BoardsUiState> = repository.observeAll()
        .map { BoardsUiState(boards = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BoardsUiState(isLoading = true)
        )

    fun createBoard(title: String, description: String = "", icon: String = "view_kanban", color: String = "#7C3AED") {
        viewModelScope.launch {
            repository.create(title, description, icon, color)
        }
    }
}
