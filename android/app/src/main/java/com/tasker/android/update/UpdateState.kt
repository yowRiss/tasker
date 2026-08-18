package com.tasker.android.update

import java.io.File

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkName: String,
    val publishedAt: String,
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : UpdateState
    data class Downloading(val releaseInfo: ReleaseInfo, val progressPercent: Int) : UpdateState
    data class ReadyToInstall(val releaseInfo: ReleaseInfo, val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}
