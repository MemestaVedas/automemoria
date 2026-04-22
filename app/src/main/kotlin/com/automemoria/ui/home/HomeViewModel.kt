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

data class HomeUiState(
    val todayHabits: List<HabitWithTodayStatus> = emptyList(),
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
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayHabits()
    }

    private fun loadTodayHabits() {
        viewModelScope.launch {
            habitRepository.observeActiveHabits()
                .combine(
                    habitRepository.observeCompletedHabitIdsForDate(LocalDate.now())
                ) { habits, completedHabitIds ->
                    habits.map { habit ->
                        HabitWithTodayStatus(
                            habit = habit,
                            completedToday = completedHabitIds.contains(habit.id)
                        )
                    }
                }
                .collect { withStatus ->
                    _uiState.update {
                        it.copy(todayHabits = withStatus, isLoading = false)
                    }
                }
        }
    }

    fun toggleHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId)
        }
    }
}
