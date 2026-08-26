package com.tasker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tasker.android.sync.SyncState
import com.tasker.android.ui.theme.TaskerTheme

@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    onSyncClick: (() -> Unit)? = null,
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

    val interactionModifier = if (onSyncClick != null) {
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                onClickLabel = "Sync now",
                role = Role.Button,
                onClick = onSyncClick,
            )
    } else {
        Modifier
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(interactionModifier)
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
                color = colors.textPrimary
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
