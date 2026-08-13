package com.tasker.android.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.CreateTaskInput
import com.tasker.android.data.model.Project
import com.tasker.android.data.model.SubtaskInput
import com.tasker.android.data.model.Tag
import com.tasker.android.data.model.UpdateTaskInput
import com.tasker.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubtaskItem(
    val id: String? = null,
    val title: String,
    val completed: Boolean = false
)

data class TaskDetailUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val dueDate: String? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val selectedTagIds: List<String> = emptyList(),
    val subtasks: List<SubtaskItem> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    val projects: StateFlow<List<Project>> = taskRepository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = taskRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(taskId: String?) {
        if (taskId == null || taskId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, taskId = taskId) }
            val task = taskRepository.getTask(taskId)
            if (task != null) {
                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description ?: "",
                        dueDate = task.dueDate,
                        priority = task.priority,
                        projectId = task.projectId,
                        selectedTagIds = task.tags.map { tag -> tag.id },
                        subtasks = task.subtasks.map { st -> SubtaskItem(st.id, st.title, st.completed) },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Task not found") }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDueDateChange(value: String?) = _uiState.update { it.copy(dueDate = value) }
    fun onPriorityChange(value: Int) = _uiState.update { it.copy(priority = value) }
    fun onProjectChange(projectId: String?) = _uiState.update { it.copy(projectId = projectId) }

    fun toggleTagSelection(tagId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedTagIds.contains(tagId)) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = updated)
        }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        _uiState.update { state ->
            state.copy(subtasks = state.subtasks + SubtaskItem(title = title.trim()))
        }
    }

    fun removeSubtask(index: Int) {
        _uiState.update { state ->
            val updated = state.subtasks.toMutableList().apply { removeAt(index) }
            state.copy(subtasks = updated)
        }
    }

    fun toggleSubtask(index: Int) {
        _uiState.update { state ->
            val updated = state.subtasks.toMutableList().apply {
                this[index] = this[index].copy(completed = !this[index].completed)
            }
            state.copy(subtasks = updated)
        }
    }

    fun createProject(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val project = taskRepository.createProject(name)
            onProjectChange(project.id)
        }
    }

    fun createTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val tag = taskRepository.createTag(name)
            toggleTagSelection(tag.id)
        }
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentTaskId = state.taskId
            if (currentTaskId != null) {
                taskRepository.updateTask(
                    id = currentTaskId,
                    input = UpdateTaskInput(
                        title = state.title.trim(),
                        description = state.description.ifBlank { null },
                        dueDate = state.dueDate,
                        priority = state.priority,
                        projectId = state.projectId,
                        tagIds = state.selectedTagIds,
                        subtasks = state.subtasks.mapIndexed { idx, st ->
                            SubtaskInput(
                                id = st.id,
                                title = st.title,
                                completed = st.completed,
                                position = idx,
                            )
                        }
                    )
                )
            } else {
                taskRepository.createTask(
                    CreateTaskInput(
                        title = state.title.trim(),
                        description = state.description.ifBlank { null },
                        dueDate = state.dueDate,
                        priority = state.priority,
                        projectId = state.projectId,
                        tagIds = state.selectedTagIds,
                        subtasks = state.subtasks.map { it.title }
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    fun deleteTask() {
        val taskId = _uiState.value.taskId ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
