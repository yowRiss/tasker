package com.tasker.android.ui.offline

import com.tasker.android.sync.SyncState

internal fun offlineStatusText(syncState: SyncState): String? = when {
    !syncState.isOnline && syncState.failedCount > 0 ->
        "Offline · ${syncState.failedCount} sync failure${if (syncState.failedCount == 1) "" else "s"} need attention"
    !syncState.isOnline && syncState.pendingCount > 0 ->
        "Offline · ${syncState.pendingCount} change${if (syncState.pendingCount == 1) "" else "s"} saved locally"
    !syncState.isOnline -> "Offline · Your data is available on this device"
    syncState.failedCount > 0 ->
        "Sync needs attention · ${syncState.failedCount} item${if (syncState.failedCount == 1) "" else "s"}"
    else -> null
}

internal enum class OfflineSummaryState {
    OFFLINE_WITH_FAILURES,
    OFFLINE,
    SYNCING,
    FAILED,
    PENDING,
    LOCAL_ONLY,
    SYNCED,
}

internal fun offlineSummaryState(
    syncState: SyncState,
    accountConnected: Boolean,
): OfflineSummaryState = when {
    !syncState.isOnline && syncState.failedCount > 0 -> OfflineSummaryState.OFFLINE_WITH_FAILURES
    !syncState.isOnline -> OfflineSummaryState.OFFLINE
    syncState.isSyncing -> OfflineSummaryState.SYNCING
    syncState.failedCount > 0 -> OfflineSummaryState.FAILED
    syncState.pendingCount > 0 -> OfflineSummaryState.PENDING
    !accountConnected -> OfflineSummaryState.LOCAL_ONLY
    else -> OfflineSummaryState.SYNCED
}
