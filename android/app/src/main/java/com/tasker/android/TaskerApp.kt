package com.tasker.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.channel_sync_id),
                    getString(R.string.channel_sync_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Background sync status" }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.channel_reminders_id),
                    getString(R.string.channel_reminders_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Task due-date reminders" }
            )
        }
    }
}
