package com.automemoria.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.GoalRepository
import com.automemoria.domain.model.GoalStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GoalEditorViewModel @Inject constructor(
    private val repository: GoalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String? = savedStateHandle["id"]

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var icon by mutableStateOf("track_changes")
    var color by mutableStateOf("#3B82F6")
    var targetDate by mutableStateOf<LocalDate?>(null)
    
    // Milestones being added/edited (simplified for now)
    var milestones by mutableStateOf<List<String>>(emptyList())

    init {
        if (goalId != null) {
            viewModelScope.launch {
                // Fetch goal if editing
                // For simplicity, we could observe it
            }
        }
    }

    fun addMilestone(title: String) {
        milestones = milestones + title
    }

    fun removeMilestone(index: Int) {
        milestones = milestones.filterIndexed { i, _ -> i != index }
    }

    fun saveGoal(onSuccess: () -> Unit) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val goal = repository.saveGoal(
                id = goalId,
                title = title,
                description = description,
                icon = icon,
                color = color,
                targetDate = targetDate
            )
            
            // Create initial milestones if new goal
            if (goalId == null) {
                milestones.forEach { mTitle ->
                    repository.createMilestone(goal.id, mTitle)
                }
            }
            
            onSuccess()
        }
    }
}
