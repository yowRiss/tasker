package com.tasker.android.ui.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasker.android.sync.SyncState
import com.tasker.android.ui.components.SyncStatusBadge
import com.tasker.android.ui.theme.TaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    onBack: () -> Unit,
    onConnectAccount: () -> Unit,
    viewModel: OfflineViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val contentState by viewModel.contentState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Offline access",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OfflineSummaryCard(
                syncState = syncState,
                contentCount = contentState.totalCount,
                accountConnected = viewModel.accountConnected,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Available on this device",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Active content is stored in private app storage and remains available without a connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    OfflineContentRow(
                        icon = Icons.Outlined.CheckBox,
                        label = "Tasks",
                        supportingText = "Create, edit, complete and change views",
                        count = contentState.taskCount,
                    )
                    OfflineContentRow(
                        icon = Icons.Outlined.Notes,
                        label = "Notes",
                        supportingText = "Read, write and use Markdown",
                        count = contentState.noteCount,
                    )
                    OfflineContentRow(
                        icon = Icons.Outlined.ReceiptLong,
                        label = "Transactions",
                        supportingText = "Record and review money activity",
                        count = contentState.transactionCount,
                    )
                }
            }

            SyncQueueCard(
                syncState = syncState,
                accountConnected = viewModel.accountConnected,
                onSync = viewModel::syncNow,
                onRetryFailed = viewModel::retryFailed,
                onConnectAccount = onConnectAccount,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Offline capabilities",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    CapabilityRow(Icons.Outlined.CheckBox, "Create and edit tasks")
                    CapabilityRow(Icons.Outlined.CheckBox, "Use list, Kanban board and table task views")
                    CapabilityRow(Icons.Outlined.Notes, "Create and edit Markdown notes")
                    CapabilityRow(Icons.Outlined.CalendarMonth, "View locally stored calendar items")
                    CapabilityRow(Icons.Outlined.ReceiptLong, "Create and edit money records")
                    CapabilityRow(Icons.Outlined.Image, "Keep attached local images and receipts available")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceAlt),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "A connection is still required",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Account connection, server backup, pulling changes from other devices, and uploading queued images or receipts resume when the network returns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OfflineSummaryCard(
    syncState: SyncState,
    contentCount: Int,
    accountConnected: Boolean,
) {
    val colors = TaskerTheme.colors
    val (icon, title, description, iconColor, iconBackground) = when (
        offlineSummaryState(syncState, accountConnected)
    ) {
        OfflineSummaryState.OFFLINE_WITH_FAILURES -> OfflineSummaryVisual(
            Icons.Outlined.CloudOff,
            "Offline · sync needs attention",
            "$contentCount items remain available. ${syncState.failedCount} failed sync item${if (syncState.failedCount == 1) " needs" else "s need"} retry after reconnecting.",
            colors.destructive,
            colors.destructiveSubtle,
        )
        OfflineSummaryState.OFFLINE -> OfflineSummaryVisual(
            Icons.Outlined.CloudOff,
            "Offline and ready",
            if (accountConnected) {
                "$contentCount items remain available. New changes are saved on this device and queued for sync."
            } else {
                "$contentCount items remain available. New changes remain local-only while no account is connected."
            },
            colors.warning,
            colors.warningSubtle,
        )
        OfflineSummaryState.SYNCING -> OfflineSummaryVisual(
            Icons.Outlined.Sync,
            "Syncing local changes",
            "$contentCount items are available while Tasker updates the server backup.",
            colors.accent,
            colors.accentSubtle,
        )
        OfflineSummaryState.FAILED -> OfflineSummaryVisual(
            Icons.Outlined.CloudOff,
            "Saved locally · sync needs attention",
            "$contentCount items remain available. ${syncState.failedCount} queued item${if (syncState.failedCount == 1) " needs" else "s need"} another sync attempt.",
            colors.destructive,
            colors.destructiveSubtle,
        )
        OfflineSummaryState.PENDING -> OfflineSummaryVisual(
            Icons.Outlined.Sync,
            "Saved locally · waiting to sync",
            "$contentCount items remain available. ${syncState.pendingCount} local change${if (syncState.pendingCount == 1) " is" else "s are"} waiting for server sync.",
            colors.accent,
            colors.accentSubtle,
        )
        OfflineSummaryState.LOCAL_ONLY -> OfflineSummaryVisual(
            Icons.Outlined.CloudDone,
            "Offline access ready",
            "$contentCount items are stored on this device. Local-only content is not backed up to a server.",
            colors.success,
            colors.successSubtle,
        )
        OfflineSummaryState.SYNCED -> OfflineSummaryVisual(
            Icons.Outlined.CloudDone,
            "Online and synced",
            "$contentCount items are stored on this device and synced to the server.",
            colors.success,
            colors.successSubtle,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = iconBackground,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

private data class OfflineSummaryVisual(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconColor: androidx.compose.ui.graphics.Color,
    val iconBackground: androidx.compose.ui.graphics.Color,
)

@Composable
private fun OfflineContentRow(
    icon: ImageVector,
    label: String,
    supportingText: String,
    count: Int,
) {
    val colors = TaskerTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
    }
}

@Composable
private fun SyncQueueCard(
    syncState: SyncState,
    accountConnected: Boolean,
    onSync: () -> Unit,
    onRetryFailed: () -> Unit,
    onConnectAccount: () -> Unit,
) {
    val colors = TaskerTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sync queue",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (accountConnected) {
                    SyncStatusBadge(
                        syncState = syncState,
                    )
                } else {
                    Surface(
                        color = colors.surfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Local only",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = when {
                    !accountConnected -> "Content created in local-only mode stays only on this device. Connect before creating changes you want backed up."
                    syncState.failedCount > 0 && !syncState.isOnline -> "${syncState.failedCount} failed sync item${if (syncState.failedCount == 1) " needs" else "s need"} retry after reconnecting."
                    !syncState.isOnline -> "${syncState.pendingCount} change${if (syncState.pendingCount == 1) " is" else "s are"} waiting for a connection."
                    syncState.failedCount > 0 -> "${syncState.failedCount} item${if (syncState.failedCount == 1) " needs" else "s need"} another sync attempt."
                    syncState.pendingCount > 0 -> "${syncState.pendingCount} local change${if (syncState.pendingCount == 1) " is" else "s are"} ready to sync."
                    else -> "No local changes are waiting to sync."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            if (accountConnected) {
                Button(
                    onClick = if (syncState.failedCount > 0) onRetryFailed else onSync,
                    enabled = syncState.isOnline && !syncState.isSyncing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            syncState.isSyncing -> "Syncing"
                            syncState.failedCount > 0 -> "Retry failed sync"
                            else -> "Sync now"
                        },
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onConnectAccount,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Connect account for server backup")
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    icon: ImageVector,
    text: String,
) {
    val colors = TaskerTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
