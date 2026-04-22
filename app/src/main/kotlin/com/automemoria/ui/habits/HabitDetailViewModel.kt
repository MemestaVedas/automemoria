package com.automemoria.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.Habit
import com.automemoria.domain.model.HabitLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitDetailUiState(
    val habit: Habit? = null,
    val logs: List<HabitLog> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val heatmapCounts: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String = savedStateHandle.get<String>("habitId").orEmpty()

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        if (habitId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Missing habit id") }
        } else {
            observeHabit()
        }
    }

    private fun observeHabit() {
        viewModelScope.launch {
            combine(
                habitRepository.observeHabitById(habitId),
                habitRepository.observeHabitLogs(habitId)
            ) { habit, logs ->
                val completedDates = logs
                    .filter { it.completed }
                    .map { it.loggedDate }

                val (current, longest, total) = calculateStreakStats(completedDates)
                val heatmap = completedDates.groupingBy { it }.eachCount()

                HabitDetailUiState(
                    habit = habit,
                    logs = logs,
                    currentStreak = current,
                    longestStreak = longest,
                    totalCompletions = total,
                    heatmapCounts = heatmap,
                    isLoading = false,
                    error = null
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleToday() {
        if (habitId.isBlank()) return
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId)
        }
    }

    fun archiveHabit() {
        if (habitId.isBlank()) return
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId)
        }
    }

    fun deleteHabit() {
        if (habitId.isBlank()) return
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }

    private fun calculateStreakStats(completedDates: List<LocalDate>): Triple<Int, Int, Int> {
        if (completedDates.isEmpty()) return Triple(0, 0, 0)

        val uniqueDates = completedDates.toSet()

        var currentStart = LocalDate.now()
        if (!uniqueDates.contains(currentStart)) {
            currentStart = currentStart.minusDays(1)
        }

        var current = 0
        while (uniqueDates.contains(currentStart)) {
            current += 1
            currentStart = currentStart.minusDays(1)
        }

        val sorted = uniqueDates.sorted()
        var longest = 1
        var running = 1

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val next = sorted[i]
            running = if (next == prev.plusDays(1)) running + 1 else 1
            if (running > longest) longest = running
        }

        return Triple(current, longest, uniqueDates.size)
    }
}
