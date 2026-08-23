package com.tasker.android.sync

import com.tasker.android.data.local.TokenStore
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.entity.SyncQueueEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncOutbox @Inject constructor(
    private val tokenStore: TokenStore,
    private val syncQueueDao: SyncQueueDao,
) {
    suspend fun enqueue(item: SyncQueueEntity): Long? =
        if (tokenStore.isLoggedIn()) syncQueueDao.enqueue(item) else null
}
