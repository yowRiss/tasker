package com.tasker.android.di

import android.content.Context
import androidx.room.Room
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.MIGRATION_1_2
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "tasker.db"
    )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addMigrations(MIGRATION_1_2)
        .build()

    @Provides fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun provideSyncMetadataDao(db: AppDatabase): SyncMetadataDao = db.syncMetadataDao()
    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideSubtaskDao(db: AppDatabase): SubtaskDao = db.subtaskDao()
    @Provides fun provideTaskTagDao(db: AppDatabase): TaskTagDao = db.taskTagDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideNoteTagDao(db: AppDatabase): NoteTagDao = db.noteTagDao()
    @Provides fun provideNoteTaskLinkDao(db: AppDatabase): NoteTaskLinkDao = db.noteTaskLinkDao()
    @Provides fun provideNoteImageDao(db: AppDatabase): NoteImageDao = db.noteImageDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideReceiptDao(db: AppDatabase): TransactionReceiptDao = db.receiptDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideRecurringDao(db: AppDatabase): RecurringDao = db.recurringDao()
    @Provides fun provideTargetDao(db: AppDatabase): TargetDao = db.targetDao()
}

