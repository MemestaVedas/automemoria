package com.automemoria.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.Habit
import com.automemoria.domain.model.HabitLog
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
                    // We need today's logs — observe them reactively
                    // For simplicity in Phase 0 we derive from the habit logs flow per habit
                    // This will be refined in Phase 1
                    flowOf(emptyList<HabitLog>())
                ) { habits, _ ->
                    habits
                }
                .collect { habits ->
                    // For each habit, check if completed today
                    val today = LocalDate.now().toString()
                    val withStatus = habits.map { habit ->
                        HabitWithTodayStatus(
                            habit = habit,
                            completedToday = false // refined in Phase 1 with per-habit log query
                        )
                    }
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
