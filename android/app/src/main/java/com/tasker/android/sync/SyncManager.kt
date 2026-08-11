package com.tasker.android.sync

import com.tasker.android.data.local.dao.SyncQueueDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
)

@Singleton
class SyncManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val queueProcessor: QueueProcessor,
    private val pullSync: PullSync,
    private val syncQueueDao: SyncQueueDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isSyncing = MutableStateFlow(false)

    val syncState: StateFlow<SyncState> = combine(
        networkMonitor.isOnline,
        _isSyncing,
        syncQueueDao.observePendingCount(),
        syncQueueDao.observeFailedCount(),
    ) { isOnline, isSyncing, pending, failed ->
        SyncState(
            isOnline = isOnline,
            isSyncing = isSyncing,
            pendingCount = pending,
            failedCount = failed,
        )
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        SyncState()
    )

    init {
        // Observe connectivity changes -> trigger sync on reconnect
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && !_isSyncing.value) {
                    triggerSync()
                }
            }
        }

        // 60-second foreground periodic sync loop while online
        scope.launch {
            while (true) {
                delay(60_000)
                if (networkMonitor.isOnline.value && !_isSyncing.value) {
                    triggerSync()
                }
            }
        }
    }

    suspend fun triggerSync() {
        if (_isSyncing.value || !networkMonitor.isOnline.value) return

        _isSyncing.value = true
        try {
            // 1. Push pending local mutations
            val result = queueProcessor.processQueue()
            if (result.pauseReason == PauseReason.AUTH_ERROR) return

            // 2. Pull server changes
            pullSync.pullAll()
        } catch (e: Exception) {
            // Log & finish sync loop
        } finally {
            _isSyncing.value = false
        }
    }
}
