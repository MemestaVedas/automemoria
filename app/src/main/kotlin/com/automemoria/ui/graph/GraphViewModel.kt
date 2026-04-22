package com.automemoria.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.NoteRepository
import com.automemoria.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class GraphUiState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val isLoading: Boolean = false
)

data class GraphNode(
    val id: String,
    val label: String,
    val type: String // "note", "board", etc.
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<GraphUiState> = combine(
        noteRepository.observeAll(),
        noteRepository.observeAllLinks()
    ) { notes, links ->
        val nodes = notes.map { GraphNode(it.id, it.title, "note") }
        val edges = links.map { GraphEdge(it.sourceNoteId, it.targetNoteId) }
        GraphUiState(nodes = nodes, edges = edges, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GraphUiState(isLoading = true)
    )
}
