package com.automemoria.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.HabitRepository
import com.automemoria.domain.model.HabitFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitEditorUiState(
    val habitId: String? = null,
    val name: String = "",
    val description: String = "",
    val icon: String = "check_circle",
    val color: String = "#7C3AED",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetStreak: String = "0",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeHabitId = savedStateHandle.get<String>("id")

    private val _uiState = MutableStateFlow(HabitEditorUiState())
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    init {
        if (routeHabitId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, isEditMode = false) }
        } else {
            _uiState.update { it.copy(habitId = routeHabitId, isEditMode = true, isLoading = true) }
            observeExistingHabit(routeHabitId)
        }
    }

    private fun observeExistingHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.observeHabitById(habitId).collect { habit ->
                if (habit == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Habit not found") }
                } else {
                    _uiState.update {
                        it.copy(
                            habitId = habit.id,
                            name = habit.name,
                            description = habit.description.orEmpty(),
                            icon = habit.icon ?: "check_circle",
                            color = habit.color ?: "#7C3AED",
                            frequency = habit.frequency,
                            targetStreak = habit.targetStreak.toString(),
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onIconChange(value: String) = _uiState.update { it.copy(icon = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(color = value) }
    fun onFrequencyChange(value: HabitFrequency) = _uiState.update { it.copy(frequency = value) }
    fun onTargetStreakChange(value: String) = _uiState.update { it.copy(targetStreak = value.filter { c -> c.isDigit() }) }

    fun save() {
        val current = _uiState.value
        if (current.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val target = current.targetStreak.toIntOrNull() ?: 0
                if (current.isEditMode && !current.habitId.isNullOrBlank()) {
                    habitRepository.updateHabit(
                        habitId = current.habitId,
                        name = current.name.trim(),
                        description = current.description.trim().ifBlank { null },
                        icon = current.icon,
                        color = current.color,
                        frequency = current.frequency,
                        targetStreak = target
                    )
                } else {
                    habitRepository.createHabit(
                        name = current.name.trim(),
                        description = current.description.trim().ifBlank { null },
                        icon = current.icon,
                        color = current.color,
                        frequency = current.frequency,
                        targetStreak = target
                    )
                }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") }
            }
        }
    }
}
