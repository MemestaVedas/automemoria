package com.automemoria.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.Habit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

import com.automemoria.data.repository.GoalRepository
import com.automemoria.data.repository.NoteRepository
import com.automemoria.domain.model.Goal
import com.automemoria.domain.model.Note
import com.automemoria.domain.model.GoalStatus

data class HomeUiState(
    val todayHabits: List<HabitWithTodayStatus> = emptyList(),
    val activeGoals: List<Goal> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val syncStatus: SyncIndicator = SyncIndicator.Idle
)

data class HabitWithTodayStatus(
    val habit: Habit,
    val completedToday: Boolean
)

enum class SyncIndicator { Idle, Syncing, Error }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        habitRepository.observeActiveHabits().combine(
            habitRepository.observeCompletedHabitIdsForDate(LocalDate.now())
        ) { habits, completedIds ->
            habits.map { HabitWithTodayStatus(it, completedIds.contains(it.id)) }
        },
        goalRepository.observeActiveGoals(),
        noteRepository.observeAll().map { it.take(5) }
    ) { habits, goals, notes ->
        HomeUiState(
            todayHabits = habits,
            activeGoals = goals,
            recentNotes = notes,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun toggleHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId)
        }
    }
}
