package com.tasker.android.di

// SyncManager is annotated with @Singleton + @Inject constructor, so Hilt
// provides it automatically. No manual @Provides binding is needed here.
// The previous provideSyncManager() was redundant and risked a Hilt
// dependency-cycle warning because it took SyncManager as a parameter
// and returned it unchanged.
