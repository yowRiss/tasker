package com.tasker.android.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.Project
import com.tasker.android.data.model.Tag
import com.tasker.android.data.model.Task
import com.tasker.android.data.model.TaskFilters
import com.tasker.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val filters: TaskFilters = TaskFilters(status = "open"),
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val viewPreferences = context.getSharedPreferences(
        "task_view_preferences",
        Context.MODE_PRIVATE,
    )
    private val initialViewMode = TaskViewMode.fromStored(
        viewPreferences.getString("selected_view", null),
    )

    private val _filters = MutableStateFlow(
        TaskFilters(status = defaultTaskStatusFilter(initialViewMode)),
    )
    val filters: StateFlow<TaskFilters> = _filters.asStateFlow()

    private val _viewMode = MutableStateFlow(initialViewMode)
    val viewMode: StateFlow<TaskViewMode> = _viewMode.asStateFlow()
    private var lastNonBoardStatus = defaultTaskStatusFilter(TaskViewMode.LIST)

    val projects: StateFlow<List<Project>> = taskRepository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = taskRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = _filters.flatMapLatest { filterSpec ->
        taskRepository.observeTasks(filterSpec)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStatusFilter(status: String) {
        if (_viewMode.value != TaskViewMode.BOARD) {
            lastNonBoardStatus = status
        }
        _filters.update { it.copy(status = status) }
    }

    fun setProjectFilter(projectId: String?) {
        _filters.update { it.copy(projectId = projectId) }
    }

    fun setPriorityFilter(priority: Int?) {
        _filters.update { it.copy(priority = priority) }
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setViewMode(mode: TaskViewMode) {
        val currentView = _viewMode.value
        if (currentView == mode) return
        if (currentView != TaskViewMode.BOARD) {
            lastNonBoardStatus = _filters.value.status
        }
        val nextStatus = taskStatusFilterAfterViewChange(
            currentView = currentView,
            nextView = mode,
            currentStatus = _filters.value.status,
            lastNonBoardStatus = lastNonBoardStatus,
        )
        _viewMode.value = mode
        _filters.update { it.copy(status = nextStatus) }
        viewPreferences.edit().putString("selected_view", mode.storedValue).apply()
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val nextCompleted = task.status != "completed"
            taskRepository.toggleTaskCompletion(task.id, nextCompleted)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}
