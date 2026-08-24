package com.tasker.android.sync

import com.tasker.android.data.local.dao.AccountDao
import com.tasker.android.data.local.dao.BudgetDao
import com.tasker.android.data.local.dao.CategoryDao
import com.tasker.android.data.local.dao.NoteDao
import com.tasker.android.data.local.dao.NoteTagDao
import com.tasker.android.data.local.dao.ProjectDao
import com.tasker.android.data.local.dao.RecurringDao
import com.tasker.android.data.local.dao.SubtaskDao
import com.tasker.android.data.local.dao.SyncMetadataDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TagDao
import com.tasker.android.data.local.dao.TargetDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TaskTagDao
import com.tasker.android.data.local.dao.TransactionDao
import com.tasker.android.data.local.entity.AccountEntity
import com.tasker.android.data.local.entity.BudgetEntity
import com.tasker.android.data.local.entity.CategoryEntity
import com.tasker.android.data.local.entity.NoteEntity
import com.tasker.android.data.local.entity.NoteTagEntity
import com.tasker.android.data.local.entity.ProjectEntity
import com.tasker.android.data.local.entity.RecurringTransactionEntity
import com.tasker.android.data.local.entity.SubtaskEntity
import com.tasker.android.data.local.entity.SyncMetadataEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TargetEntity

import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.local.entity.TaskTagEntity
import com.tasker.android.data.local.entity.TransactionEntity
import com.tasker.android.remote.api.MoneyApi
import com.tasker.android.remote.api.NoteApi
import com.tasker.android.remote.api.TaskApi
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PullSync @Inject constructor(
    private val taskApi: TaskApi,
    private val noteApi: NoteApi,
    private val moneyApi: MoneyApi,
    private val projectDao: ProjectDao,
    private val tagDao: TagDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskTagDao: TaskTagDao,
    private val noteDao: NoteDao,
    private val noteTagDao: NoteTagDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val targetDao: TargetDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncMetadataDao: SyncMetadataDao,
) {
    suspend fun pullAll() {
        pullProjects()
        pullTags()
        pullTasks()
        pullNotes()
        pullAccounts()
        pullCategories()
        pullTransactions()
        pullBudgets()
        pullRecurring()
        pullTargets()

        val now = Instant.now().toString()

        syncMetadataDao.upsert(
            SyncMetadataEntity(
                tableName = "all",
                lastSyncedAt = now
            )
        )
    }

    suspend fun pullProjects() {
        try {
            val response = taskApi.listProjects()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = projectDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    projectDao.upsert(
                        ProjectEntity(
                            id = dto.id, name = dto.name, color = dto.color, isArchived = dto.isArchived, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullTags() {
        try {
            val response = taskApi.listTags()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = tagDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    tagDao.upsert(
                        TagEntity(
                            id = dto.id, name = dto.name, color = dto.color, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullTasks() {
        try {
            val response = taskApi.listTasks(status = "all", limit = 1000)
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = taskDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    taskDao.upsert(
                        TaskEntity(
                            id = dto.id, title = dto.title, description = dto.description, status = dto.status, completedAt = dto.completedAt, dueDate = dto.dueDate, priority = dto.priority, projectId = dto.projectId, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                    if (dto.tags != null) {
                        taskTagDao.deleteForTask(dto.id)
                        dto.tags.forEach { tagDto ->
                            taskTagDao.insertAll(listOf(TaskTagEntity(taskId = dto.id, tagId = tagDto.id, createdAt = dto.createdAt)))
                        }
                    }
                    if (dto.subtasks != null) {
                        subtaskDao.deleteForTask(dto.id)
                        val subtaskEntities = dto.subtasks.map { stDto ->
                            SubtaskEntity(id = stDto.id, taskId = dto.id, title = stDto.title, completed = stDto.completed, position = stDto.position, createdAt = stDto.createdAt, updatedAt = stDto.updatedAt, isDeleted = 0)
                        }
                        subtaskDao.upsertAll(subtaskEntities)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullNotes() {
        try {
            val response = noteApi.listNotes(limit = 1000)
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = noteDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    val offsetsStr = dto.reminderOffsets?.joinToString(",") ?: "0"
                    noteDao.upsert(
                        NoteEntity(
                            id = dto.id,
                            title = dto.title,
                            contentMd = dto.contentMd,
                            reminderAt = dto.reminderAt,
                            reminderOffsets = offsetsStr,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt,
                            isDeleted = 0
                        )
                    )
                    if (dto.tags != null) {
                        noteTagDao.deleteForNote(dto.id)
                        dto.tags.forEach { tagDto ->
                            noteTagDao.insertAll(listOf(NoteTagEntity(noteId = dto.id, tagId = tagDto.id, createdAt = dto.createdAt)))
                        }
                    }
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullAccounts() {
        try {
            val response = moneyApi.listAccounts()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = accountDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    accountDao.upsert(
                        AccountEntity(
                            id = dto.id, name = dto.name, accountType = dto.accountType, currency = dto.currency, archivedAt = dto.archivedAt, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullCategories() {
        try {
            val response = moneyApi.listCategories()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = categoryDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    categoryDao.upsert(
                        CategoryEntity(
                            id = dto.id, name = dto.name, categoryType = dto.categoryType, icon = dto.icon, color = dto.color, archivedAt = dto.archivedAt, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullTransactions() {
        try {
            val response = moneyApi.listTransactions(limit = 1000)
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = transactionDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    transactionDao.upsert(
                        TransactionEntity(
                            id = dto.id, transactionType = dto.transactionType, amount = dto.amount.toDoubleOrNull() ?: 0.0, transactionDate = dto.transactionDate, accountId = dto.accountId, transferAccountId = dto.transferAccountId, categoryId = dto.categoryId, description = dto.description, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullBudgets() {
        try {
            val response = moneyApi.listBudgets()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = budgetDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    budgetDao.upsert(
                        BudgetEntity(
                            id = dto.id, categoryId = dto.categoryId, periodStart = dto.periodStart, periodEnd = dto.periodEnd, amountLimit = dto.amountLimit.toDoubleOrNull() ?: 0.0, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullRecurring() {
        try {
            val response = moneyApi.listRecurring()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = recurringDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    recurringDao.upsert(
                        RecurringTransactionEntity(
                            id = dto.id, transactionType = dto.transactionType, amount = dto.amount.toDoubleOrNull() ?: 0.0, accountId = dto.accountId, categoryId = dto.categoryId, description = dto.description, cadence = dto.cadence, nextDueDate = dto.nextDueDate, endsOn = dto.endsOn, isActive = dto.isActive, lastProcessedOn = dto.lastProcessedOn, createdAt = dto.createdAt, updatedAt = dto.updatedAt, isDeleted = 0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun pullTargets() {
        try {
            val response = moneyApi.listTargets()
            val pendingEntities = syncQueueDao.getPending().map { it.entityId }.toSet()

            for (dto in response.items) {
                if (pendingEntities.contains(dto.id)) continue

                val existing = targetDao.getById(dto.id)
                if (existing == null || dto.updatedAt >= existing.updatedAt) {
                    targetDao.upsert(
                        TargetEntity(
                            id = dto.id,
                            name = dto.name,
                            targetAmount = dto.targetAmount.toDoubleOrNull() ?: 0.0,
                            currentAmount = dto.currentAmount.toDoubleOrNull() ?: 0.0,
                            targetDate = dto.targetDate,
                            categoryId = dto.categoryId,
                            accountId = dto.accountId,
                            color = dto.color,
                            icon = dto.icon,
                            status = dto.status,
                            notes = dto.notes,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt,
                            isDeleted = 0,
                        )
                    )
                }
            }
        } catch (e: Exception) { }
    }
}

