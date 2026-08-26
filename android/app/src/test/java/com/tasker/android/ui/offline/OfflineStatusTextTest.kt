package com.tasker.android.ui.offline

import com.tasker.android.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineStatusTextTest {
    @Test
    fun contentSummaryCountsEveryOfflineCoreRecord() {
        assertEquals(
            6,
            OfflineContentState(taskCount = 1, noteCount = 2, transactionCount = 3).totalCount,
        )
    }

    @Test
    fun healthyOnlineStateDoesNotShowAnExceptionBanner() {
        assertNull(offlineStatusText(SyncState(isOnline = true)))
    }

    @Test
    fun offlineStateExplainsThatChangesAreSavedLocally() {
        assertEquals(
            "Offline · 2 changes saved locally",
            offlineStatusText(SyncState(isOnline = false, pendingCount = 2)),
        )
    }

    @Test
    fun failedOnlineStateLinksToSyncAttention() {
        assertEquals(
            "Sync needs attention · 1 item",
            offlineStatusText(SyncState(isOnline = true, failedCount = 1)),
        )
    }

    @Test
    fun summaryStateDoesNotTreatPendingChangesAsSynced() {
        assertEquals(
            OfflineSummaryState.PENDING,
            offlineSummaryState(
                syncState = SyncState(isOnline = true, pendingCount = 2),
                accountConnected = true,
            ),
        )
    }

    @Test
    fun summaryStateRequiresConnectedAccountBeforeClaimingServerSync() {
        assertEquals(
            OfflineSummaryState.LOCAL_ONLY,
            offlineSummaryState(SyncState(isOnline = true), accountConnected = false),
        )
        assertEquals(
            OfflineSummaryState.SYNCED,
            offlineSummaryState(SyncState(isOnline = true), accountConnected = true),
        )
    }
}
