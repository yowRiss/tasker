package com.tasker.android.sync

import androidx.room.withTransaction
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.dao.AccountDao
import com.tasker.android.data.local.dao.BudgetDao
import com.tasker.android.data.local.dao.CategoryDao
import com.tasker.android.data.local.dao.NoteDao
import com.tasker.android.data.local.dao.NoteImageDao
import com.tasker.android.data.local.dao.NoteTagDao
import com.tasker.android.data.local.dao.NoteTaskLinkDao
import com.tasker.android.data.local.dao.ProjectDao
import com.tasker.android.data.local.dao.RecurringDao
import com.tasker.android.data.local.dao.SubtaskDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TagDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TaskTagDao
import com.tasker.android.data.local.dao.TransactionDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdRemapper @Inject constructor(
    private val db: AppDatabase,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskTagDao: TaskTagDao,
    private val projectDao: ProjectDao,
    private val tagDao: TagDao,
    private val noteDao: NoteDao,
    private val noteTagDao: NoteTagDao,
    private val noteTaskLinkDao: NoteTaskLinkDao,
    private val noteImageDao: NoteImageDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val syncQueueDao: SyncQueueDao,
) {
    suspend fun remapId(entityType: String, oldId: String, newId: String) {
        if (oldId == newId) return

        db.withTransaction {
            when (entityType) {
                "task" -> {
                    taskDao.remapId(oldId, newId)
                    subtaskDao.remapTaskId(oldId, newId)
                    taskTagDao.remapTaskId(oldId, newId)
                }
                "project" -> {
                    projectDao.remapId(oldId, newId)
                }
                "tag" -> {
                    tagDao.remapId(oldId, newId)
                    taskTagDao.remapTagId(oldId, newId)
                }
                "note" -> {
                    noteDao.remapId(oldId, newId)
                    noteTagDao.remapNoteId(oldId, newId)
                    noteTaskLinkDao.remapNoteId(oldId, newId)
                    noteImageDao.remapNoteId(oldId, newId)
                }
                "account" -> {
                    accountDao.remapId(oldId, newId)
                }
                "category" -> {
                    categoryDao.remapId(oldId, newId)
                }
                "transaction" -> {
                    transactionDao.remapId(oldId, newId)
                }
                "budget" -> {
                    budgetDao.remapId(oldId, newId)
                }
                "recurring_transaction" -> {
                    recurringDao.remapId(oldId, newId)
                }
            }
            syncQueueDao.remapEntityId(oldId, newId)
        }
    }
}
