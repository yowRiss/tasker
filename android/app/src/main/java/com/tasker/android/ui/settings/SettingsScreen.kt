package com.tasker.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.repository.AuthRepository
import com.tasker.android.sync.SyncManager
import com.tasker.android.sync.SyncState
import com.tasker.android.ui.components.SyncStatusBadge
import com.tasker.android.ui.theme.TaskerTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncManager.syncState

    fun getUsername(): String = authRepository.getCurrentUsername() ?: "User"

    fun triggerManualSync() {
        viewModelScope.launch {
            syncManager.triggerSync()
        }
    }

    fun logout() {
        authRepository.logout()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val syncState by viewModel.syncState.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = colors.accentSubtle,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = colors.accent)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(viewModel.getUsername(), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Tasker Personal Account", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // Sync Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Engine", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        SyncStatusBadge(syncState = syncState, onSyncClick = viewModel::triggerManualSync)
                    }

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Connection Status", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Text(if (syncState.isOnline) "Connected (Online)" else "Offline Mode", style = MaterialTheme.typography.bodyMedium, color = if (syncState.isOnline) colors.success else colors.warning)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pending Mutations", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Text("${syncState.pendingCount} queued", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    }

                    if (syncState.failedCount > 0) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Failed Sync Items", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            Text("${syncState.failedCount} errors", style = MaterialTheme.typography.bodyMedium, color = colors.destructive)
                        }
                    }

                    Button(
                        onClick = viewModel::triggerManualSync,
                        enabled = syncState.isOnline && !syncState.isSyncing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Now")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Logout Button
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.destructive),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.destructive),
                ),
            ) {
                Text("Log out", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
