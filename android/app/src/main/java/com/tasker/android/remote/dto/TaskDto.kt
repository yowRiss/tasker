package com.tasker.android.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("project_id")
    val projectId: String? = null,
    val status: String = "open",
    @SerialName("completed_at")
    val completedAt: String? = null,
    val priority: Int = 0,
    val tags: List<TagDto>? = null,
    val subtasks: List<SubtaskDto>? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class SubtaskDto(
    val id: String,
    @SerialName("task_id")
    val taskId: String,
    val title: String,
    val completed: Boolean = false,
    val position: Int = 0,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val color: String? = null,
    @SerialName("is_archived")
    val isArchived: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val color: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class TaskCreateRequest(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    val priority: Int = 0,
    @SerialName("project_id")
    val projectId: String? = null,
    @SerialName("tag_ids")
    val tagIds: List<String> = emptyList(),
    val subtasks: List<SubtaskCreateItem> = emptyList(),
)

@Serializable
data class SubtaskCreateItem(
    val title: String,
    val completed: Boolean = false,
    val position: Int = 0,
)

@Serializable
data class TaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    val priority: Int? = null,
    @SerialName("project_id")
    val projectId: String? = null,
    val status: String? = null,
    @SerialName("tag_ids")
    val tagIds: List<String>? = null,
)

@Serializable
data class CompletionRequest(
    val completed: Boolean,
)

@Serializable
data class ProjectCreateRequest(
    val name: String,
    val color: String? = null,
)

@Serializable
data class TagCreateRequest(
    val name: String,
    val color: String? = null,
)
