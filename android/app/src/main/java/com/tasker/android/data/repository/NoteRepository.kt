package com.tasker.android.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.dao.NoteDao
import com.tasker.android.data.local.dao.NoteImageDao
import com.tasker.android.data.local.dao.NoteTagDao
import com.tasker.android.data.local.dao.NoteTaskLinkDao
import com.tasker.android.data.local.dao.ProjectDao
import com.tasker.android.data.local.dao.SubtaskDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TagDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TaskTagDao
import com.tasker.android.data.local.entity.NoteEntity
import com.tasker.android.data.local.entity.NoteImageEntity
import com.tasker.android.data.local.entity.NoteTagEntity
import com.tasker.android.data.local.entity.NoteTaskLinkEntity
import com.tasker.android.data.local.entity.SyncQueueEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.model.CreateNoteInput
import com.tasker.android.data.model.Note
import com.tasker.android.data.model.NoteImage
import com.tasker.android.data.model.Tag
import com.tasker.android.data.model.Task
import com.tasker.android.data.model.UpdateNoteInput
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val noteDao: NoteDao,
    private val noteTagDao: NoteTagDao,
    private val noteTaskLinkDao: NoteTaskLinkDao,
    private val noteImageDao: NoteImageDao,
    private val tagDao: TagDao,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val subtaskDao: SubtaskDao,
    private val taskTagDao: TaskTagDao,
    private val syncQueueDao: SyncQueueDao,
) {
    fun observeNotes(query: String = ""): Flow<List<Note>> =
        noteDao.observeNotesFiltered(query).map { noteEntities ->
            val result = mutableListOf<Note>()
            for (entity in noteEntities) {
                val tags = noteTagDao.getTagsForNote(entity.id).map { it.toDomain() }
                val tasks = noteTaskLinkDao.getTasksForNote(entity.id).map { it.toDomain() }
                val images = noteImageDao.getImagesForNote(entity.id).map { it.toDomain() }
                result.add(entity.toDomain(tags, tasks, images))
            }
            result
        }

    suspend fun getNote(id: String): Note? {
        val entity = noteDao.getById(id) ?: return null
        val tags = noteTagDao.getTagsForNote(entity.id).map { it.toDomain() }
        val taskEntities = noteTaskLinkDao.getTasksForNote(entity.id)
        val tasks = mutableListOf<Task>()
        for (te in taskEntities) tasks.add(te.toDomain())
        val images = noteImageDao.getImagesForNote(entity.id).map { it.toDomain() }
        return entity.toDomain(tags, tasks, images)
    }

    suspend fun createNote(input: CreateNoteInput): Note {
        val noteId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val offsetsStr = input.reminderOffsets?.joinToString(",") ?: "0"
        val noteEntity = NoteEntity(
            id = noteId,
            title = input.title.trim(),
            contentMd = input.contentMd,
            reminderAt = input.reminderAt,
            reminderOffsets = offsetsStr,
            createdAt = now,
            updatedAt = now,
        )

        val noteTags = input.tagIds.map { tagId ->
            NoteTagEntity(noteId = noteId, tagId = tagId, createdAt = now)
        }

        val noteTasks = input.taskIds.map { taskId ->
            NoteTaskLinkEntity(noteId = noteId, taskId = taskId, createdAt = now)
        }

        val payload = buildJsonObject {
            put("title", input.title.trim())
            put("content_md", input.contentMd)
            if (input.reminderAt != null) put("reminder_at", input.reminderAt)
            if (input.reminderOffsets != null) {
                put("reminder_offsets", kotlinx.serialization.json.JsonArray(input.reminderOffsets.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            }
        }.toString()

        db.withTransaction {
            noteDao.upsert(noteEntity)
            if (noteTags.isNotEmpty()) noteTagDao.insertAll(noteTags)
            if (noteTasks.isNotEmpty()) noteTaskLinkDao.insertAll(noteTasks)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "note",
                    entityId = noteId,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        return getNote(noteId)!!
    }

    suspend fun updateNote(id: String, input: UpdateNoteInput) {
        val existing = getNote(id) ?: return
        val now = Instant.now().toString()

        val newTitle = input.title?.trim() ?: existing.title
        val newContent = input.contentMd ?: existing.contentMd
        val newReminderAt = input.reminderAt ?: existing.reminderAt
        val newOffsets = input.reminderOffsets ?: existing.reminderOffsets
        val newOffsetsStr = newOffsets.joinToString(",")

        val updatedEntity = NoteEntity(
            id = id,
            title = newTitle,
            contentMd = newContent,
            reminderAt = newReminderAt,
            reminderOffsets = newOffsetsStr,
            createdAt = existing.createdAt,
            updatedAt = now,
        )

        val payload = buildJsonObject {
            put("title", newTitle)
            put("content_md", newContent)
            if (newReminderAt != null) put("reminder_at", newReminderAt)
            put("reminder_offsets", kotlinx.serialization.json.JsonArray(newOffsets.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        }.toString()

        db.withTransaction {
            noteDao.upsert(updatedEntity)
            if (input.tagIds != null) {
                noteTagDao.deleteForNote(id)
                noteTagDao.insertAll(input.tagIds.map { NoteTagEntity(id, it, now) })
            }
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "note",
                    entityId = id,
                    operation = "UPDATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
    }

    suspend fun deleteNote(id: String) {
        val now = Instant.now().toString()
        db.withTransaction {
            noteDao.softDelete(id, now)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "note",
                    entityId = id,
                    operation = "DELETE",
                    payload = "{}",
                    createdAt = now,
                )
            )
        }
    }

    suspend fun attachImage(noteId: String, uri: Uri): NoteImage? {
        val imageId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }

        // Copy uri to app internal storage
        val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
        val destFile = File(imagesDir, "$imageId.$ext")

        val bytesCopied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: 0L
        }.getOrDefault(0L)

        if (bytesCopied <= 0L || !destFile.exists()) {
            if (destFile.exists()) destFile.delete()
            return null
        }

        val imageEntity = NoteImageEntity(
            id = imageId,
            noteId = noteId,
            localUri = destFile.absolutePath,
            originalFilename = "$imageId.$ext",
            mimeType = mimeType,
            byteSize = destFile.length(),
            createdAt = now,
            syncStatus = "pending"
        )

        val payload = buildJsonObject {
            put("note_id", noteId)
            put("image_id", imageId)
            put("local_uri", destFile.absolutePath)
        }.toString()

        db.withTransaction {
            noteImageDao.upsert(imageEntity)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "note_image",
                    entityId = imageId,
                    operation = "UPLOAD_IMAGE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        return imageEntity.toDomain()
    }

    suspend fun deleteNoteImage(imageId: String) {
        val existing = noteImageDao.getById(imageId) ?: return
        val now = Instant.now().toString()

        runCatching {
            val file = File(existing.localUri)
            if (file.exists()) file.delete()
        }

        val payload = buildJsonObject {
            put("image_id", imageId)
            put("note_id", existing.noteId)
            if (existing.objectPath != null) put("object_path", existing.objectPath)
        }.toString()

        db.withTransaction {
            noteImageDao.delete(imageId)
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "note_image",
                    entityId = imageId,
                    operation = "DELETE_IMAGE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
    }

    // ── Mappers ────────────────────────────────────────────────────

    private fun TagEntity.toDomain() = Tag(
        id = id, name = name, color = color, createdAt = createdAt, updatedAt = updatedAt
    )

    private suspend fun TaskEntity.toDomain(): Task {
        val project = projectId?.let { projectDao.getById(it) }?.let {
            com.tasker.android.data.model.Project(it.id, it.name, it.color, it.isArchived, it.createdAt, it.updatedAt)
        }
        val tags = taskTagDao.getTagsForTask(id).map { it.toDomain() }
        val subtasks = subtaskDao.getSubtasksForTask(id).map {
            com.tasker.android.data.model.Subtask(it.id, it.taskId, it.title, it.completed, it.position, it.createdAt, it.updatedAt)
        }
        return Task(id, title, description, status, completedAt, dueDate, priority, projectId, project, tags, subtasks, createdAt, updatedAt)
    }

    private fun NoteImageEntity.toDomain() = NoteImage(
        id = id,
        noteId = noteId,
        localUri = localUri,
        objectPath = objectPath,
        originalFilename = originalFilename,
        mimeType = mimeType,
        byteSize = byteSize,
        altText = altText,
        syncStatus = syncStatus
    )

    private fun NoteEntity.toDomain(tags: List<Tag>, tasks: List<Task>, images: List<NoteImage>): Note {
        val offsets = try {
            reminderOffsets.split(",").mapNotNull { it.trim().toIntOrNull() }
        } catch (_: Exception) {
            listOf(0)
        }
        return Note(
            id = id,
            title = title,
            contentMd = contentMd,
            reminderAt = reminderAt,
            reminderOffsets = if (offsets.isNotEmpty()) offsets else listOf(0),
            tags = tags,
            tasks = tasks,
            images = images,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
