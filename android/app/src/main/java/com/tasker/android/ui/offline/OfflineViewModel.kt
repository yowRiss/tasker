package com.tasker.android.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.local.dao.NoteDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.data.local.dao.TaskDao
import com.tasker.android.data.local.dao.TransactionDao
import com.tasker.android.data.repository.AuthRepository
import com.tasker.android.sync.SyncManager
import com.tasker.android.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfflineContentState(
    val taskCount: Int = 0,
    val noteCount: Int = 0,
    val transactionCount: Int = 0,
) {
    val totalCount: Int
        get() = taskCount + noteCount + transactionCount
}

@HiltViewModel
class OfflineViewModel @Inject constructor(
    taskDao: TaskDao,
    noteDao: NoteDao,
    transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val contentState: StateFlow<OfflineContentState> = combine(
        taskDao.observeActiveCount(),
        noteDao.observeActiveCount(),
        transactionDao.observeActiveCount(),
    ) { taskCount, noteCount, transactionCount ->
        OfflineContentState(
            taskCount = taskCount,
            noteCount = noteCount,
            transactionCount = transactionCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfflineContentState(),
    )

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val accountConnected: Boolean
        get() = authRepository.isLoggedIn()

    fun syncNow() {
        viewModelScope.launch {
            syncManager.triggerSync()
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            syncQueueDao.retryAllFailed()
            syncManager.triggerSync()
        }
    }
}
