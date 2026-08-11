package com.tasker.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"], unique = false)]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String? = null,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = false)]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["status", "due_date"]),
        Index(value = ["project_id"]),
        Index(value = ["updated_at"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String = "open", // open | completed | archived
    @ColumnInfo(name = "completed_at")
    val completedAt: String? = null,
    @ColumnInfo(name = "due_date")
    val dueDate: String? = null,
    val priority: Int = 0, // 0..3
    @ColumnInfo(name = "project_id")
    val projectId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["task_id"])]
)
data class SubtaskEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    val title: String,
    val completed: Boolean = false,
    val position: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["task_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["task_id"]), Index(value = ["tag_id"])]
)
data class TaskTagEntity(
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
