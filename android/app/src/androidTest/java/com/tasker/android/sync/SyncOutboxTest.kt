package com.tasker.android.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tasker.android.data.local.TokenStore
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncOutboxTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tokenStore = TokenStore(context)
    private val queueDao = RecordingSyncQueueDao()
    private val outbox = SyncOutbox(tokenStore, queueDao)

    @Before
    @After
    fun clearSession() {
        tokenStore.clear()
        queueDao.enqueuedItem = null
    }

    @Test
    fun localOnlyChangesAreNotQueuedForServerSync() = runBlocking {
        assertNull(outbox.enqueue(change()))
        assertNull(queueDao.enqueuedItem)
    }

    @Test
    fun connectedAccountChangesAreQueuedForServerSync() = runBlocking {
        tokenStore.saveToken("server-token")

        assertEquals(1L, outbox.enqueue(change()))
        assertEquals("task-1", queueDao.enqueuedItem?.entityId)
    }

    private fun change() = SyncQueueEntity(
        entityType = "task",
        entityId = "task-1",
        operation = "CREATE",
        payload = "{}",
        createdAt = "2026-08-23T18:00:00Z",
    )

    private class RecordingSyncQueueDao : SyncQueueDao {
        var enqueuedItem: SyncQueueEntity? = null

        override suspend fun getPending(): List<SyncQueueEntity> = emptyList()
        override fun observePendingCount(): Flow<Int> = flowOf(0)
        override fun observeFailedCount(): Flow<Int> = flowOf(0)

        override suspend fun enqueue(item: SyncQueueEntity): Long {
            enqueuedItem = item
            return 1L
        }

        override suspend fun delete(id: Long) = Unit
        override suspend fun updateStatus(id: Long, status: String, error: String?) = Unit
        override suspend fun retryAllFailed() = Unit
        override suspend fun remapEntityId(oldId: String, newId: String) = Unit
    }
}
