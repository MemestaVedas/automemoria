package com.automemoria.ui.kanban

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.BoardRepository
import com.automemoria.domain.model.Board
import com.automemoria.domain.model.BoardColumn
import com.automemoria.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardDetailUiState(
    val board: Board? = null,
    val columns: List<BoardColumnWithCards> = emptyList(),
    val isLoading: Boolean = false
)

data class BoardColumnWithCards(
    val column: BoardColumn,
    val cards: List<Card>
)

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val repository: BoardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val boardId: String = checkNotNull(savedStateHandle["boardId"])

    val uiState: StateFlow<BoardDetailUiState> = repository.observeAll().map { boards ->
        boards.find { it.id == boardId }
    }.flatMapLatest { board ->
        if (board == null) return@flatMapLatest flowOf(BoardDetailUiState(isLoading = false))
        
        repository.observeColumns(boardId).flatMapLatest { columns ->
            val columnFlows = columns.map { col ->
                repository.observeCards(col.id).map { cards ->
                    BoardColumnWithCards(col, cards)
                }
            }
            if (columnFlows.isEmpty()) {
                flowOf(BoardDetailUiState(board = board, isLoading = false))
            } else {
                combine(columnFlows) { colsWithCards ->
                    BoardDetailUiState(
                        board = board,
                        columns = colsWithCards.toList().sortedBy { it.column.sortOrder },
                        isLoading = false
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoardDetailUiState(isLoading = true)
    )

    fun addCard(columnId: String, title: String) {
        viewModelScope.launch {
            // TODO: implement card creation in repository
        }
    }
}
