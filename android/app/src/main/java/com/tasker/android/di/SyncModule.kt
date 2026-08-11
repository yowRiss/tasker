package com.tasker.android.di

import com.tasker.android.sync.NetworkMonitor
import com.tasker.android.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncManager(
        networkMonitor: NetworkMonitor,
        syncManager: SyncManager
    ): SyncManager = syncManager
}
