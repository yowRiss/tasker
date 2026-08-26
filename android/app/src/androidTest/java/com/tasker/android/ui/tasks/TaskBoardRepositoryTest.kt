package com.tasker.android.ui.tasks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.TokenStore
import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.repository.TaskRepository
import com.tasker.android.sync.SyncOutbox
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskBoardRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var tokenStore: TokenStore
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tokenStore = TokenStore(context).also { store ->
            store.clear()
            store.saveToken("test-server-token")
        }
        repository = TaskRepository(
            db = database,
            taskDao = database.taskDao(),
            projectDao = database.projectDao(),
            tagDao = database.tagDao(),
            subtaskDao = database.subtaskDao(),
            taskTagDao = database.taskTagDao(),
            syncOutbox = SyncOutbox(tokenStore, database.syncQueueDao()),
        )
    }

    @After
    fun tearDown() {
        tokenStore.clear()
        database.close()
    }

    @Test
    fun boardMoveUpdatesOneRecordAndQueuesOneCompletionMutation() = runBlocking {
        database.taskDao().upsert(
            TaskEntity(
                id = "task-1",
                title = "Move me",
                status = "open",
                createdAt = "2026-08-26T00:00:00Z",
                updatedAt = "2026-08-26T00:00:00Z",
            ),
        )

        repository.toggleTaskCompletion("task-1", completed = true)

        val tasks = database.taskDao().observeTasksFiltered(status = "all").first()
        val queued = database.syncQueueDao().getPending()

        assertEquals(1, tasks.size)
        assertEquals("task-1", tasks.single().id)
        assertEquals("completed", tasks.single().status)
        assertEquals(1, queued.size)
        assertEquals("task-1", queued.single().entityId)
        assertEquals("UPDATE", queued.single().operation)
        assertTrue(queued.single().payload.contains("\"completed\":true"))
    }
}
