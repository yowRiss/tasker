package com.tasker.android.data.model

data class Project(
    val id: String,
    val name: String,
    val color: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val completed: Boolean = false,
    val position: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String = "open", // open | completed | archived
    val completedAt: String? = null,
    val dueDate: String? = null,
    val priority: Int = 0, // 0..3
    val projectId: String? = null,
    val project: Project? = null,
    val tags: List<Tag> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class TaskFilters(
    val status: String = "open", // open | completed | all
    val projectId: String? = null,
    val priority: Int? = null,
    val query: String = "",
)

data class CreateTaskInput(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val tagIds: List<String> = emptyList(),
    val subtasks: List<String> = emptyList(),
)

data class UpdateTaskInput(
    val title: String? = null,
    val description: String? = null,
    val dueDate: String? = null,
    val priority: Int? = null,
    val projectId: String? = null,
    val tagIds: List<String>? = null,
    val subtasks: List<SubtaskInput>? = null,
)

data class SubtaskInput(
    val id: String? = null,
    val title: String,
    val completed: Boolean = false,
    val position: Int = 0,
)
