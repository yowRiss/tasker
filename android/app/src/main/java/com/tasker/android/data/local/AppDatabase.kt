package com.tasker.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.tasker.android.data.local.dao.SyncMetadataDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TagDao
import com.tasker.android.data.local.dao.TargetDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TaskTagDao
import com.tasker.android.data.local.dao.TransactionDao
import com.tasker.android.data.local.dao.TransactionReceiptDao
import com.tasker.android.data.local.entity.AccountEntity
import com.tasker.android.data.local.entity.BudgetEntity
import com.tasker.android.data.local.entity.CategoryEntity
import com.tasker.android.data.local.entity.NoteEntity
import com.tasker.android.data.local.entity.NoteImageEntity
import com.tasker.android.data.local.entity.NoteTagEntity
import com.tasker.android.data.local.entity.NoteTaskLinkEntity
import com.tasker.android.data.local.entity.ProjectEntity
import com.tasker.android.data.local.entity.RecurringTransactionEntity
import com.tasker.android.data.local.entity.SubtaskEntity
import com.tasker.android.data.local.entity.SyncMetadataEntity
import com.tasker.android.data.local.entity.SyncQueueEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TargetEntity
import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.local.entity.TaskTagEntity
import com.tasker.android.data.local.entity.TransactionEntity
import com.tasker.android.data.local.entity.TransactionReceiptEntity

@Database(
    entities = [
        SyncQueueEntity::class,
        SyncMetadataEntity::class,
        ProjectEntity::class,
        TagEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        TaskTagEntity::class,
        NoteEntity::class,
        NoteTagEntity::class,
        NoteTaskLinkEntity::class,
        NoteImageEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionReceiptEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        TargetEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun projectDao(): ProjectDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun taskTagDao(): TaskTagDao
    abstract fun noteDao(): NoteDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun noteTaskLinkDao(): NoteTaskLinkDao
    abstract fun noteImageDao(): NoteImageDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun receiptDao(): TransactionReceiptDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun targetDao(): TargetDao
}

