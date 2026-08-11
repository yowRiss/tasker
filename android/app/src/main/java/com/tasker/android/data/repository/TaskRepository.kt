package com.tasker.android.data.repository

import androidx.room.withTransaction
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.dao.ProjectDao
import com.tasker.android.data.local.dao.SubtaskDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TagDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TaskTagDao
import com.tasker.android.data.local.entity.ProjectEntity
import com.tasker.android.data.local.entity.SubtaskEntity
import com.tasker.android.data.local.entity.SyncQueueEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.local.entity.TaskTagEntity
import com.tasker.android.data.model.CreateTaskInput
import com.tasker.android.data.model.Project
import com.tasker.android.data.model.Subtask
import com.tasker.android.data.model.Tag
import com.tasker.android.data.model.Task
import com.tasker.android.data.model.TaskFilters
import com.tasker.android.data.model.UpdateTaskInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val tagDao: TagDao,
    private val subtaskDao: SubtaskDao,
    private val taskTagDao: TaskTagDao,
    private val syncQueueDao: SyncQueueDao,
) {
    private val jsonFormatter = Json { ignoreUnknownKeys = true }

    // ── Projects ───────────────────────────────────────────────────

    fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun createProject(name: String, color: String? = null): Project {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val entity = ProjectEntity(
            id = id,
            name = name.trim(),
            color = color,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
        )

        val payload = buildJsonObject {
            put("name", name.trim())
            if (color != null) put("color", color)
        }.toString()

        db.withTransaction {
            projectDao.upsert(entity)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "project",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
        return entity.toDomain()
    }

    // ── Tags ───────────────────────────────────────────────────────

    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun createTag(name: String, color: String? = null): Tag {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val entity = TagEntity(
            id = id,
            name = name.trim(),
            color = color,
            createdAt = now,
            updatedAt = now,
        )

        val payload = buildJsonObject {
            put("name", name.trim())
            if (color != null) put("color", color)
        }.toString()

        db.withTransaction {
            tagDao.upsert(entity)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "tag",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
        return entity.toDomain()
    }

    // ── Tasks ──────────────────────────────────────────────────────

    fun observeTasks(filters: TaskFilters): Flow<List<Task>> {
        return taskDao.observeTasksFiltered(
            status = filters.status,
            projectId = filters.projectId,
            priority = filters.priority,
            query = filters.query,
        ).map { taskEntities ->
            taskEntities.map { entity ->
                val project = entity.projectId?.let { projectDao.getById(it)?.toDomain() }
                val tags = taskTagDao.getTagsForTask(entity.id).map { it.toDomain() }
                val subtasks = subtaskDao.getSubtasksForTask(entity.id).map { it.toDomain() }
                entity.toDomain(project, tags, subtasks)
            }
        }
    }

    suspend fun getTask(id: String): Task? {
        val entity = taskDao.getById(id) ?: return null
        val project = entity.projectId?.let { projectDao.getById(it)?.toDomain() }
        val tags = taskTagDao.getTagsForTask(entity.id).map { it.toDomain() }
        val subtasks = subtaskDao.getSubtasksForTask(entity.id).map { it.toDomain() }
        return entity.toDomain(project, tags, subtasks)
    }

    suspend fun createTask(input: CreateTaskInput): Task {
        val taskId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val taskEntity = TaskEntity(
            id = taskId,
            title = input.title.trim(),
            description = input.description?.trim(),
            status = "open",
            dueDate = input.dueDate,
            priority = input.priority,
            projectId = input.projectId,
            createdAt = now,
            updatedAt = now,
        )

        val taskTags = input.tagIds.map { tagId ->
            TaskTagEntity(taskId = taskId, tagId = tagId, createdAt = now)
        }

        val subtasks = input.subtasks.mapIndexed { index, subTitle ->
            SubtaskEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = subTitle.trim(),
                completed = false,
                position = index,
                createdAt = now,
                updatedAt = now,
            )
        }

        val payload = buildJsonObject {
            put("id", taskId)
            put("title", input.title.trim())
            if (input.description != null) put("description", input.description)
            if (input.dueDate != null) put("due_date", input.dueDate)
            put("priority", input.priority)
            if (input.projectId != null) put("project_id", input.projectId)
            if (input.tagIds.isNotEmpty()) putJsonArray("tag_ids") {
                input.tagIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            }
        }.toString()

        db.withTransaction {
            taskDao.upsert(taskEntity)
            if (taskTags.isNotEmpty()) taskTagDao.insertAll(taskTags)
            if (subtasks.isNotEmpty()) subtaskDao.upsertAll(subtasks)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "task",
                    entityId = taskId,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        return getTask(taskId)!!
    }

    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean) {
        val now = Instant.now().toString()
        val status = if (completed) "completed" else "open"
        val completedAt = if (completed) now else null

        val payload = buildJsonObject {
            put("completed", completed)
        }.toString()

        db.withTransaction {
            taskDao.updateCompletion(taskId, status, completedAt, now)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "task",
                    entityId = taskId,
                    operation = "UPDATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
    }

    suspend fun deleteTask(taskId: String) {
        val now = Instant.now().toString()
        db.withTransaction {
            taskDao.softDelete(taskId, now)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "task",
                    entityId = taskId,
                    operation = "DELETE",
                    payload = "{}",
                    createdAt = now,
                )
            )
        }
    }

    // ── Mappers ────────────────────────────────────────────────────

    private fun ProjectEntity.toDomain() = Project(
        id = id,
        name = name,
        color = color,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TagEntity.toDomain() = Tag(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SubtaskEntity.toDomain() = Subtask(
        id = id,
        taskId = taskId,
        title = title,
        completed = completed,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TaskEntity.toDomain(
        project: Project?,
        tags: List<Tag>,
        subtasks: List<Subtask>,
    ) = Task(
        id = id,
        title = title,
        description = description,
        status = status,
        completedAt = completedAt,
        dueDate = dueDate,
        priority = priority,
        projectId = projectId,
        project = project,
        tags = tags,
        subtasks = subtasks,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
