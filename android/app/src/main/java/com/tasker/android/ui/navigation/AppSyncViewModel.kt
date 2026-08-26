package com.tasker.android.ui.navigation

import androidx.lifecycle.ViewModel
import com.tasker.android.sync.SyncManager
import com.tasker.android.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppSyncViewModel @Inject constructor(
    syncManager: SyncManager,
) : ViewModel() {
    val syncState: StateFlow<SyncState> = syncManager.syncState
}
