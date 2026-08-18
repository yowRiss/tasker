package com.tasker.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.tasker.android.ui.navigation.AppNavGraph
import com.tasker.android.ui.theme.TaskerTheme
import com.tasker.android.update.UpdateDialog
import com.tasker.android.update.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto-check for updates when app launches
        lifecycleScope.launch {
            updateManager.checkForUpdates(isAutoCheck = true)
        }

        setContent {
            TaskerTheme {
                val updateState by updateManager.updateState.collectAsState()

                AppNavGraph()

                UpdateDialog(
                    updateState = updateState,
                    onStartDownload = {
                        lifecycleScope.launch {
                            updateManager.startDownload()
                        }
                    },
                    onInstallApk = { apkFile ->
                        updateManager.installApk(apkFile)
                    },
                    onDismiss = {
                        updateManager.dismissUpdate()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            updateManager.checkForUpdates(isAutoCheck = true)
        }
    }
}
