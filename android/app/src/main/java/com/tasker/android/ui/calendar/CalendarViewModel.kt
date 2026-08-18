package com.tasker.android.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.TaskFilters
import com.tasker.android.data.repository.NoteRepository
import com.tasker.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class CalendarItem {
    abstract val id: String
    abstract val title: String
    abstract val date: LocalDate
    abstract val displayTime: String

    data class NoteReminderItem(
        override val id: String,
        override val title: String,
        val contentMd: String,
        val noteId: String,
        override val date: LocalDate,
        override val displayTime: String,
    ) : CalendarItem()

    data class TaskItem(
        override val id: String,
        override val title: String,
        val status: String,
        val priority: Int,
        val taskId: String,
        override val date: LocalDate,
        override val displayTime: String,
    ) : CalendarItem()
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isAgendaView: Boolean = false,
    val itemsByDate: Map<LocalDate, List<CalendarItem>> = emptyMap(),
    val selectedDateItems: List<CalendarItem> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                noteRepository.observeNotes(),
                taskRepository.observeTasks(TaskFilters(status = "all"))
            ) { notes, tasks ->
                val map = mutableMapOf<LocalDate, MutableList<CalendarItem>>()
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

                for (note in notes) {
                    val remAt = note.reminderAt ?: continue
                    try {
                        val inst = Instant.parse(remAt)
                        val zdt = inst.atZone(ZoneId.systemDefault())
                        val date = zdt.toLocalDate()
                        val timeStr = timeFormatter.format(zdt)

                        val item = CalendarItem.NoteReminderItem(
                            id = "note_${note.id}",
                            title = note.title,
                            contentMd = note.contentMd,
                            noteId = note.id,
                            date = date,
                            displayTime = timeStr,
                        )
                        map.getOrPut(date) { mutableListOf() }.add(item)
                    } catch (_: Exception) {}
                }

                for (task in tasks) {
                    val dueStr = task.dueDate ?: continue
                    try {
                        val date = LocalDate.parse(dueStr)
                        val item = CalendarItem.TaskItem(
                            id = "task_${task.id}",
                            title = task.title,
                            status = task.status,
                            priority = task.priority,
                            taskId = task.id,
                            date = date,
                            displayTime = "Due Today",
                        )
                        map.getOrPut(date) { mutableListOf() }.add(item)
                    } catch (_: Exception) {}
                }

                map.toMap()
            }.collect { map ->
                _uiState.update { state ->
                    state.copy(
                        itemsByDate = map,
                        selectedDateItems = map[state.selectedDate] ?: emptyList()
                    )
                }
            }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { state ->
            state.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                selectedDateItems = state.itemsByDate[date] ?: emptyList()
            )
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        selectDate(today)
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isAgendaView = !it.isAgendaView) }
    }
}
