package com.automemoria.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.GoalRepository
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.Goal
import com.automemoria.domain.model.GoalMilestone
import com.automemoria.domain.model.Habit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalDetailUiState(
    val goal: Goal? = null,
    val milestones: List<GoalMilestone> = emptyList(),
    val linkedHabits: List<Habit> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle["goalId"])

    private val _goal = goalRepository.observeGoal(goalId)
    private val _milestones = goalRepository.observeMilestones(goalId)
    
    // For linked habits, we need to filter habits by IDs in goal.linkedHabitIds
    // This is a bit reactive-heavy, but let's do it.
    private val _habits = habitRepository.observeAllHabits()

    val uiState: StateFlow<GoalDetailUiState> = combine(_goal, _milestones, _habits) { goal, milestones, habits ->
        val linkedHabits = habits.filter { it.id in (goal?.linkedHabitIds ?: emptyList()) }
        GoalDetailUiState(
            goal = goal,
            milestones = milestones,
            linkedHabits = linkedHabits,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalDetailUiState(isLoading = true)
    )

    fun toggleMilestone(milestoneId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            goalRepository.toggleMilestone(milestoneId, isCompleted)
        }
    }
}
