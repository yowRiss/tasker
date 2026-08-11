package com.tasker.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tasker.android.sync.SyncState
import com.tasker.android.ui.theme.TaskerTheme

@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TaskerTheme.colors

    val (badgeBg, badgeFg, statusText) = when {
        !syncState.isOnline -> Triple(colors.warningSubtle, colors.warning, "Offline")
        syncState.isSyncing -> Triple(colors.accentSubtle, colors.accent, "Syncing...")
        syncState.failedCount > 0 -> Triple(colors.destructiveSubtle, colors.destructive, "${syncState.failedCount} error(s)")
        syncState.pendingCount > 0 -> Triple(colors.accentSubtle, colors.accent, "${syncState.pendingCount} pending")
        else -> Triple(colors.accentSubtle, colors.accent, "Synced")
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSyncClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeFg, CircleShape)
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeFg
            )

            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = badgeFg
                )
            }
        }
    }
}
