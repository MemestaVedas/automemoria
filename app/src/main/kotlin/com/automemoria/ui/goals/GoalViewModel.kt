package com.automemoria.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.GoalRepository
import com.automemoria.domain.model.Goal
import com.automemoria.domain.model.GoalMilestone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val milestones: Map<String, List<GoalMilestone>> = emptyMap(),
    val isLoading: Boolean = false,
    val totalProgress: Int = 0
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val repository: GoalRepository
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = repository.observeAll()
        .map { goals ->
            val totalProgress = if (goals.isEmpty()) 0 else goals.sumOf { it.progress } / goals.size
            GoalsUiState(
                goals = goals,
                totalProgress = totalProgress,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState(isLoading = true)
        )

    fun refresh() {
        viewModelScope.launch {
            repository.pullFromSupabase("") // In real app, use last sync
        }
    }
}
