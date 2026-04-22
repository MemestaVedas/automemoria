package com.automemoria.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.data.repository.CalendarRepository
import com.automemoria.domain.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    val uiState: StateFlow<CalendarUiState> = _selectedDate.flatMapLatest { date ->
        val start = date.atStartOfDay()
        val end = date.atTime(LocalTime.MAX)
        repository.observeInRange(start, end).map { events ->
            CalendarUiState(events = events, isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(isLoading = true)
    )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun createEvent(title: String, startTime: LocalDateTime) {
        viewModelScope.launch {
            repository.create(title = title, startTime = startTime)
        }
    }
}
