package com.automemoria.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.Habit
import com.automemoria.domain.model.HabitFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateSheet: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepository.observeActiveHabits().collect { habits ->
                _uiState.update { it.copy(habits = habits, isLoading = false) }
            }
        }
    }

    fun showCreateSheet() = _uiState.update { it.copy(showCreateSheet = true) }
    fun hideCreateSheet() = _uiState.update { it.copy(showCreateSheet = false) }

    fun createHabit(
        name: String,
        description: String?,
        icon: String?,
        color: String?,
        frequency: HabitFrequency
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            habitRepository.createHabit(
                name        = name.trim(),
                description = description?.trim(),
                icon        = icon,
                color       = color,
                frequency   = frequency
            )
            hideCreateSheet()
        }
    }

    fun toggleToday(habitId: String) {
        viewModelScope.launch { habitRepository.toggleHabitCompletion(habitId) }
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch { habitRepository.archiveHabit(habitId) }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch { habitRepository.deleteHabit(habitId) }
    }
}
