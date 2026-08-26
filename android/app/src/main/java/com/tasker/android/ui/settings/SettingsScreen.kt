package com.tasker.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.BuildConfig
import com.tasker.android.data.repository.AuthRepository
import com.tasker.android.sync.SyncManager
import com.tasker.android.sync.SyncState
import com.tasker.android.ui.components.SyncStatusBadge
import com.tasker.android.ui.theme.TaskerTheme
import com.tasker.android.update.UpdateManager
import com.tasker.android.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    val updateManager: UpdateManager,
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val updateState: StateFlow<UpdateState> = updateManager.updateState
    private val _accountConnected = MutableStateFlow(authRepository.isLoggedIn())
    val accountConnected: StateFlow<Boolean> = _accountConnected.asStateFlow()

    fun getUsername(): String = authRepository.getCurrentUsername() ?: "User"

    fun triggerManualSync() {
        viewModelScope.launch {
            syncManager.triggerSync()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(isAutoCheck = false)
        }
    }

    fun disconnectAccount() {
        authRepository.logout()
        _accountConnected.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onConnectAccount: () -> Unit,
    onOpenOffline: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val syncState by viewModel.syncState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val accountConnected by viewModel.accountConnected.collectAsState()
    val offlineAccessIcon = when {
        !syncState.isOnline -> Icons.Outlined.CloudOff
        syncState.failedCount > 0 -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.CloudDone
    }
    val offlineAccessTint = when {
        !syncState.isOnline -> colors.warning
        syncState.failedCount > 0 -> colors.destructive
        else -> colors.success
    }
    val offlineAccessBackground = when {
        !syncState.isOnline -> colors.warningSubtle
        syncState.failedCount > 0 -> colors.destructiveSubtle
        else -> colors.successSubtle
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (accountConnected) {
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
                            color = colors.accentSubtle,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Person, null, tint = colors.accent)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.getUsername(), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Tasker Personal Account", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Sync Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            SyncStatusBadge(
                                syncState = syncState,
                                onSyncClick = if (syncState.isOnline) viewModel::triggerManualSync else null,
                            )
                        }

                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Connection Status", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text(if (syncState.isOnline) "Connected (Online)" else "Offline Mode", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Pending Mutations", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("${syncState.pendingCount} queued", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }

                        if (syncState.failedCount > 0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Failed Sync Items", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Text("${syncState.failedCount} errors", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            }
                        }

                        Button(
                            onClick = viewModel::triggerManualSync,
                            enabled = syncState.isOnline && !syncState.isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = colors.accentSubtle,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Person, null, tint = colors.accent)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local mode", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(
                                    "No account required. Your data stays on this device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onConnectAccount,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Connect account for sync")
                        }
                    }
                }
            }

            Card(
                onClick = onOpenOffline,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = offlineAccessBackground,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = offlineAccessIcon,
                                contentDescription = null,
                                tint = offlineAccessTint,
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline access",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = when {
                                !syncState.isOnline -> "Your content remains available on this device"
                                syncState.failedCount > 0 -> "Review local content and failed sync items"
                                else -> "Review device content and sync readiness"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                    )
                }
            }

            // Automatic Updates Card
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
                        Text("Automatic Updates", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = colors.accent)
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Installed Version", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Update Source", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Text("GitHub Releases (${BuildConfig.GITHUB_REPO})", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Status", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        val statusText = when (updateState) {
                            is UpdateState.Checking -> "Checking for updates..."
                            is UpdateState.UpToDate -> "App is up to date"
                            is UpdateState.UpdateAvailable -> "New version available!"
                            is UpdateState.Downloading -> "Downloading update..."
                            is UpdateState.ReadyToInstall -> "Ready to install"
                            is UpdateState.Error -> "Check failed"
                            else -> "Auto-update enabled"
                        }
                        Text(statusText, style = MaterialTheme.typography.bodyMedium, color = if (updateState is UpdateState.UpdateAvailable) colors.accent else colors.textPrimary)
                    }

                    OutlinedButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Checking...")
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check for Updates")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (accountConnected) {
                OutlinedButton(
                    onClick = viewModel::disconnectAccount,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Disconnect account", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
