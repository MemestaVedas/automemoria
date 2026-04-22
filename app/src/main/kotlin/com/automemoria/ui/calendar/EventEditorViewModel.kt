package com.automemoria.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EventEditorViewModel @Inject constructor(
    private val repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String? = savedStateHandle["id"]

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf("")
    var startTime by mutableStateOf(LocalDateTime.now().withMinute(0).withSecond(0))
    var endTime by mutableStateOf(LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0))
    var color by mutableStateOf("#3B82F6")
    var isAllDay by mutableStateOf(false)

    init {
        if (eventId != null) {
            viewModelScope.launch {
                // Fetch event if editing
            }
        }
    }

    fun saveEvent(onSuccess: () -> Unit) {
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.create(
                title = title,
                description = description,
                location = location,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                color = color
            )
            onSuccess()
        }
    }
}
